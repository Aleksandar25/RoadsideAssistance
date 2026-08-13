package com.diploma.roadsideassistance.ui.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.dto.ServiceType
import com.diploma.roadsideassistance.data.remote.dto.VehicleDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.data.repository.VehicleRepository
import com.diploma.roadsideassistance.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateRequestUiState(
    val vehicles: List<VehicleDto> = emptyList(),
    val selectedVehicleId: String? = null,
    val selectedServiceType: ServiceType = ServiceType.TOWING,
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFetchingLocation: Boolean = false,
    val locationErrorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
    val submitSuccess: Boolean = false,
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

class CreateRequestViewModel(
    private val requestRepository: RequestRepository,
    private val vehicleRepository: VehicleRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRequestUiState())
    val uiState: StateFlow<CreateRequestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = vehicleRepository.getMyVehicles()) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(vehicles = result.data.vehicles)
                else -> Unit // липсата на автомобили не е блокираща - полето просто остава празно
            }
        }
    }

    fun selectVehicle(vehicleId: String?) {
        _uiState.value = _uiState.value.copy(selectedVehicleId = vehicleId)
    }

    fun selectServiceType(serviceType: ServiceType) {
        _uiState.value = _uiState.value.copy(selectedServiceType = serviceType)
    }

    fun updateDescription(text: String) {
        _uiState.value = _uiState.value.copy(description = text)
    }

    // Извиква се само след като UI слоят е потвърдил, че permission е одобрен
    fun fetchCurrentLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingLocation = true, locationErrorMessage = null)

            val location = locationProvider.getCurrentLocation()
            _uiState.value = if (location != null) {
                _uiState.value.copy(
                    isFetchingLocation = false,
                    latitude = location.latitude,
                    longitude = location.longitude,
                )
            } else {
                _uiState.value.copy(
                    isFetchingLocation = false,
                    locationErrorMessage = "Не успяхме да засечем локацията. Провери дали GPS е включен и опитай пак.",
                )
            }
        }
    }

    // Извиква се, когато потребителят откаже permission диалога - показваме съобщение, не гърмим
    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            locationErrorMessage = "Нужно е разрешение за локация, за да заявиш пътна помощ.",
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        val lat = state.latitude
        val lng = state.longitude

        if (lat == null || lng == null) {
            _uiState.value = state.copy(submitErrorMessage = "Първо засечи текущата си локация")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, submitErrorMessage = null)

            val result = requestRepository.createRequest(
                serviceType = state.selectedServiceType,
                lat = lat,
                lng = lng,
                description = state.description.ifBlank { null },
                vehicleId = state.selectedVehicleId,
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitSuccess = true)
                    onSuccess()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(isSubmitting = false, submitErrorMessage = result.message)
                }
                else -> Unit
            }
        }
    }

    fun clearSubmitError() {
        _uiState.value = _uiState.value.copy(submitErrorMessage = null)
    }
}

class CreateRequestViewModelFactory(
    private val requestRepository: RequestRepository,
    private val vehicleRepository: VehicleRepository,
    private val locationProvider: LocationProvider,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateRequestViewModel::class.java)) {
            return CreateRequestViewModel(requestRepository, vehicleRepository, locationProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
