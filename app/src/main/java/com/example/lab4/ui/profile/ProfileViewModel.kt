package com.example.lab4.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.model.UpdateProfileDto
import com.example.lab4.data.repository.ProfileRepository
import com.example.lab4.data.repository.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * ViewModel for profile operations
 * Manages UI state for profile viewing and editing
 */
class ProfileViewModel(
    private val repository: ProfileRepository
) : ViewModel() {

    // Profile state
    private val _profileState = MutableStateFlow<UiState<ProfileResponseDto>>(UiState.Idle)
    val profileState: StateFlow<UiState<ProfileResponseDto>> = _profileState.asStateFlow()

    // Update profile state
    private val _updateProfileState = MutableStateFlow<UiState<ProfileResponseDto>>(UiState.Idle)
    val updateProfileState: StateFlow<UiState<ProfileResponseDto>> = _updateProfileState.asStateFlow()

    // Upload profile image state
    private val _uploadImageState = MutableStateFlow<UiState<ProfileResponseDto>>(UiState.Idle)
    val uploadImageState: StateFlow<UiState<ProfileResponseDto>> = _uploadImageState.asStateFlow()

    /**
     * Fetch user profile
     */
    fun fetchProfile() {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            val result = repository.getProfile()
            _profileState.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to fetch profile")
            }
        }
    }

    /**
     * Update user profile
     */
    fun updateProfile(request: UpdateProfileDto) {
        viewModelScope.launch {
            _updateProfileState.value = UiState.Loading
            val result = repository.updateProfile(request)
            _updateProfileState.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update profile")
            }
        }
    }

    /**
     * Upload profile image
     */
    fun uploadProfileImage(imagePart: MultipartBody.Part) {
        viewModelScope.launch {
            _uploadImageState.value = UiState.Loading
            val result = repository.uploadProfileImage(imagePart)
            _uploadImageState.value = if (result.isSuccess) {
                UiState.Success(result.getOrThrow())
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to upload image")
            }
        }
    }

    /**
     * Reset update state
     */
    fun resetUpdateState() {
        _updateProfileState.value = UiState.Idle
    }

    /**
     * Reset upload image state
     */
    fun resetUploadState() {
        _uploadImageState.value = UiState.Idle
    }

    /**
     * Logout
     */
    fun logout(token: String) {
        viewModelScope.launch {
            repository.logout(token)
        }
    }
}
