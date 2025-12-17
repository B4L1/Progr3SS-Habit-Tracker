package com.example.lab4.data.remote

import com.example.lab4.data.model.AuthResponseDto
import com.example.lab4.data.model.GoogleSignInDto
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.model.RefreshTokenRequest
import com.example.lab4.data.model.ResetPasswordRequest
import com.example.lab4.data.model.SignInDto
import com.example.lab4.data.model.UpdateProfileDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthService {
    @POST("auth/local/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<AuthResponseDto>

    @Multipart
    @POST("auth/local/signup")
    suspend fun register(
        @Part("username") username: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody
    ): AuthResponseDto

    @POST("auth/local/signin")
    suspend fun login(@Body request: SignInDto): AuthResponseDto

    @POST("auth/local/logout")
    fun logout(@Header("Authorization") token: String): Call<Void>

    @POST("auth/google")
    fun googleLogin(@Body request: GoogleSignInDto): Call<AuthResponseDto>

    @GET("profile")
    fun getProfile(): Call<ProfileResponseDto>

    @PATCH("profile")
    fun updateProfile(@Body request: UpdateProfileDto): Call<ProfileResponseDto>
    
    @Multipart
    @POST("profile/upload-profile-image")
    fun uploadProfileImage(@Part profileImage: MultipartBody.Part): Call<ProfileResponseDto>

    @POST("auth/reset-password-via-email")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<Void>
}
