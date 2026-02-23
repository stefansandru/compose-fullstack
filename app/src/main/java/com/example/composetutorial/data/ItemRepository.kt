package com.example.composetutorial.data

import android.util.Log
import com.example.composetutorial.connectivity.ConnectivityObserver
import com.example.composetutorial.connectivity.ConnectivityStatus
import com.example.composetutorial.data.local.ItemDao
import com.example.composetutorial.data.local.UserPreferencesRepository
import com.example.composetutorial.data.model.Item
import com.example.composetutorial.data.model.LoginRequest
import com.example.composetutorial.data.remote.ApiInterface
import com.example.composetutorial.data.remote.ItemEvent
import com.example.composetutorial.data.remote.ItemWebSocket
import com.example.composetutorial.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ItemRepository(
        private val api: ApiInterface,
        private val webSocket: ItemWebSocket,
        private val itemDao: ItemDao,
        private val userPreferencesRepository: UserPreferencesRepository,
        private val notificationHelper: NotificationHelper,
        private val connectivityObserver: ConnectivityObserver
) {
    // Current token in memory, kept in sync with DataStore
    var token: String? = null

    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocketJob: kotlinx.coroutines.Job? = null

    // Source of truth is now the Database
    val items: Flow<List<Item>> = itemDao.getAllItems()

    init {
        // Observe token changes and keep local variable updated
        scope.launch {
            userPreferencesRepository.authToken
                    .distinctUntilChanged() // this is new: only collect if the token changes
                    .collect { savedToken ->
                        token = savedToken
                        // Cancel previous connection job if any
                        webSocketJob?.cancel()

                        if (savedToken != null) {
                            startWebSocketListener()
                        } else {
                            webSocket.disconnect()
                        }
                    }
        }

        // Observe connectivity changes and trigger sync when connection is restored
        scope.launch {
            connectivityObserver.observe().collect { status ->
                Log.d("ItemRepository", "Connectivity status changed: $status")
                if (status == ConnectivityStatus.Available) {
                    // Connection restored - trigger sync immediately
                    Log.d("ItemRepository", "Connection restored - triggering sync")
                    try {
                        sync()
                        refreshItems()
                    } catch (e: Exception) {
                        Log.e("ItemRepository", "Error during auto-sync on connectivity restore", e)
                    }
                }
            }
        }
    }

    suspend fun login(username: String, password: String): Boolean {
        return try {
            val response = api.login(LoginRequest(username, password))
            val newToken = "Bearer ${response.token}"
            userPreferencesRepository.saveAuthToken(newToken)
            true
        } catch (e: Exception) {
            Log.e("ItemRepository", "Login failed", e)
            false
        }
    }

    suspend fun logout() {
        userPreferencesRepository.clearAuthToken()
        itemDao.clearAll()
    }

    private fun startWebSocketListener() {
        webSocketJob =
                scope.launch {
                    webSocket
                            .connect()
                            .catch { e -> Log.e("ItemRepository", "WebSocket error", e) }
                            .collect { event ->
                                when (event) {
                                    is ItemEvent.ItemAdded -> {
                                        Log.d(
                                                "ItemRepository",
                                                "WebSocket: Item added ${event.item.name}"
                                        )
                                        // Ensure items from server are marked as clean (not
                                        // dirty/offline)
                                        val cleanItem =
                                                event.item.copy(
                                                        isDirty = false,
                                                        isDeleted = false,
                                                        isCreatedLocally = false
                                                )
                                        itemDao.insertItem(cleanItem)
                                        // Trigger Notification safely
                                        try {
                                            notificationHelper.showNotification(
                                                    "New Item Received",
                                                    "Added: ${event.item.name}"
                                            )
                                        } catch (e: Exception) {
                                            Log.e(
                                                    "ItemRepository",
                                                    "Failed to show notification",
                                                    e
                                            )
                                        }
                                    }
                                    is ItemEvent.ItemUpdated -> {
                                        Log.d(
                                                "ItemRepository",
                                                "WebSocket: Item updated ${event.item.name}"
                                        )
                                        // Ensure items from server are marked as clean (not
                                        // dirty/offline)
                                        val cleanItem =
                                                event.item.copy(
                                                        isDirty = false,
                                                        isDeleted = false,
                                                        isCreatedLocally = false
                                                )
                                        itemDao.updateItem(cleanItem)
                                    }
                                    is ItemEvent.ItemDeleted -> {
                                        Log.d(
                                                "ItemRepository",
                                                "WebSocket: Item deleted ${event.item.name}"
                                        )
                                        itemDao.deleteById(event.item.id)
                                    }
                                }
                            }
                }
    }

    suspend fun refreshItems() {
        // Ensure we have a token (wait for it if necessary or verify)
        val currentToken = token ?: userPreferencesRepository.authToken.first()

        if (currentToken != null) {
            // IMPORTANT: Preserve dirty items before clearing - these have pending changes
            val dirtyItems = itemDao.getDirtyItems()

            val remoteItems = api.getItems(currentToken)
            // Ensure items from server are marked as clean (not dirty/offline)
            val cleanItems =
                    remoteItems.map { item ->
                        item.copy(isDirty = false, isDeleted = false, isCreatedLocally = false)
                    }

            // Clear all and insert server data
            itemDao.clearAll()
            itemDao.insertAll(cleanItems)

            // Re-insert dirty items that haven't been synced yet
            // (They will be synced on the next sync() call)
            for (dirtyItem in dirtyItems) {
                // Only re-insert if it's not already in the server response
                // (to avoid duplicates for items that were synced but still marked dirty)
                val existsOnServer = cleanItems.any { it.id == dirtyItem.id }
                if (!existsOnServer || dirtyItem.isCreatedLocally) {
                    // For locally created items, they have local IDs that won't match server IDs
                    // For deleted items, we still want to track them for deletion
                    itemDao.insertItem(dirtyItem)
                }
            }
        }
    }

    suspend fun sync() {
        val currentToken = token ?: userPreferencesRepository.authToken.first() ?: return

        val dirtyItems = itemDao.getDirtyItems()
        Log.d("ItemRepository", "Syncing ${dirtyItems.size} dirty items")

        for (item in dirtyItems) {
            try {
                when {
                    item.isDeleted -> {
                        // Item was deleted locally, delete on server
                        Log.d("ItemRepository", "Syncing deleted item ${item.id}")
                        try {
                            api.deleteItem(currentToken, item.id)
                        } catch (e: retrofit2.HttpException) {
                            if (e.code() != 404) throw e
                            // Item doesn't exist on server, that's fine
                        }
                        itemDao.deleteById(item.id)
                        Log.d("ItemRepository", "Successfully synced deletion of item ${item.id}")
                    }
                    item.isCreatedLocally -> {
                        // Item was created locally, create on server
                        Log.d("ItemRepository", "Syncing locally created item: ${item.name}")
                        val serverItem =
                                api.addItem(
                                        currentToken,
                                        item.copy(isDirty = false, isCreatedLocally = false)
                                )
                        // Remove local placeholder - WebSocket will add the server version
                        // OR if WebSocket is slow, we insert the server response directly
                        itemDao.deleteById(item.id)
                        // Insert the server response as a clean item to ensure it's saved
                        val cleanServerItem =
                                serverItem.copy(
                                        isDirty = false,
                                        isDeleted = false,
                                        isCreatedLocally = false
                                )
                        itemDao.insertItem(cleanServerItem)
                        Log.d("ItemRepository", "Successfully synced new item ${serverItem.id}")
                    }
                    item.isDirty -> {
                        // Item was updated locally, update on server
                        Log.d("ItemRepository", "Syncing updated item ${item.id}")
                        val serverItem =
                                api.updateItem(currentToken, item.id, item.copy(isDirty = false))
                        // Update local item to mark as clean
                        val cleanServerItem =
                                serverItem.copy(
                                        isDirty = false,
                                        isDeleted = false,
                                        isCreatedLocally = false
                                )
                        itemDao.updateItem(cleanServerItem)
                        Log.d("ItemRepository", "Successfully synced update of item ${item.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ItemRepository", "Error syncing item ${item.id}: ${e.message}", e)
                // Keep the item dirty so it will be retried next sync
            }
        }
        Log.d("ItemRepository", "Sync completed")
    }

    suspend fun addItem(item: Item) {
        val currentToken = token ?: userPreferencesRepository.authToken.first()

        try {
            if (currentToken != null) {
                api.addItem(currentToken, item)
                // WebSocket will handle the local DB update
                Log.d("ItemRepository", "Item added to server successfully")
            }
        } catch (e: java.net.UnknownHostException) {
            // No network - save locally for later sync
            Log.w("ItemRepository", "Offline: Saving item locally for later sync")
            val offlineItem = item.copy(isDirty = true, isCreatedLocally = true)
            itemDao.insertItem(offlineItem)
        } catch (e: java.net.ConnectException) {
            // Connection refused - save locally for later sync
            Log.w("ItemRepository", "Offline: Saving item locally for later sync")
            val offlineItem = item.copy(isDirty = true, isCreatedLocally = true)
            itemDao.insertItem(offlineItem)
        } catch (e: java.io.IOException) {
            // Network error - save locally for later sync
            Log.w("ItemRepository", "Network error: Saving item locally for later sync", e)
            val offlineItem = item.copy(isDirty = true, isCreatedLocally = true)
            itemDao.insertItem(offlineItem)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error adding item", e)
        }
    }

    suspend fun updateItem(item: Item) {
        val currentToken = token ?: userPreferencesRepository.authToken.first()

        try {
            if (currentToken != null) {
                api.updateItem(currentToken, item.id, item)
                // WebSocket will handle the local DB update
                Log.d("ItemRepository", "Item updated on server successfully")
            }
        } catch (e: java.net.UnknownHostException) {
            // No network - save locally for later sync
            Log.w("ItemRepository", "Offline: Updating item locally for later sync")
            val offlineItem = item.copy(isDirty = true)
            itemDao.updateItem(offlineItem)
        } catch (e: java.net.ConnectException) {
            // Connection refused - save locally for later sync
            Log.w("ItemRepository", "Offline: Updating item locally for later sync")
            val offlineItem = item.copy(isDirty = true)
            itemDao.updateItem(offlineItem)
        } catch (e: java.io.IOException) {
            // Network error - save locally for later sync
            Log.w("ItemRepository", "Network error: Updating item locally for later sync", e)
            val offlineItem = item.copy(isDirty = true)
            itemDao.updateItem(offlineItem)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error updating item", e)
        }
    }

    suspend fun deleteItem(itemId: Int) {
        val currentToken = token ?: userPreferencesRepository.authToken.first()

        try {
            if (currentToken != null) {
                try {
                    api.deleteItem(currentToken, itemId)
                } catch (e: retrofit2.HttpException) {
                    if (e.code() != 404) {
                        throw e
                    }
                    Log.w(
                            "ItemRepository",
                            "Item $itemId not found on server (404), deleting locally to sync."
                    )
                }
                itemDao.deleteById(itemId)
                Log.d("ItemRepository", "Item deleted from server successfully")
            }
        } catch (e: java.net.UnknownHostException) {
            // No network - mark for deletion and sync later
            Log.w("ItemRepository", "Offline: Marking item for deletion")
            markItemForDeletion(itemId)
        } catch (e: java.net.ConnectException) {
            // Connection refused - mark for deletion and sync later
            Log.w("ItemRepository", "Offline: Marking item for deletion")
            markItemForDeletion(itemId)
        } catch (e: java.io.IOException) {
            // Network error - mark for deletion and sync later
            Log.w("ItemRepository", "Network error: Marking item for deletion", e)
            markItemForDeletion(itemId)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Error deleting item", e)
        }
    }

    private suspend fun markItemForDeletion(itemId: Int) {
        // Get the item and mark it as deleted (soft delete)
        val items = itemDao.getAllItems().first()
        val item = items.find { it.id == itemId }
        if (item != null) {
            val deletedItem = item.copy(isDirty = true, isDeleted = true)
            itemDao.updateItem(deletedItem)
        }
    }
}
