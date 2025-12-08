package com.example.lab4.data.model

import com.google.gson.annotations.SerializedName

data class CreateHabitDto(
    val name: String,
    val description: String?,
    val goal: String, // Changed to non-nullable as per spec
    val categoryId: Int
)
