package com.diploma.roadsideassistance.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.remote.dto.UserRole
import com.diploma.roadsideassistance.data.repository.AuthRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    // true, докато проверяваме дали има запазена сесия от предишно стартиране (DataStore)
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loggedInUser: UserDto? = null,
) {
    val isAuthenticated: Boolean get() = loggedInUser != null
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // При стартиране на приложението проверяваме дали вече има запазена сесия (JWT),
        // за да прескочим Login екрана, ако потребителят вече е логнат.
        viewModelScope.launch {
            val savedUser = repository.getSavedUser()
            _uiState.value = _uiState.value.copy(loggedInUser = savedUser, isCheckingSession = false)
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Моля, попълнете имейл и парола")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = repository.login(email.trim(), password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loggedInUser = result.data.user,
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun register(name: String, email: String, password: String, phone: String, role: UserRole) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || phone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Моля, попълнете всички полета")
            return
        }
        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Паролата трябва да е поне 6 символа")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = repository.register(name.trim(), email.trim(), password, phone.trim(), role)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loggedInUser = result.data.user,
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            // isCheckingSession = false изрично, иначе AuthUiState() по подразбиране би
            // показал отново loading spinner-а (виж NavGraph) вместо Login екрана
            _uiState.value = AuthUiState(isCheckingSession = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class AuthViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
