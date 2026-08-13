package com.diploma.roadsideassistance.data.repository

import com.diploma.roadsideassistance.data.remote.ApiService
import com.diploma.roadsideassistance.data.remote.dto.CreateRequestBody
import com.diploma.roadsideassistance.data.remote.dto.RequestListResponse
import com.diploma.roadsideassistance.data.remote.dto.RequestStatus
import com.diploma.roadsideassistance.data.remote.dto.ServiceType
import com.diploma.roadsideassistance.data.remote.dto.SingleRequestResponse
import com.diploma.roadsideassistance.data.remote.dto.UpdateStatusBody
import com.diploma.roadsideassistance.util.Resource
import com.diploma.roadsideassistance.util.safeApiCall

// Обвива всички /api/requests извиквания. Ползва се и от CLIENT, и от PROVIDER екраните.
class RequestRepository(private val api: ApiService) {

    suspend fun createRequest(
        serviceType: ServiceType,
        lat: Double,
        lng: Double,
        address: String? = null,
        description: String? = null,
        vehicleId: String? = null,
    ): Resource<SingleRequestResponse> = safeApiCall {
        api.createRequest(CreateRequestBody(serviceType, lat, lng, address, description, vehicleId))
    }

    suspend fun getNearbyRequests(
        lat: Double,
        lng: Double,
        maxDistanceMeters: Int? = null,
    ): Resource<RequestListResponse> = safeApiCall {
        api.getNearbyRequests(lat, lng, maxDistanceMeters)
    }

    suspend fun getMyRequests(): Resource<RequestListResponse> = safeApiCall {
        api.getMyRequests()
    }

    suspend fun getRequestById(id: String): Resource<SingleRequestResponse> = safeApiCall {
        api.getRequestById(id)
    }

    suspend fun updateRequestStatus(
        id: String,
        status: RequestStatus,
    ): Resource<SingleRequestResponse> = safeApiCall {
        api.updateRequestStatus(id, UpdateStatusBody(status))
    }
}
