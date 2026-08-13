package com.diploma.roadsideassistance.data.repository

import com.diploma.roadsideassistance.data.remote.ApiService
import com.diploma.roadsideassistance.data.remote.dto.CreateVehicleBody
import com.diploma.roadsideassistance.data.remote.dto.SingleVehicleResponse
import com.diploma.roadsideassistance.data.remote.dto.VehicleListResponse
import com.diploma.roadsideassistance.util.Resource
import com.diploma.roadsideassistance.util.safeApiCall

class VehicleRepository(private val api: ApiService) {

    suspend fun getMyVehicles(): Resource<VehicleListResponse> = safeApiCall {
        api.getMyVehicles()
    }

    suspend fun createVehicle(
        make: String,
        model: String,
        licensePlate: String,
        year: Int,
    ): Resource<SingleVehicleResponse> = safeApiCall {
        api.createVehicle(CreateVehicleBody(make, model, licensePlate, year))
    }
}
