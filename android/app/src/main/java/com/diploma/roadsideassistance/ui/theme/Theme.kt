package com.diploma.roadsideassistance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = RoadPrimary,
    secondary = RoadSecondary,
    error = RoadError,
    background = RoadBackground,
    surface = RoadSurface,
)

private val DarkColors = darkColorScheme(
    primary = RoadPrimaryDark,
    secondary = RoadSecondary,
    error = RoadError,
)

@Composable
fun RoadsideAssistanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
