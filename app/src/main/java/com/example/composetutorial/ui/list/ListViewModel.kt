package com.example.composetutorial.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.composetutorial.data.ItemRepository
import com.example.composetutorial.data.model.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ListViewModel(private val repository: ItemRepository) : ViewModel() {

    // Expose items as a StateFlow for Compose
    val items: StateFlow<List<Item>> = repository.items
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Optimistically refresh items from remote when VM starts
        refreshItems()
    }

    private fun refreshItems() {
        viewModelScope.launch {
            try {
                repository.refreshItems()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 403 || e.code() == 401) {
                    // Token expired. Logout automatically.
                    repository.logout()
                    // Navigation to login should be handled by observing token state in MainActivity,
                    // but since MainActivity only checks at startup, we might need a more reactive approach or simple toast.
                    // For now, clearing data is the safe "logout" action.
                }
            } catch (e: Exception) {
                // Ignore other errors (offline mode filters them mostly)
                e.printStackTrace()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}

class ListViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
