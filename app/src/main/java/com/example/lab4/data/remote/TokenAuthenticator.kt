package com.example.lab4.data.remote

import android.content.Context
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.AuthResponseDto
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(context: Context) : Authenticator {
    private val tokenManager = TokenManager(context)
    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        // If we already tried to authenticate with the new token, give up to prevent loops
        val currentToken = tokenManager.getAccessToken()
        if (response.request.header("Authorization")?.contains(currentToken ?: "") == true) {
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // Synchronous call to refresh token
        // Use a separate OkHttpClient to avoid sharing the authenticator and interceptors
        val client = OkHttpClient()
        
        val jsonBody = gson.toJson(mapOf("refreshToken" to refreshToken))
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(RetrofitClient.BASE_URL + "auth/local/refresh")
            .post(requestBody)
            .build()

        try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful && refreshResponse.body != null) {
                val responseString = refreshResponse.body!!.string()
                val authResponse = gson.fromJson(responseString, AuthResponseDto::class.java)

                // Save new tokens
                tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)

                // Retry the original request with the new token
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${authResponse.tokens.accessToken}")
                    .build()
            } else {
                // Refresh failed (e.g., token expired), clear tokens so user has to login again
                tokenManager.clearTokens()
                return null
            }
        } catch (e: Exception) {
            return null
        }
    }
}