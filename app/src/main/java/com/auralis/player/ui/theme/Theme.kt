package com.auralis.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.AppTheme
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
    val style: AuralisStyle
        @Composable get() = LocalAuralisStyle.current
}

/**
 * Display typography voice per theme. Applied on top of the base scale at the
 * few hero call sites (screen titles, section headers, now-playing title) so a
 * theme switch changes the typographic personality, not just colours.
 */
fun AuralisStyle.display(base: TextStyle): TextStyle = when {
    serifDisplay -> base.copy(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
    )
    airyDisplay -> base.copy(
        fontWeight = FontWeight.Light,
        letterSpacing = 2.4.sp
    )
    else -> base
}

/**
 * Root theme. Everything is driven by state collected from DataStore, so the
 * moment a setting changes the entire tree repaints — switching themes never
 * needs an app restart. Key colours animate between design systems for a
 * silky cross-fade instead of a hard cut.
 */
@Composable
fun AuralisTheme(
    appTheme: AppTheme,
    themeMode: ThemeMode,
    accentPalette: AccentPalette,
    customAccentArgb: Long,
    dynamicAccent: Color? = null,
    animationsEnabled: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val spec = remember(appTheme) { ThemeSpecs.of(appTheme) }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    // --- accent resolution ------------------------------------------------
    val context = LocalContext.current
    val userAccent = AuralisColors.accentSeed(accentPalette, customAccentArgb)
    val accent: Color = when {
        appTheme == AppTheme.DYNAMIC -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val m3 = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                m3.primary
            }
            dynamicAccent != null -> dynamicAccent
            else -> userAccent
        }
        spec.signatureAccent != null -> spec.signatureAccent
        else -> userAccent
    }

    val targetScheme = remember(spec, isDark, accent, highContrast) {
        val base = if (isDark) spec.dark(accent) else spec.light(accent)
        if (highContrast) {
            base.copy(
                textSecondary = if (isDark) Color(0xFFD3D7E2) else Color(0xFF3A4054),
                textTertiary = if (isDark) Color(0xFFB6BCCC) else Color(0xFF4E556A),
                outline = if (isDark) Color(0xFF6D7488) else Color(0xFF8B93A6)
            )
        } else {
            base
        }
    }

    val scheme = animateScheme(targetScheme, animationsEnabled)

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
                surfaceContainer = scheme.backgroundElevated,
                surfaceContainerHigh = scheme.surface,
                surfaceContainerHighest = scheme.surfaceMuted,
                outline = scheme.outline,
                outlineVariant = scheme.outline.copy(alpha = 0.6f),
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
                surfaceContainer = scheme.backgroundElevated,
                surfaceContainerHigh = scheme.surface,
                surfaceContainerHighest = scheme.surfaceMuted,
                outline = scheme.outline,
                outlineVariant = scheme.outline.copy(alpha = 0.6f),
                error = scheme.danger
            )
        }
    }

    val motion = remember(animationsEnabled, spec) {
        AuralisMotion(
            enabled = animationsEnabled,
            speed = spec.motionSpeed,
            playful = spec.motionPlayful
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !isDark
            insets.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalAuralisColors provides scheme,
        LocalAuralisSpacing provides AuralisSpacing(),
        LocalAuralisShapes provides spec.shapes,
        LocalAuralisStyle provides spec.style,
        LocalAuralisMotion provides motion
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = AuralisType.material(),
            content = content
        )
    }
}

/**
 * Cross-fades the structural colours whenever the target scheme changes.
 * Only runs while a switch is in flight; at rest this is a plain pass-through.
 */
@Composable
private fun animateScheme(target: AuralisColorScheme, animate: Boolean): AuralisColorScheme {
    val spec: AnimationSpec<Color> = tween(
        durationMillis = if (animate) 420 else 0,
        easing = AuralisMotion.EmphasizedEasing
    )
    val background by animateColorAsState(target.background, spec, label = "bg")
    val elevated by animateColorAsState(target.backgroundElevated, spec, label = "bgEl")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val muted by animateColorAsState(target.surfaceMuted, spec, label = "muted")
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "textP")
    val textSecondary by animateColorAsState(target.textSecondary, spec, label = "textS")
    val outline by animateColorAsState(target.outline, spec, label = "outline")

    return target.copy(
        background = background,
        backgroundElevated = elevated,
        surface = surface,
        surfaceMuted = muted,
        accent = accent,
        accentSoft = accent.copy(alpha = if (target.isDark) 0.16f else 0.13f),
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        outline = outline
    )
}
