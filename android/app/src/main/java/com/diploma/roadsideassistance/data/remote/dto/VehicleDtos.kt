package com.diploma.roadsideassistance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class VehicleDto(
    @SerializedName("_id") val id: String,
    val make: String,
    val model: String,
    val licensePlate: String,
    val year: Int,
) {
    val displayName: String get() = "$make $model ($year)"
}

data class CreateVehicleBody(
    val make: String,
    val model: String,
    val licensePlate: String,
    val year: Int,
)

data class SingleVehicleResponse(
    val vehicle: VehicleDto,
)

data class VehicleListResponse(
    val count: Int,
    val vehicles: List<VehicleDto>,
)
