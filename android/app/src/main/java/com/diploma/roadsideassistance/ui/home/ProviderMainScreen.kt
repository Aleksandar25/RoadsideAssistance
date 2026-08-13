package com.diploma.roadsideassistance.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.ui.profile.ProfileScreen
import com.diploma.roadsideassistance.ui.provider.NearbyRequestsScreen

private enum class ProviderTab(val label: String) {
    NEARBY("Наблизо"),
    PROFILE("Профил"),
}

// Основен екран за роля PROVIDER - Scaffold с bottom navigation меню между
// "Наблизо" (активни заявки) и "Профил" (данни за контакт + изход).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderMainScreen(
    user: UserDto,
    requestRepository: RequestRepository,
    locationProvider: LocationProvider,
    onLogout: () -> Unit,
    onOpenRequest: (String) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ProviderTab.NEARBY) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Пътна помощ - Доставчик") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == ProviderTab.NEARBY,
                    onClick = { selectedTab = ProviderTab.NEARBY },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(ProviderTab.NEARBY.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == ProviderTab.PROFILE,
                    onClick = { selectedTab = ProviderTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(ProviderTab.PROFILE.label) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                ProviderTab.NEARBY -> NearbyRequestsScreen(
                    requestRepository = requestRepository,
                    locationProvider = locationProvider,
                    onOpenRequest = onOpenRequest,
                )
                ProviderTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }
}
