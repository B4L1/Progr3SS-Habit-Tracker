package com.example.lab4.data.model

data class CreateHabitDto(
    val name: String,
    val description: String?,
    val goal: String, // Changed to non-nullable as per spec
    val categoryId: Int
)
