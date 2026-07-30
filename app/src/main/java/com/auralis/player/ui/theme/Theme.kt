package com.auralis.player.ui.theme

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
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.ThemeMode

// ── Accent-palette → primary / secondary color mapping ──────────────────────
private data class AccentColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color
)

private fun accentColorsFor(palette: AccentPalette, customHex: Long): AccentColors {
    val base = when (palette) {
        AccentPalette.BLUE   -> AccentColors(
            primary = Color(0xFF4A90D9),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1A3A5C),
            onPrimaryContainer = Color(0xFFB8D4F8),
            secondary = Color(0xFF7AB4F0),
            onSecondary = Color.White
        )
        AccentPalette.PURPLE -> AccentColors(
            primary = Color(0xFF7C5CFF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF4A2FB0),
            onPrimaryContainer = Color(0xFFB7A4FF),
            secondary = Color(0xFFB65CFF),
            onSecondary = Color.White
        )
        AccentPalette.VIOLET -> AccentColors(
            primary = Color(0xFF9C5CFF),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF5C2FB0),
            onPrimaryContainer = Color(0xFFD4B8FF),
            secondary = Color(0xFFC77DFF),
            onSecondary = Color.White
        )
        AccentPalette.CYAN   -> AccentColors(
            primary = Color(0xFF00BFA5),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF004D40),
            onPrimaryContainer = Color(0xFFA7F3E0),
            secondary = Color(0xFF5CFFEC),
            onSecondary = Color.Black
        )
        AccentPalette.PINK   -> AccentColors(
            primary = Color(0xFFE91E63),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF8C1540),
            onPrimaryContainer = Color(0xFFFFB8D4),
            secondary = Color(0xFFFF7CB7),
            onSecondary = Color.White
        )
        AccentPalette.PEACH  -> AccentColors(
            primary = Color(0xFFFF8A65),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF8C3E1A),
            onPrimaryContainer = Color(0xFFFFD4C4),
            secondary = Color(0xFFFFAB91),
            onSecondary = Color.Black
        )
        AccentPalette.GREEN  -> AccentColors(
            primary = Color(0xFF4CAF50),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF1B5E20),
            onPrimaryContainer = Color(0xFFC8E6C9),
            secondary = Color(0xFF81C784),
            onSecondary = Color.Black
        )
        AccentPalette.ORANGE -> AccentColors(
            primary = Color(0xFFFF9800),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF8C4A00),
            onPrimaryContainer = Color(0xFFFFE0B2),
            secondary = Color(0xFFFFB74D),
            onSecondary = Color.Black
        )
        AccentPalette.CUSTOM -> {
            val c = Color(customHex.toInt())
            // Derive lighter / darker variants from the custom color
            AccentColors(
                primary = c,
                onPrimary = Color.White,
                primaryContainer = c.copy(alpha = 0.35f),
                onPrimaryContainer = Color.White.copy(alpha = 0.9f),
                secondary = c.copy(alpha = 0.8f),
                onSecondary = Color.White
            )
        }
    }
    return base
}

// ── Base background / surface colours (unchanged) ───────────────────────────
private fun darkBase(isAmoled: Boolean) = darkColorScheme(
    primary = Color.Unspecified,
    onPrimary = Color.Unspecified,
    primaryContainer = Color.Unspecified,
    onPrimaryContainer = Color.Unspecified,
    secondary = Color.Unspecified,
    onSecondary = Color.Unspecified,
    background = if (isAmoled) Color.Black else DarkBackground,
    onBackground = DarkOnBackground,
    surface = if (isAmoled) Color(0xFF0A0A0A) else DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightBase = lightColorScheme(
    primary = Color.Unspecified,
    onPrimary = Color.Unspecified,
    primaryContainer = Color.Unspecified,
    onPrimaryContainer = Color.Unspecified,
    secondary = Color.Unspecified,
    onSecondary = Color.Unspecified,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

// ── Main theme composable ───────────────────────────────────────────────────
@Composable
fun AuralisTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPalette = AccentPalette.VIOLET,
    customAccent: Long = 0xFF7C5CFFL,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val colorScheme = when {
        // Material You dynamic color takes priority
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        // Otherwise build scheme from the chosen accent palette
        else -> {
            val accentColours = accentColorsFor(accent, customAccent)
            if (isDark) {
                darkBase(isAmoled).copy(
                    primary = accentColours.primary,
                    onPrimary = accentColours.onPrimary,
                    primaryContainer = accentColours.primaryContainer,
                    onPrimaryContainer = accentColours.onPrimaryContainer,
                    secondary = accentColours.secondary,
                    onSecondary = accentColours.onSecondary
                )
            } else {
                LightBase.copy(
                    primary = accentColours.primary,
                    onPrimary = accentColours.onPrimary,
                    primaryContainer = accentColours.primaryContainer,
                    onPrimaryContainer = accentColours.onPrimaryContainer,
                    secondary = accentColours.secondary,
                    onSecondary = accentColours.onSecondary
                )
            }
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuralisTypography,
        content = content
    )
}
