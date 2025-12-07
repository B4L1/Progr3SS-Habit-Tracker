package com.example.lab4.data.model

data class CreateCustomScheduleDto(
    val habitId: Int,
    val date: String, // ISO 8601 date-time string
    val start_time: String, // ISO 8601 date-time string
    val duration_minutes: Int?,
    val notes: String?,
    val is_custom: Boolean = true,
    val icon: String? = null
)

data class CreateRecurringScheduleDto(
    val habitId: Int,
    val start_time: String, // ISO 8601 date-time string
    val repeatPattern: String, // "daily", "weekdays", "weekends"
    val duration_minutes: Int?,
    val notes: String?,
    val is_custom: Boolean = true,
    val repeatDays: Int = 30,
    val icon: String? = null
)

data class UpdateScheduleDto(
    val start_time: String? = null,
    val end_time: String? = null,
    val duration_minutes: Int? = null,
    val status: String? = null, // "Planned", "Completed", "Skipped"
    val date: String? = null,
    val is_custom: Boolean? = null,
    val participantIds: List<Int>? = null,
    val notes: String? = null,
    val icon: String? = null
)
