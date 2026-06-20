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

private val LightColorScheme = lightColorScheme(
    primary = AuroraPrimary,
    onPrimary = AuroraOnPrimary,
    primaryContainer = AuroraPrimaryContainer,
    onPrimaryContainer = AuroraOnPrimaryContainer,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    secondaryContainer = AuroraSecondaryContainer,
    onSecondaryContainer = AuroraOnSecondaryContainer,
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    background = AuroraBackground,
    onBackground = AuroraOnBackground,
    surface = AuroraSurface,
    onSurface = AuroraOnSurface,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = AuroraOnSurfaceVariant,
    outline = AuroraOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = AuroraPrimaryDark,
    onPrimary = AuroraOnPrimaryDark,
    primaryContainer = AuroraPrimaryContainerDark,
    onPrimaryContainer = AuroraOnPrimaryContainerDark,
    secondary = AuroraSecondary,
    onSecondary = AuroraOnSecondary,
    tertiary = AuroraTertiary,
    onTertiary = AuroraOnTertiary,
    background = AuroraBackgroundDark,
    onBackground = AuroraOnBackgroundDark,
    surface = AuroraSurfaceDark,
    onSurface = AuroraOnSurfaceDark,
    surfaceVariant = AuroraSurfaceVariantDark,
    onSurfaceVariant = AuroraOnSurfaceVariantDark,
    outline = AuroraOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to strictly enforce the brand's custom design
    content: @Composable () -> Unit,
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
