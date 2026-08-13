package com.diploma.roadsideassistance.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.remote.dto.ServiceRequestDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyRequestsUiState(
    val isLoading: Boolean = false,
    val requests: List<ServiceRequestDto> = emptyList(),
    val errorMessage: String? = null,
)

class MyRequestsViewModel(private val repository: RequestRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRequestsUiState())
    val uiState: StateFlow<MyRequestsUiState> = _uiState.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            when (val result = repository.getMyRequests()) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, requests = result.data.requests)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class MyRequestsViewModelFactory(private val repository: RequestRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyRequestsViewModel::class.java)) {
            return MyRequestsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
