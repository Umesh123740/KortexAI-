package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryMint,
    secondary = BrightGreen,
    tertiary = LightMint,
    background = DarkInk,
    surface = DarkInk.copy(alpha = 0.8f),
    onPrimary = DarkInk,
    onSecondary = DarkInk,
    onTertiary = DarkInk,
    onBackground = White,
    onSurface = White,
    surfaceVariant = DarkInk.copy(alpha = 0.6f),
    onSurfaceVariant = LightMint,
    primaryContainer = PaleMint,
    onPrimaryContainer = DarkInk
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryMint,
    secondary = BrightGreen,
    tertiary = LightMint,
    background = Cream,
    surface = White,
    onPrimary = DarkInk,
    onSecondary = DarkInk,
    onTertiary = DarkInk,
    onBackground = DarkInk,
    onSurface = DarkInk,
    surfaceVariant = PaleMint,
    onSurfaceVariant = MutedSecondary,
    primaryContainer = PaleMint,
    onPrimaryContainer = DarkInk
)

@Composable
fun KortexTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
