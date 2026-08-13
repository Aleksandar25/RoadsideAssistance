package com.diploma.roadsideassistance.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

// Разрешенията, нужни за автоматично засичане на локацията на потребителя
val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun hasLocationPermission(context: Context): Boolean {
    return LOCATION_PERMISSIONS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

// Compose helper за заявка на runtime permission за локация.
// onResult(granted) - granted е true, ако поне едно от двете разрешения е одобрено;
// при отказ приложението трябва да покаже съобщение, а не да гърми (виж CreateRequestScreen).
@Composable
fun rememberLocationPermissionLauncher(
    onResult: (granted: Boolean) -> Unit,
): ActivityResultLauncher<Array<String>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result.values.any { it }
        onResult(granted)
    }
}
