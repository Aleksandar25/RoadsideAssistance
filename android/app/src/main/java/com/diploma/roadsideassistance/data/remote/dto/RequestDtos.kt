package com.diploma.roadsideassistance.data.remote.dto

import com.google.gson.annotations.SerializedName

enum class ServiceType {
    TOWING,
    JUMP_START,
    TIRE_CHANGE,
    FUEL_DELIVERY,
    MECHANICAL_FAILURE,
    OTHER,
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

data class LocationDto(
    val type: String,
    // GeoJSON ред: [longitude, latitude]
    val coordinates: List<Double>,
) {
    val longitude: Double get() = coordinates.getOrElse(0) { 0.0 }
    val latitude: Double get() = coordinates.getOrElse(1) { 0.0 }
}

// Кратка референция към клиент/доставчик, каквато бекендът връща populate-ната
// (виж backend/src/controllers/requestController.js - .populate('client'/'provider', 'name phone'))
data class UserRefDto(
    @SerializedName("_id") val id: String,
    val name: String,
    val phone: String,
)

data class StatusHistoryEntryDto(
    val status: RequestStatus,
    val changedAt: String,
)

data class ServiceRequestDto(
    @SerializedName("_id") val id: String,
    val client: UserRefDto?,
    val provider: UserRefDto?,
    val vehicle: VehicleDto?,
    val serviceType: ServiceType,
    val location: LocationDto,
    val address: String?,
    val description: String?,
    val status: RequestStatus,
    val statusHistory: List<StatusHistoryEntryDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
)

// --- Заявки (request bodies) ---

data class CreateRequestBody(
    val serviceType: ServiceType,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val description: String? = null,
    val vehicleId: String? = null,
)

data class UpdateStatusBody(
    val status: RequestStatus,
)

// --- Отговори (response bodies) ---

data class SingleRequestResponse(
    val request: ServiceRequestDto,
)

data class RequestListResponse(
    val count: Int,
    val requests: List<ServiceRequestDto>,
)
