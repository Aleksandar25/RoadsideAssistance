package com.diploma.roadsideassistance.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.diploma.roadsideassistance.data.remote.dto.UserDto
import com.diploma.roadsideassistance.data.remote.dto.UserRole

// Споделен екран между CLIENT и PROVIDER - показва данните за контакт на текущия
// потребител (име, имейл, телефон, роля) и бутон за изход от профила.
@Composable
fun ProfileScreen(user: UserDto, onLogout: () -> Unit) {
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

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Изход")
        }
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
