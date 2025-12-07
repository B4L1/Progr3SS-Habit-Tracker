package com.example.lab4.data.remote

import android.content.Context
import com.example.lab4.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(context: Context) : Interceptor {
    private val tokenManager = TokenManager(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // List of endpoints that do NOT require authentication
        val publicEndpoints = listOf(
            "/auth/local/signin",
            "/auth/local/signup",
            "/auth/local/refresh", 
            "/auth/reset-password-via-email"
        )

        val url = request.url.toString()
        val isPublicEndpoint = publicEndpoints.any { url.endsWith(it) }

        if (!isPublicEndpoint) {
            tokenManager.getAccessToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
