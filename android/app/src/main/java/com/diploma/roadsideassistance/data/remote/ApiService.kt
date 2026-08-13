package com.diploma.roadsideassistance.data.remote

import com.diploma.roadsideassistance.data.remote.dto.AuthResponse
import com.diploma.roadsideassistance.data.remote.dto.CreateRequestBody
import com.diploma.roadsideassistance.data.remote.dto.LoginRequest
import com.diploma.roadsideassistance.data.remote.dto.RegisterRequest
import com.diploma.roadsideassistance.data.remote.dto.CreateVehicleBody
import com.diploma.roadsideassistance.data.remote.dto.RequestListResponse
import com.diploma.roadsideassistance.data.remote.dto.SingleRequestResponse
import com.diploma.roadsideassistance.data.remote.dto.SingleVehicleResponse
import com.diploma.roadsideassistance.data.remote.dto.UpdateStatusBody
import com.diploma.roadsideassistance.data.remote.dto.VehicleListResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Огледава 1:1 рутовете от backend/src/routes/authRoutes.js и requestRoutes.js
interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/requests")
    suspend fun createRequest(@Body body: CreateRequestBody): Response<SingleRequestResponse>

    @GET("api/requests/nearby")
    suspend fun getNearbyRequests(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("maxDistance") maxDistanceMeters: Int? = null,
    ): Response<RequestListResponse>

    @GET("api/requests/my")
    suspend fun getMyRequests(): Response<RequestListResponse>

    @GET("api/requests/{id}")
    suspend fun getRequestById(@Path("id") id: String): Response<SingleRequestResponse>

    @PATCH("api/requests/{id}/status")
    suspend fun updateRequestStatus(
        @Path("id") id: String,
        @Body body: UpdateStatusBody,
    ): Response<SingleRequestResponse>

    @GET("api/vehicles")
    suspend fun getMyVehicles(): Response<VehicleListResponse>

    @POST("api/vehicles")
    suspend fun createVehicle(@Body body: CreateVehicleBody): Response<SingleVehicleResponse>
}
