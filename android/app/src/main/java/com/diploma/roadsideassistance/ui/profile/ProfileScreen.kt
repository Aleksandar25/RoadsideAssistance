package com.diploma.roadsideassistance.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.remote.dto.UserRole
import com.diploma.roadsideassistance.data.repository.AuthRepository

// Споделен екран между CLIENT и PROVIDER - показва данните за контакт на текущия
// потребител (име, имейл, телефон, роля), позволява редакция на име/телефон,
// и бутон за изход от профила.
@Composable
fun ProfileScreen(
    user: UserDto,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onProfileUpdated: (UserDto) -> Unit,
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(authRepository))
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(text = "Моят профил", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (uiState.isEditing) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        label = { Text("Име") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.phone,
                        onValueChange = viewModel::updatePhone,
                        label = { Text("Телефон") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    ProfileField(label = "Име", value = user.name)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(label = "Имейл", value = user.email)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(label = "Телефон", value = user.phone)
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfileField(
                        label = "Роля",
                        value = if (user.role == UserRole.PROVIDER) "Доставчик на пътна помощ" else "Шофьор",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isEditing) {
            Row {
                Button(
                    onClick = { viewModel.save(onSuccess = onProfileUpdated) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Запази")
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = viewModel::cancelEditing,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Отказ")
                }
            }
        } else {
            OutlinedButton(
                onClick = { viewModel.startEditing(user) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Редактирай профила")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Изход")
        }

        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
