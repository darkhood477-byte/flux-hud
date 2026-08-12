package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MinimalDarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnBackgroundDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = OnBackgroundDark,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = OnBackgroundDark,
    outline = BorderDark,
    outlineVariant = BorderDark
)

private val MinimalLightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    background = Color(0xFFFDFDFD),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF4F4F4),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE5E5E5),
    onSurfaceVariant = Color(0xFF1C1B1F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MinimalDarkColorScheme
        else -> MinimalLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
