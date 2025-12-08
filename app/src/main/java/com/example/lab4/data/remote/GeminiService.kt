package com.example.lab4.data.remote

import com.example.lab4.data.model.GeminiRequest
import com.example.lab4.data.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface GeminiService {
    // This is a placeholder for the Gemini API endpoint.
    // The RetrofitClient will add the Authorization header if `BuildConfig.GEMINI_API_KEY` is set.
    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun getIconSuggestion(
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST("v1beta/models/gemini-pro:generateContent")
    suspend fun generateContent(
        @Body request: GeminiRequest
    ): GeminiResponse
}
