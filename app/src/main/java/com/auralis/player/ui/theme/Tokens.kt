package com.auralis.player.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale (4dp base). */
@Immutable
data class AuralisSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val huge: Dp = 48.dp,
    val screen: Dp = 20.dp
)

/** Corner scale — soft but restrained, closer to a minimal player. */
@Immutable
data class AuralisShapes(
    val chip: RoundedCornerShape = RoundedCornerShape(50),
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val card: RoundedCornerShape = RoundedCornerShape(18.dp),
    val large: RoundedCornerShape = RoundedCornerShape(26.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    val artwork: RoundedCornerShape = RoundedCornerShape(22.dp)
)

/** Semantic colours resolved from the active theme mode + accent. */
@Immutable
data class AuralisColorScheme(
    val isDark: Boolean,
    val isAmoled: Boolean,
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceGlass: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentGlow: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val danger: Color,
    val success: Color,
    val scrim: Color
)

/**
 * Motion tokens. Durations are deliberately short: the app should feel instant
 * first and animated second, which is what makes scrolling and taps feel fast.
 */
@Immutable
data class AuralisMotion(
    val fast: Int = 120,
    val medium: Int = 200,
    val slow: Int = 320,
    val enabled: Boolean = true
) {
    fun duration(base: Int): Int = if (enabled) base else 0

    val easing: Easing get() = StandardEasing

    fun <T> tweenFast(): FiniteAnimationSpec<T> = tween(duration(fast), easing = StandardEasing)

    fun <T> tweenMedium(): FiniteAnimationSpec<T> = tween(duration(medium), easing = EmphasizedEasing)

    fun <T> tweenSlow(): FiniteAnimationSpec<T> = tween(duration(slow), easing = EmphasizedEasing)

    /** Gentle, almost critically damped spring for position / size changes. */
    fun <T> softSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
        } else {
            tween(0)
        }

    /** Snappy spring for presses, icon morphs and selection pills. */
    fun <T> popSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.48f, stiffness = Spring.StiffnessHigh)
        } else {
            tween(0)
        }

    /** Playful, overshooting spring used for likes, badges and hero moments. */
    fun <T> bouncySpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMedium)
        } else {
            tween(0)
        }

    /** Slow, cinematic spring for sheets and the full-screen player. */
    fun <T> sheetSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow)
        } else {
            tween(0)
        }

    companion object {
        /** Material-3 style standard curve. */
        val StandardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** Emphasized curve: quick start, long luxurious settle. */
        val EmphasizedEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** Used for elements leaving the screen. */
        val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

val LocalAuralisSpacing = staticCompositionLocalOf { AuralisSpacing() }
val LocalAuralisShapes = staticCompositionLocalOf { AuralisShapes() }

// Dynamic (non-static) locals: theme and motion changes must repaint the running
// UI immediately, without restarting the app.
val LocalAuralisMotion = compositionLocalOf { AuralisMotion() }
val LocalAuralisColors = compositionLocalOf<AuralisColorScheme> {
    error("AuralisTheme not applied")
}
