package com.diploma.roadsideassistance.ui.request

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diploma.roadsideassistance.data.remote.dto.RequestStatus
import com.diploma.roadsideassistance.data.remote.dto.ServiceRequestDto
import com.diploma.roadsideassistance.data.remote.dto.StatusHistoryEntryDto
import com.diploma.roadsideassistance.data.remote.dto.UserRole
import com.diploma.roadsideassistance.data.repository.RequestRepository
import com.diploma.roadsideassistance.ui.common.LocationMap
import com.diploma.roadsideassistance.ui.common.MissingMapsKeyNotice
import com.diploma.roadsideassistance.ui.common.hasMapsApiKey
import com.diploma.roadsideassistance.util.displayName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailsScreen(
    requestId: String,
    requestRepository: RequestRepository,
    currentUserId: String,
    currentUserRole: UserRole,
    onNavigateBack: () -> Unit,
) {
    val viewModel: RequestDetailsViewModel = viewModel(
        factory = RequestDetailsViewModelFactory(requestId, requestRepository),
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детайли на заявката") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обнови")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val request = uiState.request

        if (request == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (uiState.isLoading) CircularProgressIndicator() else Text("Заявката не е намерена.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.serviceType.displayName, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(12.dp))
                StatusBadge(status = request.status)
            }
            Spacer(modifier = Modifier.height(16.dp))

            InfoCard(request = request)
            Spacer(modifier = Modifier.height(16.dp))

            Text("История на статуса", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            StatusHistoryList(history = request.statusHistory)
            Spacer(modifier = Modifier.height(24.dp))

            ActionButtons(
                request = request,
                currentUserId = currentUserId,
                currentUserRole = currentUserRole,
                isUpdating = uiState.isUpdatingStatus,
                onAction = { newStatus ->
                    viewModel.updateStatus(newStatus) { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    }
                },
            )
        }
    }
}

@Composable
private fun InfoCard(request: ServiceRequestDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!request.description.isNullOrBlank()) {
                DetailRow(label = "Описание", value = request.description)
            }
            request.vehicle?.let {
                DetailRow(label = "Автомобил", value = "${it.make} ${it.model}, ${it.licensePlate}")
            }
            val context = LocalContext.current
            Text(text = "Локация", fontWeight = FontWeight.SemiBold)
            if (remember { hasMapsApiKey(context) }) {
                LocationMap(
                    latitude = request.location.latitude,
                    longitude = request.location.longitude,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                )
            } else {
                Text(text = "${request.location.latitude}, ${request.location.longitude}")
            }
            DetailRow(
                label = "Доставчик",
                value = request.provider?.name ?: "Все още не е назначен",
            )
            DetailRow(label = "Създадена на", value = request.createdAt)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = value)
    }
}

@Composable
private fun StatusBadge(status: RequestStatus) {
    val (color, onColor) = when (status) {
        RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        RequestStatus.ACCEPTED, RequestStatus.IN_PROGRESS ->
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        RequestStatus.COMPLETED -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
    }

    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(
            text = status.displayName,
            color = onColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StatusHistoryList(history: List<StatusHistoryEntryDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (history.isEmpty()) {
                Text("Няма данни.")
            }
            history.forEachIndexed { index, entry ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(text = entry.status.displayName, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = entry.changedAt)
                }
                if (index != history.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

private data class RequestAction(
    val targetStatus: RequestStatus,
    val label: String,
    val isDestructive: Boolean = false,
)

// Огледава ALLOWED_TRANSITIONS от backend/src/controllers/requestController.js,
// за да покаже само действия, които бекендът реално ще приеме за текущия потребител.
private fun availableActions(
    request: ServiceRequestDto,
    currentUserId: String,
    role: UserRole,
): List<RequestAction> {
    val isOwner = request.client?.id == currentUserId
    val isAssignedProvider = request.provider?.id == currentUserId
    val actions = mutableListOf<RequestAction>()

    if (role == UserRole.PROVIDER) {
        when (request.status) {
            RequestStatus.PENDING -> actions += RequestAction(RequestStatus.ACCEPTED, "Приеми заявката")
            RequestStatus.ACCEPTED -> if (isAssignedProvider) {
                actions += RequestAction(RequestStatus.IN_PROGRESS, "Стартирай")
                actions += RequestAction(RequestStatus.CANCELLED, "Откажи", isDestructive = true)
            }
            RequestStatus.IN_PROGRESS -> if (isAssignedProvider) {
                actions += RequestAction(RequestStatus.COMPLETED, "Приключи")
            }
            else -> Unit
        }
    } else if (isOwner && (request.status == RequestStatus.PENDING || request.status == RequestStatus.ACCEPTED)) {
        actions += RequestAction(RequestStatus.CANCELLED, "Отмени заявката", isDestructive = true)
    }

    return actions
}

@Composable
private fun ActionButtons(
    request: ServiceRequestDto,
    currentUserId: String,
    currentUserRole: UserRole,
    isUpdating: Boolean,
    onAction: (RequestStatus) -> Unit,
) {
    val actions = availableActions(request, currentUserId, currentUserRole)

    Column {
        actions.forEach { action ->
            val buttonModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)

            if (action.isDestructive) {
                OutlinedButton(onClick = { onAction(action.targetStatus) }, enabled = !isUpdating, modifier = buttonModifier) {
                    Text(action.label)
                }
            } else {
                Button(onClick = { onAction(action.targetStatus) }, enabled = !isUpdating, modifier = buttonModifier) {
                    Text(action.label)
                }
            }
        }
    }
}
