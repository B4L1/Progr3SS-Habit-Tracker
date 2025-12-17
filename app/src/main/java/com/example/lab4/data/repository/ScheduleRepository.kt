package com.example.lab4.data.repository

import android.util.Log
import com.example.lab4.data.model.*
import com.example.lab4.data.remote.ScheduleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for schedule-related operations
 * Handles all data operations for schedules and progress tracking
 */
class ScheduleRepository(
    private val scheduleService: ScheduleService
) {
    private val TAG = "ScheduleRepository"

    /**
     * Extension function to await a Retrofit Call
     */
    private suspend fun <T> Call<T>.await(): T {
        return suspendCancellableCoroutine { continuation ->
            enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    if (response.isSuccessful && response.body() != null) {
                        continuation.resume(response.body()!!)
                    } else {
                        continuation.resumeWithException(
                            Exception("HTTP ${response.code()}: ${response.message()}")
                        )
                    }
                }

                override fun onFailure(call: Call<T>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }
    }

    /**
     * Get schedules for a specific date
     * @param date Date in yyyy-MM-dd format (null for today)
     * @return Result containing list of schedules on success, exception on failure
     */
    suspend fun getSchedules(date: String?): Result<List<ScheduleResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val schedules = scheduleService.getSchedules(date).await()
                Log.d(TAG, "Fetched ${schedules.size} schedules")
                Result.success(schedules)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching schedules", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Get a specific schedule by ID
     */
    suspend fun getScheduleById(id: Int): Result<ScheduleResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val schedule = scheduleService.getScheduleById(id).await()
                Log.d(TAG, "Fetched schedule $id")
                Result.success(schedule)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching schedule", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Create a custom (one-time) schedule
     */
    suspend fun createCustomSchedule(request: CreateCustomScheduleDto): Result<ScheduleResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val schedule = scheduleService.createCustomSchedule(request).await()
                Log.d(TAG, "Created custom schedule")
                Result.success(schedule)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating schedule", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Create recurring schedules for designated weekdays
     */
    suspend fun createRecurringSchedule(request: CreateRecurringScheduleDto): Result<List<ScheduleResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val schedules = scheduleService.createRecurringSchedule(request).await()
                Log.d(TAG, "Created recurring schedules")
                Result.success(schedules)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating recurring schedules", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Create progress for a schedule
     */
    suspend fun createProgress(request: CreateProgressDto): Result<ProgressResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val progress = scheduleService.createProgress(request).await()
                Log.d(TAG, "Created progress")
                Result.success(progress)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating progress", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Update an existing schedule
     */
    suspend fun updateSchedule(id: Int, request: UpdateScheduleDto): Result<ScheduleResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val schedule = scheduleService.updateSchedule(id, request).await()
                Log.d(TAG, "Updated schedule $id")
                Result.success(schedule)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating schedule", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Delete a schedule
     */
    suspend fun deleteSchedule(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                scheduleService.deleteSchedule(id).await()
                Log.d(TAG, "Deleted schedule $id")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting schedule", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
}
