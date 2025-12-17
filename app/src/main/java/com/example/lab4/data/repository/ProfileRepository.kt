package com.example.lab4.data.repository

import android.util.Log
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.model.UpdateProfileDto
import com.example.lab4.data.remote.AuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for profile-related operations
 * Handles all data operations for user profile management
 */
class ProfileRepository(
    private val authService: AuthService
) {
    private val TAG = "ProfileRepository"

    /**
     * Extension function to await a Retrofit Call
     */
    private suspend fun <T> Call<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful && response.body() != null) {
                        continuation.resume(response.body()!!)
                    } else {
                        continuation.resumeWithException(
                            Exception("HTTP ${response.code()}: ${response.message()}")
                        )
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    /**
     * Get user profile
     */
    suspend fun getProfile(): Result<ProfileResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = authService.getProfile().await()
                Log.d(TAG, "Fetched profile for user: ${profile.username}")
                Result.success(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching profile", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(request: UpdateProfileDto): Result<ProfileResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = authService.updateProfile(request).await()
                Log.d(TAG, "Updated profile for user: ${profile.username}")
                Result.success(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Upload profile image
     */
    suspend fun uploadProfileImage(imagePart: MultipartBody.Part): Result<ProfileResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val profile = authService.uploadProfileImage(imagePart).await()
                Log.d(TAG, "Uploaded profile image")
                Result.success(profile)
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading profile image", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Logout user
     */
    suspend fun logout(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                suspendCancellableCoroutine<Unit> { continuation ->
                    authService.logout("Bearer $token").enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            continuation.resume(Unit)
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            continuation.resume(Unit)
                        }
                    })
                }
                Result.success(Unit)
            } catch (e: Exception) {
                // Ignore logout errors
                Result.success(Unit)
            }
        }
    }
}
