package com.example.lab4.data.model

import com.google.gson.annotations.SerializedName

data class CreateCustomScheduleDto(
    val habitId: Int, // backend expects camelCase 'habitId'
    val date: String,
    @SerializedName("start_time") val start_time: String,
    @SerializedName("duration_minutes") val duration_minutes: Int,
    val notes: String?
)

data class CreateRecurringScheduleDto(
    val habitId: Int, // backend expects camelCase 'habitId'
    @SerializedName("start_time") val start_time: String,
    val daysOfWeek: List<Int>, // backend expects camelCase 'daysOfWeek'
    val numberOfWeeks: Int = 4, // backend expects camelCase 'numberOfWeeks'
    @SerializedName("duration_minutes") val duration_minutes: Int,
    val notes: String?
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
