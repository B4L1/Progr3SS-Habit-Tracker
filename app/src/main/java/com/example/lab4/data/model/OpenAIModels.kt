package com.example.lab4.data.model

data class ChatRequestDto(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponseDto(
    val id: String,
    val choices: List<ChatChoice>
)

data class ChatChoice(
    val index: Int,
    val message: ChatMessage
)
