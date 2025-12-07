package com.example.lab4.data.remote

import com.example.lab4.data.model.CreateHabitDto
import com.example.lab4.data.model.HabitCategoryResponseDto
import com.example.lab4.data.model.HabitResponseDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HabitService {
    @GET("habit")
    fun getHabits(): Call<List<HabitResponseDto>>

    @POST("habit")
    fun createHabit(@Body request: CreateHabitDto): Call<HabitResponseDto>

    @GET("habit/categories")
    fun getHabitCategories(): Call<List<HabitCategoryResponseDto>>

    @GET("habit/user/{userId}")
    fun getHabitsByUser(@Path("userId") userId: Int): Call<List<HabitResponseDto>>
}
