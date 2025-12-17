package com.example.lab4.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab4.data.repository.AuthRepository
import com.example.lab4.data.repository.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _resetState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val resetState: StateFlow<UiState<Unit>> = _resetState.asStateFlow()

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetState.value = UiState.Loading
            val result = repository.resetPassword(email)
            if (result.isSuccess) {
                _resetState.value = UiState.Success(Unit)
            } else {
                _resetState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
    
    fun resetState() {
        _resetState.value = UiState.Idle
    }
}
