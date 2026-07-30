package com.auralis.player.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.ThemeMode

object AuralisTheme {
    val colors: AuralisColorScheme
        @Composable get() = LocalAuralisColors.current
    val spacing: AuralisSpacing
        @Composable get() = LocalAuralisSpacing.current
    val shapes: AuralisShapes
        @Composable get() = LocalAuralisShapes.current
    val motion: AuralisMotion
        @Composable get() = LocalAuralisMotion.current
}

@Composable
fun AuralisTheme(
    themeMode: ThemeMode,
    accentPalette: AccentPalette,
    customAccentArgb: Long,
    dynamicAccent: Color? = null,
    animationsEnabled: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val isAmoled = themeMode == ThemeMode.AMOLED

    val baseAccent = dynamicAccent ?: AuralisColors.accentSeed(accentPalette, customAccentArgb)
    val animatedAccent by animateColorAsState(
        targetValue = baseAccent,
        animationSpec = tween(if (animationsEnabled) 450 else 0),
        label = "accent"
    )

    val scheme = remember(isDark, isAmoled, animatedAccent, highContrast) {
        buildScheme(isDark, isAmoled, animatedAccent, highContrast)
    }

    val materialScheme = remember(scheme) {
        if (scheme.isDark) {
            darkColorScheme(
                primary = scheme.accent,
                onPrimary = scheme.onAccent,
                secondary = scheme.accentGlow,
                background = scheme.background,
                onBackground = scheme.textPrimary,
                surface = scheme.surface,
                onSurface = scheme.textPrimary,
                surfaceVariant = scheme.surfaceMuted,
                onSurfaceVariant = scheme.textSecondary,
                outline = scheme.outline,
                error = scheme.danger
            )
        } else {
            lightColorScheme(
                primary = scheme.accent,
                onPrimary = scheme.onAccent,
                secondary = scheme.accentGlow,
                background = scheme.background,
                onBackground = scheme.textPrimary,
                surface = scheme.surface,
                onSurface = scheme.textPrimary,
                surfaceVariant = scheme.surfaceMuted,
                onSurfaceVariant = scheme.textSecondary,
                outline = scheme.outline,
                error = scheme.danger
            )
        }
    }

    CompositionLocalProvider(
        LocalAuralisColors provides scheme,
        LocalAuralisSpacing provides AuralisSpacing(),
        LocalAuralisShapes provides AuralisShapes(),
        LocalAuralisMotion provides AuralisMotion(enabled = animationsEnabled)
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = AuralisType.material(),
            content = content
        )
    }
}

private fun buildScheme(
    isDark: Boolean,
    isAmoled: Boolean,
    accent: Color,
    highContrast: Boolean
): AuralisColorScheme {
    val glow = AuralisColors.accentGlow(accent)
    return if (isDark) {
        val background = if (isAmoled) Color.Black else AuralisColors.Ink0
        AuralisColorScheme(
            isDark = true,
            isAmoled = isAmoled,
            background = background,
            backgroundElevated = if (isAmoled) Color(0xFF060608) else AuralisColors.Ink1,
            surface = if (isAmoled) Color(0xFF0B0B0D) else AuralisColors.Ink2,
            surfaceMuted = if (isAmoled) Color(0xFF121215) else AuralisColors.Ink3,
            surfaceGlass = (if (isAmoled) Color.White else AuralisColors.Ink5).copy(alpha = if (isAmoled) 0.06f else 0.55f),
            outline = if (highContrast) Color(0xFF6D7488) else AuralisColors.Ink5,
            textPrimary = AuralisColors.TextOnDark,
            textSecondary = if (highContrast) Color(0xFFD3D7E2) else AuralisColors.TextOnDarkMuted,
            textTertiary = if (highContrast) Color(0xFFB6BCCC) else Color(0xFF6D7488),
            accent = accent,
            accentGlow = glow,
            accentSoft = accent.copy(alpha = 0.16f),
            onAccent = AuralisColors.readableOn(accent),
            danger = AuralisColors.Danger,
            success = AuralisColors.Success,
            scrim = Color.Black.copy(alpha = 0.55f)
        )
    } else {
        AuralisColorScheme(
            isDark = false,
            isAmoled = false,
            background = AuralisColors.Paper0,
            backgroundElevated = Color.White,
            surface = Color.White,
            surfaceMuted = AuralisColors.Paper2,
            surfaceGlass = Color.White.copy(alpha = 0.72f),
            outline = if (highContrast) Color(0xFF8B93A6) else AuralisColors.Paper4,
            textPrimary = AuralisColors.TextOnLight,
            textSecondary = if (highContrast) Color(0xFF3A4054) else AuralisColors.TextOnLightMuted,
            textTertiary = if (highContrast) Color(0xFF4E556A) else Color(0xFF8B93A6),
            accent = accent,
            accentGlow = glow,
            accentSoft = accent.copy(alpha = 0.14f),
            onAccent = AuralisColors.readableOn(accent),
            danger = AuralisColors.Danger,
            success = Color(0xFF16A971),
            scrim = Color.Black.copy(alpha = 0.35f)
        )
    }
}
