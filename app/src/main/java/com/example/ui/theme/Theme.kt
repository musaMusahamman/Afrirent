package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NigerGreenPrimaryDark,
    secondary = NigerGreenSecondaryDark,
    tertiary = NigerGreenTertiaryDark,
    background = NigerDarkBackground,
    surface = NigerDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = NigerDarkOnSurface,
    onSurface = NigerDarkOnSurface,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = NigerGreenPrimary,
    secondary = NigerGreenSecondary,
    tertiary = NigerGreenTertiary,
    background = NigerCreamBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = NigerDarkSlate,
    onSurface = NigerDarkSlate,
    error = Color(0xFFB00020)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default dynamicColor to false to force our corporate green brand color
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
