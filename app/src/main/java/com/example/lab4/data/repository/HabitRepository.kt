package com.example.lab4.data.repository

import android.util.Log
import com.example.lab4.data.model.CreateHabitDto
import com.example.lab4.data.model.HabitCategoryResponseDto
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.remote.HabitService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for habit-related operations
 * Handles all data operations for habits and habit categories
 */
class HabitRepository(
    private val habitService: HabitService
) {
    private val TAG = "HabitRepository"

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
     * Get all habits
     */
    suspend fun getHabits(): Result<List<HabitResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val habits = habitService.getHabits().await()
                Log.d(TAG, "Fetched ${habits.size} habits")
                Result.success(habits)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching habits", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Create a new habit
     */
    suspend fun createHabit(request: CreateHabitDto): Result<HabitResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val habit = habitService.createHabit(request).await()
                Log.d(TAG, "Created habit: ${request.name}")
                Result.success(habit)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating habit", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Get all habit categories
     */
    suspend fun getHabitCategories(): Result<List<HabitCategoryResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val categories = habitService.getHabitCategories().await()
                Log.d(TAG, "Fetched ${categories.size} habit categories")
                Result.success(categories)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching habit categories", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Get habits for a specific user
     */
    suspend fun getHabitsByUser(userId: Int): Result<List<HabitResponseDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val habits = habitService.getHabitsByUser(userId).await()
                Log.d(TAG, "Fetched habits for user $userId")
                Result.success(habits)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user habits", e)
                Result.failure(Exception("Network error: ${e.message}"))
            }
        }
    }
}
