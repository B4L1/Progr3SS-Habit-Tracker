package com.example.lab4.data.repository

import android.util.Log
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.AuthResponseDto
import com.example.lab4.data.model.SignInDto
import com.example.lab4.data.remote.AuthService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import com.example.lab4.data.model.ResetPasswordRequest

/**
 * Repository for authentication-related operations
 * Handles all data operations for login, registration, and token management
 */
class AuthRepository(
    private val authService: AuthService,
    private val tokenManager: TokenManager
) {
    private val TAG = "AuthRepository"

    /**
     * Attempt to login with email and password
     * @return Result containing AuthResponseDto on success, exception on failure
     */
    suspend fun login(email: String, password: String): Result<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val signInDto = SignInDto(email, password)
                val authResponse = authService.login(signInDto)
                
                // Save tokens
                tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)
                tokenManager.saveEmail(email)
                
                Log.d(TAG, "Login successful for $email")
                Result.success(authResponse)
            } catch (e: retrofit2.HttpException) {
                // Extract error message from HTTP exception
                val errorMessage = try {
                    e.response()?.errorBody()?.string()?.let { errorBody ->
                        val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)
                        messageMatch?.groupValues?.get(1) ?: "Login failed"
                    } ?: "Login failed"
                } catch (ex: Exception) {
                    "Login failed"
                }
                
                Log.e(TAG, "Login failed: ${e.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Register a new user
     * @return Result containing AuthResponseDto on success, exception on failure
     */
    suspend fun register(email: String, password: String, name: String): Result<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                // Create RequestBody instances for multipart data
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
                val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
                
                val authResponse = authService.register(nameBody, emailBody, passwordBody)
                
                // Save tokens
                tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)
                tokenManager.saveEmail(email)
                
                Log.d(TAG, "Registration successful for $email")
                Result.success(authResponse)
            } catch (e: retrofit2.HttpException) {
                // Extract error message from HTTP exception
                val errorMessage = try {
                    e.response()?.errorBody()?.string()?.let { errorBody ->
                        val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(errorBody)
                        messageMatch?.groupValues?.get(1) ?: "Registration failed"
                    } ?: "Registration failed"
                } catch (ex: Exception) {
                    "Registration failed"
                }
                
                Log.e(TAG, "Registration failed: ${e.code()} - $errorMessage")
                Result.failure(Exception(errorMessage))
            } catch (e: Exception) {
                Log.e(TAG, "Registration error", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Get the saved email from local storage
     */
    fun getSavedEmail(): String? {
        return tokenManager.getEmail()
    }

    /**
     * Check if user has saved refresh token (potentially logged in)
     */
    fun hasRefreshToken(): Boolean {
        return !tokenManager.getRefreshToken().isNullOrEmpty()
    }

    /**
     * Clear all authentication tokens
     */
    fun logout() {
        tokenManager.clearTokens()
        Log.d(TAG, "User logged out")
    }

    /**
     * Request password reset
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                val request = ResetPasswordRequest(email)
                authService.resetPassword(request).enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            continuation.resume(Result.success(Unit))
                        } else {
                            val errorBody = response.errorBody()?.string() ?: response.message()
                            continuation.resume(Result.failure(Exception(errorBody)))
                        }
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        continuation.resume(Result.failure(t))
                    }
                })
            }
        }
    }
}
