package com.diploma.roadsideassistance.ui.common

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val DEFAULT_ZOOM = 15f

// Малка карта с единичен маркер на дадена локация - ползва се и при заявяване
// (текуща GPS позиция), и при преглед на детайли за заявка (мястото на инцидента).
@Composable
fun LocationMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
) {
    val position = remember(latitude, longitude) { LatLng(latitude, longitude) }
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, DEFAULT_ZOOM)
    }

    GoogleMap(
        modifier = modifier
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp)),
        cameraPositionState = cameraPositionState,
    ) {
        Marker(state = MarkerState(position = position))
    }
}

// Проверява дали в манифеста има реално попълнен API ключ (не е празен низ) -
// ползва се, за да решим дали да покажем GoogleMap или MissingMapsKeyNotice,
// вместо картата да се провали тихо/да покаже сива плочка.
fun hasMapsApiKey(context: Context): Boolean {
    val appInfo = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
    )
    val key = appInfo.metaData?.getString("com.google.android.geo.API_KEY")
    return !key.isNullOrBlank()
}

// Показва се вместо картата, ако липсва Google Maps API ключ - екранът остава
// напълно използваем (координатите вече са достатъчни за backend заявката),
// просто без визуална карта.
@Composable
fun MissingMapsKeyNotice(modifier: Modifier = Modifier) {
    Text(
        text = "Картата не е налична (липсва Google Maps API ключ).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
