package com.example.composetutorial.data.remote

import android.util.Log
import com.example.composetutorial.data.model.Item
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.URI

/**
 * WebSocket event types for real-time updates
 */
sealed class ItemEvent {
    data class ItemAdded(val item: Item) : ItemEvent()
    data class ItemUpdated(val item: Item) : ItemEvent()
    data class ItemDeleted(val item: Item) : ItemEvent()
}

/**
 * Manages WebSocket connection for real-time item updates using callbackFlow.
 * Converts Socket.IO events into a Kotlin Flow.
 */
class ItemWebSocket(private val baseUrl: String) {
    
    private var socket: Socket? = null
    private val gson = Gson()
    
    /**
     * Returns a Flow of ItemEvents using callbackFlow.
     * This converts the callback-based Socket.IO API into a reactive Flow.
     */
    fun connect(): Flow<ItemEvent> = callbackFlow {
        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket")
                reconnection = true
            }
            
            socket = IO.socket(URI.create(baseUrl), options)
            
            socket?.on(Socket.EVENT_CONNECT) {
                Log.d("ItemWebSocket", "Connected to WebSocket")
            }
            
            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d("ItemWebSocket", "Disconnected from WebSocket")
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                Log.e("ItemWebSocket", "Connection error: ${args.firstOrNull()}")
            }
            
            // Listen for itemAdded events from server
            socket?.on("itemAdded") { args ->
                try {
                    val json = args[0].toString()
                    val item = gson.fromJson(json, Item::class.java)
                    Log.d("ItemWebSocket", "Item added: ${item.name}")
                    trySend(ItemEvent.ItemAdded(item))
                } catch (e: Exception) {
                    Log.e("ItemWebSocket", "Error parsing itemAdded", e)
                }
            }
            
            // Listen for itemUpdated events from server
            socket?.on("itemUpdated") { args ->
                try {
                    val json = args[0].toString()
                    val item = gson.fromJson(json, Item::class.java)
                    Log.d("ItemWebSocket", "Item updated: ${item.name}")
                    trySend(ItemEvent.ItemUpdated(item))
                } catch (e: Exception) {
                    Log.e("ItemWebSocket", "Error parsing itemUpdated", e)
                }
            }

            // Listen for itemDeleted events from server
            socket?.on("itemDeleted") { args ->
                try {
                    val json = args[0].toString()
                    val item = gson.fromJson(json, Item::class.java)
                    Log.d("ItemWebSocket", "Item deleted: ${item.name}")
                    trySend(ItemEvent.ItemDeleted(item))
                } catch (e: Exception) {
                    Log.e("ItemWebSocket", "Error parsing itemDeleted", e)
                }
            }
            
            socket?.connect()
            
            // Keep the flow active until cancelled
            awaitClose {
                Log.d("ItemWebSocket", "Closing WebSocket connection")
                socket?.disconnect()
                socket?.off()
                socket = null
            }
        } catch (e: Exception) {
            Log.e("ItemWebSocket", "Error setting up WebSocket", e)
            close(e)
        }
    }
    
    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }
}
