package com.example.lab4.data.model

data class CreateProgressDto(
    val scheduleId: Int,
    val date: String, // YYYY-MM-DD or ISO
    val logged_time: Int? = null,
    val notes: String? = null,
    val is_completed: Boolean = true
)

data class ProgressResponseDto(
    val id: Int,
    val date: String,
    val logged_time: Int?,
    val notes: String?,
    val is_completed: Boolean
)
