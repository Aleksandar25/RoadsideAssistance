package com.diploma.roadsideassistance.data.remote.dto

import com.google.gson.annotations.SerializedName

enum class UserRole {
    CLIENT,
    PROVIDER,
}

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val role: UserRole,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class UserDto(
    @SerializedName("id") val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
)

data class AuthResponse(
    val token: String,
    val user: UserDto,
)

// Общ формат на грешка, връщан от бекенда: { "message": "..." }
data class ApiErrorBody(
    val message: String?,
)
