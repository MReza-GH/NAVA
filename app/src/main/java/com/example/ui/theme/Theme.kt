package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NavaPrimary,
    onPrimary = NavaOnPrimary,
    primaryContainer = NavaPrimaryContainer,
    onPrimaryContainer = NavaOnPrimaryContainer,
    secondary = NavaSecondary,
    onSecondary = NavaOnSecondary,
    secondaryContainer = NavaSecondaryContainer,
    onSecondaryContainer = NavaOnSecondaryContainer,
    tertiary = NavaTertiary,
    onTertiary = NavaOnTertiary,
    tertiaryContainer = NavaTertiaryContainer,
    onTertiaryContainer = NavaOnTertiaryContainer,
    background = NavaDarkBackground,
    onBackground = NavaDarkOnBackground,
    surface = NavaDarkSurface,
    onSurface = NavaDarkOnSurface,
    surfaceVariant = NavaDarkSurfaceVariant,
    onSurfaceVariant = NavaDarkOnSurfaceVariant,
    surfaceContainer = NavaDarkSurfaceContainer,
    outline = NavaDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = NavaPrimaryContainer,
    onPrimary = NavaOnPrimaryContainer,
    primaryContainer = NavaPrimary,
    onPrimaryContainer = NavaOnPrimary,
    secondary = NavaSecondaryContainer,
    onSecondary = NavaOnSecondaryContainer,
    secondaryContainer = NavaSecondary,
    onSecondaryContainer = NavaOnSecondary,
    tertiary = NavaTertiary,
    onTertiary = NavaOnTertiary,
    background = NavaLightBackground,
    onBackground = NavaLightOnBackground,
    surface = NavaLightSurface,
    onSurface = NavaLightOnSurface,
    surfaceVariant = NavaLightSurfaceVariant,
    onSurfaceVariant = NavaLightOnSurfaceVariant,
    outline = NavaLightOutline
)

@Composable
fun NavaTheme(
    themePreference: String = "dark", // "dark", "light", "system"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true // default to dark
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
