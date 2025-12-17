package com.example.lab4.data.repository.common

/**
 * Sealed class representing different UI states for better state management in MVVM
 */
sealed class UiState<out T> {
    /**
     * Initial state before any action
     */
    object Idle : UiState<Nothing>()
    
    /**
     * Loading state during async operations
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Success state with data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state with message
     */
    data class Error(val message: String) : UiState<Nothing>()
}
