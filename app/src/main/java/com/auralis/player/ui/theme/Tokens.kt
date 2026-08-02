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
import com.auralis.player.domain.model.AppTheme

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

/** Corner scale. Every theme ships its own instance. */
@Immutable
data class AuralisShapes(
    val chip: RoundedCornerShape = RoundedCornerShape(50),
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val card: RoundedCornerShape = RoundedCornerShape(18.dp),
    val large: RoundedCornerShape = RoundedCornerShape(26.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
    val artwork: RoundedCornerShape = RoundedCornerShape(22.dp)
)

/** Semantic colours resolved from the active theme + mode + accent. */
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
    val scrim: Color,
    /** Second stop for gradient accents (Experimental / hero moments). */
    val accentAlt: Color = accentGlow,
    /** Decorative hairline used by LUXE / OUTLINE panels. */
    val hairline: Color = outline,
    /** Neumorphic highlight (top-left light source). */
    val neumorphHi: Color = Color.Transparent,
    /** Neumorphic shadow (bottom-right). */
    val neumorphLo: Color = Color.Transparent
)

// ---------------------------------------------------------------------------
// Theme personality knobs — how components draw themselves per design system.
// ---------------------------------------------------------------------------

enum class PanelStyle { TONAL, GLASS, NEUMORPH, OUTLINE, LUXE }
enum class NavBarStyle { FLOATING, FLAT, HAIRLINE, NEUMORPH }
enum class BackdropStyle { ARTWORK_BLUR, LUXE_VIGNETTE, FLAT, NEUMORPH_FRAME, PURE_BLACK, AURORA_MESH }

/**
 * The non-colour half of a design system: how surfaces, navigation, the player
 * backdrop and display typography behave. Components read these knobs instead
 * of hard-coding one visual language, which is what lets a single codebase
 * render seven genuinely different-feeling apps.
 */
@Immutable
data class AuralisStyle(
    val theme: AppTheme = AppTheme.AURORA,
    val panel: PanelStyle = PanelStyle.TONAL,
    val navBar: NavBarStyle = NavBarStyle.FLOATING,
    val backdrop: BackdropStyle = BackdropStyle.ARTWORK_BLUR,
    /** Fill accent surfaces with a two-stop gradient instead of a flat colour. */
    val gradientAccent: Boolean = false,
    /** Serif display type for hero titles (Luxury). */
    val serifDisplay: Boolean = false,
    /** Light-weight, widely tracked display type (Scandinavian). */
    val airyDisplay: Boolean = false,
    /** Kill all decorative elevation/glow (AMOLED / Scandinavian). */
    val flatChrome: Boolean = false,
    /** Soft colour aura behind large artwork. */
    val artworkAura: Boolean = true
)

/**
 * Motion tokens. Durations are deliberately short: the app should feel instant
 * first and animated second. `speed` compresses or stretches every duration and
 * spring so each theme carries its own motion personality.
 */
@Immutable
data class AuralisMotion(
    val fast: Int = 120,
    val medium: Int = 200,
    val slow: Int = 320,
    val enabled: Boolean = true,
    /** >1 = snappier, <1 = more languid. */
    val speed: Float = 1f,
    /** Extra playfulness: lowers damping on pop/bouncy springs. */
    val playful: Boolean = false
) {
    fun duration(base: Int): Int = if (enabled) (base / speed).toInt().coerceAtLeast(1) else 0

    val easing: Easing get() = StandardEasing

    fun <T> tweenFast(): FiniteAnimationSpec<T> = tween(duration(fast), easing = StandardEasing)

    fun <T> tweenMedium(): FiniteAnimationSpec<T> = tween(duration(medium), easing = EmphasizedEasing)

    fun <T> tweenSlow(): FiniteAnimationSpec<T> = tween(duration(slow), easing = EmphasizedEasing)

    /** Gentle, almost critically damped spring for position / size changes. */
    fun <T> softSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow * speed)
        } else {
            tween(0)
        }

    /** Snappy spring for presses, icon morphs and selection pills. */
    fun <T> popSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(
                dampingRatio = if (playful) 0.42f else 0.48f,
                stiffness = Spring.StiffnessHigh * speed
            )
        } else {
            tween(0)
        }

    /** Playful, overshooting spring used for likes, badges and hero moments. */
    fun <T> bouncySpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(
                dampingRatio = if (playful) 0.28f else 0.34f,
                stiffness = Spring.StiffnessMedium * speed
            )
        } else {
            tween(0)
        }

    /** Slow, cinematic spring for sheets and the full-screen player. */
    fun <T> sheetSpring(): FiniteAnimationSpec<T> =
        if (enabled) {
            spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow * speed)
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

// Dynamic (non-static) locals: theme switches must repaint the running UI
// immediately, without restarting the app.
val LocalAuralisShapes = compositionLocalOf { AuralisShapes() }
val LocalAuralisStyle = compositionLocalOf { AuralisStyle() }
val LocalAuralisMotion = compositionLocalOf { AuralisMotion() }
val LocalAuralisColors = compositionLocalOf<AuralisColorScheme> {
    error("AuralisTheme not applied")
}
