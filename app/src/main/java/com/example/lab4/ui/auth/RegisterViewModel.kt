package com.example.lab4.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab4.data.model.AuthResponseDto
import com.example.lab4.data.repository.AuthRepository
import com.example.lab4.data.repository.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for RegisterFragment
 * Manages registration state and business logic
 */
class RegisterViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _registerState = MutableStateFlow<UiState<AuthResponseDto>>(UiState.Idle)
    val registerState: StateFlow<UiState<AuthResponseDto>> = _registerState.asStateFlow()

    /**
     * Attempt to register a new user
     */
    fun register(email: String, password: String, name: String) {
        // Validate inputs
        if (email.isBlank()) {
            _registerState.value = UiState.Error("Email is required")
            return
        }
        
        if (password.isBlank()) {
            _registerState.value = UiState.Error("Password is required")
            return
        }
        
        if (name.isBlank()) {
            _registerState.value = UiState.Error("Name is required")
            return
        }

        if (password.length < 6) {
            _registerState.value = UiState.Error("Password must be at least 6 characters")
            return
        }

        // Perform registration
        viewModelScope.launch {
            _registerState.value = UiState.Loading
            
            repository.register(email, password, name)
                .onSuccess { authResponse ->
                    _registerState.value = UiState.Success(authResponse)
                }
                .onFailure { exception ->
                    _registerState.value = UiState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _registerState.value = UiState.Idle
    }
}
