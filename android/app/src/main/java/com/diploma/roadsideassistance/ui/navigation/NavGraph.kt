package com.diploma.roadsideassistance.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diploma.roadsideassistance.core.AppContainer
import com.diploma.roadsideassistance.data.remote.dto.UserRole
import com.diploma.roadsideassistance.ui.auth.AuthViewModel
import com.diploma.roadsideassistance.ui.auth.AuthViewModelFactory
import com.diploma.roadsideassistance.ui.auth.LoginScreen
import com.diploma.roadsideassistance.ui.auth.RegisterScreen
import com.diploma.roadsideassistance.ui.home.ClientMainScreen
import com.diploma.roadsideassistance.ui.home.ProviderMainScreen
import com.diploma.roadsideassistance.ui.request.CreateRequestScreen
import com.diploma.roadsideassistance.ui.request.MyRequestsScreen
import com.diploma.roadsideassistance.ui.request.RequestDetailsScreen
import com.diploma.roadsideassistance.ui.vehicle.VehicleListScreen

@Composable
fun RoadsideNavGraph(container: AppContainer) {
    val navController = rememberNavController()

    // Единствен, споделен AuthViewModel за целия граф (жив, докато е жива Activity-та),
    // за да могат Login/Register/logout да управляват едно и също състояние на сесията.
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(container.authRepository))
    val uiState by authViewModel.uiState.collectAsState()

    if (uiState.isCheckingSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when {
        uiState.loggedInUser?.role == UserRole.PROVIDER -> Screen.ProviderHome.route
        uiState.loggedInUser != null -> Screen.ClientHome.route
        else -> Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
        composable(Screen.ClientHome.route) {
            uiState.loggedInUser?.let { user ->
                ClientMainScreen(
                    user = user,
                    onLogout = { authViewModel.logout() },
                    onNavigateToVehicles = { navController.navigate(Screen.Vehicles.route) },
                    onNavigateToCreateRequest = { navController.navigate(Screen.CreateRequest.route) },
                    onNavigateToMyRequests = { navController.navigate(Screen.MyRequests.route) },
                )
            }
        }
        composable(Screen.ProviderHome.route) {
            uiState.loggedInUser?.let { user ->
                ProviderMainScreen(
                    user = user,
                    requestRepository = container.requestRepository,
                    locationProvider = container.locationProvider,
                    onLogout = { authViewModel.logout() },
                    onOpenRequest = { requestId ->
                        navController.navigate(Screen.RequestDetails.buildRoute(requestId))
                    },
                )
            }
        }
        composable(Screen.Vehicles.route) {
            VehicleListScreen(
                vehicleRepository = container.vehicleRepository,
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Screen.CreateRequest.route) {
            CreateRequestScreen(
                requestRepository = container.requestRepository,
                vehicleRepository = container.vehicleRepository,
                locationProvider = container.locationProvider,
                onNavigateBack = { navController.popBackStack() },
                onRequestCreated = { navController.popBackStack() },
            )
        }
        composable(Screen.MyRequests.route) {
            MyRequestsScreen(
                requestRepository = container.requestRepository,
                onNavigateBack = { navController.popBackStack() },
                onOpenRequest = { requestId ->
                    navController.navigate(Screen.RequestDetails.buildRoute(requestId))
                },
            )
        }
        composable(
            route = Screen.RequestDetails.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId")
            val user = uiState.loggedInUser
            if (requestId != null && user != null) {
                RequestDetailsScreen(
                    requestId = requestId,
                    requestRepository = container.requestRepository,
                    currentUserId = user.id,
                    currentUserRole = user.role,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }

    // Централизирана реакция при промяна на сесията - покрива успешен login/register
    // (пренасочва по роля) и logout (връща към Login), без дублиране на логиката по екраните.
    // Първото изпълнение се прескача, защото startDestination по-горе вече го покрива -
    // иначе бихме "навигирали към себе си" веднага след първоначалното зареждане.
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(uiState.loggedInUser) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }

        val user = uiState.loggedInUser
        val target = when {
            user == null -> Screen.Login.route
            user.role == UserRole.PROVIDER -> Screen.ProviderHome.route
            else -> Screen.ClientHome.route
        }
        navController.navigate(target) {
            popUpTo(0) { inclusive = true }
        }
    }
}
