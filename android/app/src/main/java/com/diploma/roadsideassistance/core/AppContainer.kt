package com.diploma.roadsideassistance.core

import android.content.Context
import com.diploma.roadsideassistance.data.local.TokenManager
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.RetrofitClient
import com.diploma.roadsideassistance.data.repository.AuthRepository
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.data.repository.VehicleRepository

// Ръчен, лек Dependency Injection (ServiceLocator) - без Hilt/Dagger,
// достатъчен за обхвата на дипломния проект. Всички зависимости са singleton
// и се създават лениво само при първо ползване.
class AppContainer(context: Context) {

    val tokenManager: TokenManager by lazy { TokenManager(context.applicationContext) }

    private val apiService by lazy { RetrofitClient.getInstance(tokenManager) }

    val authRepository: AuthRepository by lazy { AuthRepository(apiService, tokenManager) }

    val requestRepository: RequestRepository by lazy { RequestRepository(apiService) }

    val vehicleRepository: VehicleRepository by lazy { VehicleRepository(apiService) }

    val locationProvider: LocationProvider by lazy { LocationProvider(context.applicationContext) }
}
