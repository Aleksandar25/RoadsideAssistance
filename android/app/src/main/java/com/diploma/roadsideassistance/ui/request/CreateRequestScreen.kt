package com.diploma.roadsideassistance.ui.request

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diploma.roadsideassistance.data.location.LocationProvider
import com.diploma.roadsideassistance.data.remote.dto.ServiceType
import com.diploma.roadsideassistance.data.remote.dto.VehicleDto
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.data.repository.VehicleRepository
import com.diploma.roadsideassistance.ui.common.LocationMap
import com.diploma.roadsideassistance.ui.common.MissingMapsKeyNotice
import com.diploma.roadsideassistance.ui.common.hasMapsApiKey
import com.diploma.roadsideassistance.util.LOCATION_PERMISSIONS
import com.diploma.roadsideassistance.util.displayName
import com.diploma.roadsideassistance.util.hasLocationPermission
import com.diploma.roadsideassistance.util.rememberLocationPermissionLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    requestRepository: RequestRepository,
    vehicleRepository: VehicleRepository,
    locationProvider: LocationProvider,
    onNavigateBack: () -> Unit,
    onRequestCreated: () -> Unit,
) {
    val viewModel: CreateRequestViewModel = viewModel(
        factory = CreateRequestViewModelFactory(requestRepository, vehicleRepository, locationProvider),
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLocationPermissionLauncher { granted ->
        if (granted) viewModel.fetchCurrentLocation() else viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(uiState.submitErrorMessage) {
        uiState.submitErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSubmitError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заяви пътна помощ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            VehicleDropdown(
                vehicles = uiState.vehicles,
                selectedVehicleId = uiState.selectedVehicleId,
                onSelect = viewModel::selectVehicle,
            )
            Spacer(modifier = Modifier.height(16.dp))

            ServiceTypeDropdown(
                selected = uiState.selectedServiceType,
                onSelect = viewModel::selectServiceType,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Описание на проблема") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))

            LocationSection(
                latitude = uiState.latitude,
                longitude = uiState.longitude,
                isFetching = uiState.isFetchingLocation,
                errorMessage = uiState.locationErrorMessage,
                onRequestLocation = {
                    if (hasLocationPermission(context)) {
                        viewModel.fetchCurrentLocation()
                    } else {
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                },
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.submit(onSuccess = onRequestCreated) },
                enabled = uiState.hasLocation && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Изпрати заявка")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleDropdown(
    vehicles: List<VehicleDto>,
    selectedVehicleId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedVehicle = vehicles.find { it.id == selectedVehicleId }
    val label = selectedVehicle?.displayName ?: "Без избран автомобил"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Автомобил (по избор)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Без избран автомобил") },
                onClick = { onSelect(null); expanded = false },
            )
            vehicles.forEach { vehicle ->
                DropdownMenuItem(
                    text = { Text(vehicle.displayName) },
                    onClick = { onSelect(vehicle.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceTypeDropdown(
    selected: ServiceType,
    onSelect: (ServiceType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Вид услуга") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ServiceType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun LocationSection(
    latitude: Double?,
    longitude: Double?,
    isFetching: Boolean,
    errorMessage: String?,
    onRequestLocation: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Локация", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (latitude != null && longitude != null) {
                val context = LocalContext.current
                if (remember { hasMapsApiKey(context) }) {
                    LocationMap(
                        latitude = latitude,
                        longitude = longitude,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    MissingMapsKeyNotice(modifier = Modifier.fillMaxWidth())
                }
            } else {
                Text("Локацията все още не е засечена.")
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(onClick = onRequestLocation, enabled = !isFetching, modifier = Modifier.fillMaxWidth()) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (latitude != null) "Обнови локацията" else "Засечи текуща локация")
                }
            }
        }
    }
}
