package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    background = DarkBackground,
    onBackground = Color(0xFFE2E4E3),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E4E3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0C4BF),
    surfaceContainer = Color(0xFF1B2A27),
    surfaceContainerHigh = Color(0xFF223531),
    surfaceContainerHighest = Color(0xFF2B403B),
    surfaceContainerLow = Color(0xFF14201D),
    surfaceContainerLowest = Color(0xFF0C1412),
    outline = Color(0xFF455A54),
    outlineVariant = Color(0xFF2A3D39)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldPrimaryContainerLight,
    onPrimaryContainer = EmeraldOnPrimaryContainerLight,
    secondary = GoldSecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261900),
    background = LightBackground,
    onBackground = Color(0xFF191C1B),
    surface = LightSurface,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF404947),
    surfaceContainer = Color(0xFFEFF3F1),
    surfaceContainerHigh = Color(0xFFE8ECE9),
    surfaceContainerHighest = Color(0xFFE2E6E3),
    surfaceContainerLow = Color(0xFFF5F9F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    outline = Color(0xFFCFD8D6),
    outlineVariant = Color(0xFFBEC9C5)
)

@Composable
fun PocketLedgerTheme(
    themePreference: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
