package com.example.lab4.data.remote

import com.example.lab4.data.model.CreateCustomScheduleDto
import com.example.lab4.data.model.CreateProgressDto
import com.example.lab4.data.model.CreateRecurringScheduleDto
import com.example.lab4.data.model.ProgressResponseDto
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.model.UpdateScheduleDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduleService {
    @GET("schedule/day")
    fun getSchedules(@Query("date") date: String?): Call<List<ScheduleResponseDto>>

    @GET("schedule/{id}")
    fun getScheduleById(@Path("id") id: Int): Call<ScheduleResponseDto>

    @POST("schedule/custom")
    fun createCustomSchedule(@Body request: CreateCustomScheduleDto): Call<ScheduleResponseDto>

    @POST("schedule/recurring/weekdays")
    fun createRecurringSchedule(@Body request: CreateRecurringScheduleDto): Call<List<ScheduleResponseDto>>

    @POST("progress")
    fun createProgress(@Body request: CreateProgressDto): Call<ProgressResponseDto>

    @PATCH("schedule/{id}")
    fun updateSchedule(@Path("id") id: Int, @Body request: UpdateScheduleDto): Call<ScheduleResponseDto>

    @DELETE("schedule/{id}")
    fun deleteSchedule(@Path("id") id: Int): Call<Void>
}
