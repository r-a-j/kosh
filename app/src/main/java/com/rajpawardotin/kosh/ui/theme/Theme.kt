package com.rajpawardotin.kosh.ui.theme

import android.os.Build
import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

const val THEME_SYSTEM = "SYSTEM"
const val THEME_OLED_OBSIDIAN = "OLED_OBSIDIAN"
const val THEME_MINIMALIST_SAND = "MINIMALIST_SAND"
const val THEME_AERO_GLASS = "AERO_GLASS"

private val OledColorScheme = darkColorScheme(
    primary = OledPrimary,
    secondary = OledSecondary,
    tertiary = OledTertiary,
    background = OledBackground,
    surface = OledSurface,
    surfaceVariant = OledSurfaceVariant,
    onBackground = OledOnBackground,
    onSurface = OledOnSurface,
    onSurfaceVariant = OledOnSurfaceVariant,
    outline = OledOutline
)

private val SandColorScheme = lightColorScheme(
    primary = SandPrimary,
    secondary = SandSecondary,
    tertiary = SandTertiary,
    background = SandBackground,
    surface = SandSurface,
    surfaceVariant = SandSurfaceVariant,
    onBackground = SandOnBackground,
    onSurface = SandOnSurface,
    onSurfaceVariant = SandOnSurfaceVariant,
    outline = SandOutline
)

private val AeroColorScheme = darkColorScheme(
    primary = AeroPrimary,
    secondary = AeroSecondary,
    tertiary = AeroTertiary,
    background = AeroBackground,
    surface = AeroSurface,
    surfaceVariant = AeroSurfaceVariant,
    onBackground = AeroOnBackground,
    onSurface = AeroOnSurface,
    onSurfaceVariant = AeroOnSurfaceVariant,
    outline = AeroOutline
)

@Composable
fun KoshTheme(
    themeType: String = THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    
    val colorScheme = when (themeType) {
        THEME_OLED_OBSIDIAN -> OledColorScheme
        THEME_MINIMALIST_SAND -> SandColorScheme
        THEME_AERO_GLASS -> AeroColorScheme
        else -> { // SYSTEM
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) OledColorScheme else SandColorScheme
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val isLight = when (themeType) {
            THEME_MINIMALIST_SAND -> true
            THEME_OLED_OBSIDIAN -> false
            THEME_AERO_GLASS -> false
            else -> !darkTheme
        }
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

