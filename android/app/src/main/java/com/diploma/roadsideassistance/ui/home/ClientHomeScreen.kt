package com.diploma.roadsideassistance.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diploma.roadsideassistance.data.remote.dto.UserDto

// Съдържание на таб "Начало" за роля CLIENT (шофьор).
@Composable
fun ClientHomeScreen(
    user: UserDto,
    onNavigateToVehicles: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToMyRequests: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Здравей, ${user.name}!", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToCreateRequest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ЗАЯВИ ПЪТНА ПОМОЩ")
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onNavigateToVehicles, modifier = Modifier.fillMaxWidth()) {
            Text("Моите автомобили")
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onNavigateToMyRequests, modifier = Modifier.fillMaxWidth()) {
            Text("Моите заявки")
        }
    }
}
