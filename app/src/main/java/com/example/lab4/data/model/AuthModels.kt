package com.example.lab4.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponseDto(
    val message: String,
    val user: UserDto,
    val tokens: TokensDto
)

data class UserDto(
    val id: Int,
    val username: String,
    val email: String,
    @SerializedName(value = "profileImage", alternate = ["profileImageUrl", "profile_image_url", "profile_image", "avatar", "image"])
    val profileImage: String?
)

data class TokensDto(
    val accessToken: String,
    val refreshToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class SignInDto(
    val email: String,
    val password: String
)

data class GoogleSignInDto(
    val idToken: String
)

data class ProfileResponseDto(
    val id: Int,
    val email: String,
    val username: String,
    val description: String?,
    @SerializedName(value = "profileImageUrl", alternate = ["profileImage", "profile_image_url", "profile_image", "avatar", "image"])
    val profileImageUrl: String?
)

data class UpdateProfileDto(
    val username: String?
)

data class ResetPasswordRequest(
    val email: String
)
