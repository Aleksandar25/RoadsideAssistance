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
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.ui.profile.ProfileScreen

private enum class ClientTab(val label: String) {
    HOME("Начало"),
    PROFILE("Профил"),
}

// Основен екран за роля CLIENT - Scaffold с bottom navigation меню между
// "Начало" (заявяване на пътна помощ) и "Профил" (данни за контакт + изход).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientMainScreen(
    user: UserDto,
    onLogout: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToMyRequests: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(ClientTab.HOME) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Пътна помощ") })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == ClientTab.HOME,
                    onClick = { selectedTab = ClientTab.HOME },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(ClientTab.HOME.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == ClientTab.PROFILE,
                    onClick = { selectedTab = ClientTab.PROFILE },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(ClientTab.PROFILE.label) },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                ClientTab.HOME -> ClientHomeScreen(
                    user = user,
                    onNavigateToVehicles = onNavigateToVehicles,
                    onNavigateToCreateRequest = onNavigateToCreateRequest,
                    onNavigateToMyRequests = onNavigateToMyRequests,
                )
                ClientTab.PROFILE -> ProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }
}
