package com.diploma.roadsideassistance.data.repository

import com.diploma.roadsideassistance.data.local.TokenManager
import com.diploma.roadsideassistance.data.remote.ApiService
import com.diploma.roadsideassistance.data.remote.dto.AuthResponse
import com.diploma.roadsideassistance.data.remote.dto.LoginRequest
import com.diploma.roadsideassistance.data.remote.dto.RegisterRequest
import com.diploma.roadsideassistance.data.remote.dto.UpdateProfileRequest
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.remote.dto.UserResponse
import com.diploma.roadsideassistance.data.remote.dto.UserRole
import com.diploma.roadsideassistance.util.Resource
import com.diploma.roadsideassistance.util.safeApiCall
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager,
) {

    val currentUserFlow: Flow<UserDto?> = tokenManager.userFlow

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: UserRole,
    ): Resource<AuthResponse> {
        val result = safeApiCall {
            api.register(RegisterRequest(name, email, password, phone, role))
        }
        if (result is Resource.Success) {
            tokenManager.saveSession(result.data.token, result.data.user)
        }
        return result
    }

    suspend fun login(email: String, password: String): Resource<AuthResponse> {
        val result = safeApiCall { api.login(LoginRequest(email, password)) }
        if (result is Resource.Success) {
            tokenManager.saveSession(result.data.token, result.data.user)
        }
        return result
    }

    suspend fun logout() {
        tokenManager.clearSession()
    }

    suspend fun updateProfile(name: String, phone: String): Resource<UserResponse> {
        val result = safeApiCall { api.updateProfile(UpdateProfileRequest(name, phone)) }
        if (result is Resource.Success) {
            tokenManager.updateUser(result.data.user)
        }
        return result
    }

    suspend fun isLoggedIn(): Boolean = tokenManager.getTokenBlocking() != null

    suspend fun getSavedUser(): UserDto? = tokenManager.getUserBlocking()
}
