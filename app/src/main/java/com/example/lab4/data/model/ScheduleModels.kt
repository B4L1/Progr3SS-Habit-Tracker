package com.example.lab4.data.model

data class ScheduleResponseDto(
    val id: Int,
    val start_time: String,
    val end_time: String?,
    val duration_minutes: Int?,
    val status: String, // "Planned", "Completed", "Skipped"
    val is_custom: Boolean,
    val date: String,
    val habit: HabitResponseDto?, 
    val notes: String?,
    val progress: List<ProgressResponseDto>?,
    val icon: String? = null
)

data class HabitResponseDto(
    val id: Int,
    val name: String,
    val description: String?,
    val goal: String?,
    val categoryId: Int?,
    val icon: String? = null
)

data class HabitCategoryResponseDto(
    val id: Int,
    val name: String,
    val iconUrl: String?
)
