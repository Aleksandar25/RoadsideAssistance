package com.diploma.roadsideassistance.ui.provider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.dto.ServiceRequestDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.util.LOCATION_PERMISSIONS
import com.diploma.roadsideassistance.util.displayName
import com.diploma.roadsideassistance.util.hasLocationPermission
import com.diploma.roadsideassistance.util.rememberLocationPermissionLauncher

// Съдържание на таб "Наблизо" за роля PROVIDER - реалният списък със заявки,
// заменящ старото placeholder съдържание на ProviderHomeScreen.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyRequestsScreen(
    requestRepository: RequestRepository,
    locationProvider: LocationProvider,
    onOpenRequest: (String) -> Unit,
) {
    val viewModel: NearbyRequestsViewModel = viewModel(
        factory = NearbyRequestsViewModelFactory(requestRepository, locationProvider),
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        if (granted) viewModel.loadNearbyRequests() else viewModel.onLocationPermissionDenied()
    }

    val requestLocationAndLoad = {
        if (hasLocationPermission(context)) {
            viewModel.loadNearbyRequests()
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    LaunchedEffect(Unit) { requestLocationAndLoad() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заявки наблизо") },
                actions = {
                    IconButton(onClick = requestLocationAndLoad) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обнови")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            uiState.locationErrorMessage != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(uiState.locationErrorMessage ?: "")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = requestLocationAndLoad) {
                        Text("Опитай пак")
                    }
                }
            }
            uiState.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.requests.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Няма чакащи заявки наблизо в момента.")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(uiState.requests, key = { it.id }) { item ->
                        NearbyRequestRow(request = item, onClick = { onOpenRequest(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyRequestRow(request: ServiceRequestDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = request.serviceType.displayName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = request.client?.name ?: "")
            }
            if (!request.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = request.description)
            }
        }
    }
}
