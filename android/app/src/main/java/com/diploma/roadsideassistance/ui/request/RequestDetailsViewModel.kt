package com.diploma.roadsideassistance.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.remote.dto.RequestStatus
import com.diploma.roadsideassistance.data.remote.dto.ServiceRequestDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val TERMINAL_STATUSES = setOf(RequestStatus.COMPLETED, RequestStatus.CANCELLED)
private const val POLL_INTERVAL_MS = 10_000L

data class RequestDetailsUiState(
    val request: ServiceRequestDto? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUpdatingStatus: Boolean = false,
)

// Няма push/WebSockets (съзнателно решение за обхвата на проекта) - статусът на
// заявката се опреснява чрез периодичен polling, докато не стигне терминален статус.
class RequestDetailsViewModel(
    private val requestId: String,
    private val repository: RequestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RequestDetailsUiState())
    val uiState: StateFlow<RequestDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                loadRequestInternal()
                val status = _uiState.value.request?.status
                if (status != null && status in TERMINAL_STATUSES) break
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { loadRequestInternal() }
    }

    private suspend fun loadRequestInternal() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        when (val result = repository.getRequestById(requestId)) {
            is Resource.Success -> {
                _uiState.value = _uiState.value.copy(isLoading = false, request = result.data.request)
            }
            is Resource.Error -> {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
            else -> Unit
        }
    }

    // Използва се от PROVIDER екраните (приемане/промяна на статус) в следваща стъпка
    fun updateStatus(newStatus: RequestStatus, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true)

            when (val result = repository.updateRequestStatus(requestId, newStatus)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isUpdatingStatus = false, request = result.data.request)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isUpdatingStatus = false)
                    onError(result.message)
                }
                else -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class RequestDetailsViewModelFactory(
    private val requestId: String,
    private val repository: RequestRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RequestDetailsViewModel::class.java)) {
            return RequestDetailsViewModel(requestId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
