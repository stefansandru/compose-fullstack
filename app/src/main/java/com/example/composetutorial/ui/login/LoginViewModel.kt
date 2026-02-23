package com.example.composetutorial.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.composetutorial.data.ItemRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loginEvent = Channel<Boolean>()
    val loginEvent = _loginEvent.receiveAsFlow()
    
    // Simplest way to expose error message
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun onUsernameChange(value: String) {
        _username.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun onLoginClick() {
        if (_username.value.isBlank() || _password.value.isBlank()) {
            _error.value = "Please enter username and password"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val success = repository.login(_username.value, _password.value)
            _isLoading.value = false
            if (success) {
                _loginEvent.send(true)
            } else {
                _error.value = "Login failed. Please check connection or credentials."
                // In a real app, parse the error message from exception
            }
        }
    }
}

class LoginViewModelFactory(private val repository: ItemRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
