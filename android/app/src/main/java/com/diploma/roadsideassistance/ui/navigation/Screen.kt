package com.diploma.roadsideassistance.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ClientHome : Screen("client_home")
    data object ProviderHome : Screen("provider_home")
    data object Vehicles : Screen("vehicles")
    data object CreateRequest : Screen("create_request")
    data object MyRequests : Screen("my_requests")

    data object RequestDetails : Screen("request_details/{requestId}") {
        fun buildRoute(requestId: String) = "request_details/$requestId"
    }
}
