package com.diploma.roadsideassistance.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.repository.AuthRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ProfileViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun startEditing(currentUser: UserDto) {
        _uiState.value = ProfileUiState(isEditing = true, name = currentUser.name, phone = currentUser.phone)
    }

    fun cancelEditing() {
        _uiState.value = ProfileUiState()
    }

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun updatePhone(value: String) {
        _uiState.value = _uiState.value.copy(phone = value)
    }

    fun save(onSuccess: (UserDto) -> Unit) {
        val state = _uiState.value
        if (state.name.isBlank() || state.phone.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Името и телефонът са задължителни")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            when (val result = repository.updateProfile(state.name.trim(), state.phone.trim())) {
                is Resource.Success -> {
                    _uiState.value = ProfileUiState()
                    onSuccess(result.data.user)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class ProfileViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
