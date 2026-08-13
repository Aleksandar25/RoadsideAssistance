package com.diploma.roadsideassistance.ui.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.dto.ServiceRequestDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NearbyRequestsUiState(
    val isLoadingLocation: Boolean = false,
    val isLoadingRequests: Boolean = false,
    val requests: List<ServiceRequestDto> = emptyList(),
    val errorMessage: String? = null,
    val locationErrorMessage: String? = null,
) {
    val isLoading: Boolean get() = isLoadingLocation || isLoadingRequests
}

class NearbyRequestsViewModel(
    private val requestRepository: RequestRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyRequestsUiState())
    val uiState: StateFlow<NearbyRequestsUiState> = _uiState.asStateFlow()

    // Извиква се само след като UI слоят е потвърдил, че location permission е одобрен
    fun loadNearbyRequests() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingLocation = true, locationErrorMessage = null, errorMessage = null)

            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isLoadingLocation = false,
                    locationErrorMessage = "Не успяхме да засечем локацията. Провери дали GPS е включен.",
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoadingLocation = false, isLoadingRequests = true)

            when (val result = requestRepository.getNearbyRequests(location.latitude, location.longitude)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoadingRequests = false, requests = result.data.requests)
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isLoadingRequests = false, errorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            locationErrorMessage = "Нужно е разрешение за локация, за да виждаш заявки наблизо.",
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class NearbyRequestsViewModelFactory(
    private val requestRepository: RequestRepository,
    private val locationProvider: LocationProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NearbyRequestsViewModel::class.java)) {
            return NearbyRequestsViewModel(requestRepository, locationProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
