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
 * ViewModel for LoginFragment
 * Manages login state and business logic
 */
class LoginViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<UiState<AuthResponseDto>>(UiState.Idle)
    val loginState: StateFlow<UiState<AuthResponseDto>> = _loginState.asStateFlow()

    private val _savedEmail = MutableStateFlow<String?>(null)
    val savedEmail: StateFlow<String?> = _savedEmail.asStateFlow()

    init {
        loadSavedEmail()
    }

    /**
     * Load saved email from repository
     */
    private fun loadSavedEmail() {
        _savedEmail.value = repository.getSavedEmail()
    }

    /**
     * Attempt to login with email and password
     */
    fun login(email: String, password: String) {
        // Validate inputs
        if (email.isBlank()) {
            _loginState.value = UiState.Error("Email is required")
            return
        }
        
        if (password.isBlank()) {
            _loginState.value = UiState.Error("Password is required")
            return
        }

        // Perform login
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            
            repository.login(email, password)
                .onSuccess { authResponse ->
                    _loginState.value = UiState.Success(authResponse)
                }
                .onFailure { exception ->
                    _loginState.value = UiState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    /**
     * Reset state to idle
     */
    fun resetState() {
        _loginState.value = UiState.Idle
    }

    /**
     * Check if there's a saved refresh token
     */
    fun hasRefreshToken(): Boolean {
        return repository.hasRefreshToken()
    }
}
