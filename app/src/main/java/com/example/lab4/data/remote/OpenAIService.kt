package com.example.lab4.data.remote

import com.example.lab4.data.model.ChatRequestDto
import com.example.lab4.data.model.ChatResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("v1/chat/completions")
    fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatRequestDto
    ): Call<ChatResponseDto>
}
