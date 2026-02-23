package com.example.composetutorial.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.composetutorial.data.ItemRepository
import com.example.composetutorial.data.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditViewModel(
    private val repository: ItemRepository,
    private val itemId: Int?
) : ViewModel() {

    val isEditing: Boolean = itemId != null && itemId != 0

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()

    private val _value = MutableStateFlow("")
    val value = _value.asStateFlow()

    private val _flag = MutableStateFlow(false)
    val flag = _flag.asStateFlow()

    init {
        if (itemId != null && itemId != 0) {
            loadItem(itemId)
        }
    }

    private fun loadItem(id: Int) {
        viewModelScope.launch {
            // Find item in local DB flow (simple approach) or add getById to Repository
            // For now, let's assuming we collect from repository items.
            // A better way is repo.getItem(id)
            repository.items.collect { items ->
                val item = items.find { it.id == id }
                item?.let {
                    _name.value = it.name ?: ""
                    _description.value = it.description ?: ""
                    _date.value = it.date ?: ""
                    _value.value = it.value.toString()
                    _flag.value = it.flag
                }
            }
        }
    }

    fun onNameChange(newValue: String) { _name.value = newValue }
    fun onDescriptionChange(newValue: String) { _description.value = newValue }
    fun onDateChange(newValue: String) { _date.value = newValue }
    fun onValueChange(newValue: String) { _value.value = newValue }
    fun onFlagChange(newValue: Boolean) { _flag.value = newValue }

    fun saveItem(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val itemValue = _value.value.toIntOrNull() ?: 0
            val newItem = Item(
                id = itemId ?: 0,
                name = _name.value,
                description = _description.value,
                date = _date.value,
                value = itemValue,
                flag = _flag.value
            )

            if (itemId == null || itemId == 0) {
                repository.addItem(newItem)
            } else {
                repository.updateItem(newItem)
            }
            onSuccess()
        }
    }

    fun deleteItem(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (itemId != null && itemId != 0) {
                repository.deleteItem(itemId)
                onSuccess()
            }
        }
    }
}

class EditViewModelFactory(
    private val repository: ItemRepository,
    private val itemId: Int?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditViewModel(repository, itemId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
