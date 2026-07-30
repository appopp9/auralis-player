package com.auralis.player.ui.theme

import android.app.Activity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.ThemeMode

// ══════════════════════════════════════════════════════════════════════════════
//  Aurum — Premium Dark AMOLED Theme
//  A dark-only theme with gold accents for the Aurum music player.
// ══════════════════════════════════════════════════════════════════════════════

// ── Custom color holder ──────────────────────────────────────────────────────

data class AuralisColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHigh: Color,
    val card: Color,
    val cardBorder: Color,
    val accent: Color,
    val accentLight: Color,
    val accentDark: Color,
    val accentMuted: Color,
    val accentFaint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val divider: Color,
    val outline: Color,
    val scrim: Color,
    val navBackground: Color,
    val navActive: Color,
    val navInactive: Color,
    val progressTrack: Color,
    val progressFill: Color,
    val progressHandle: Color,
    val ripple: Color,
    val highlight: Color
)

/** Static default instance — always dark AMOLED + gold. */
val AurumColors = AuralisColors(
    background        = AurumBackground,
    surface           = AurumSurface,
    surfaceElevated   = AurumSurfaceMid,
    surfaceHigh       = AurumSurfaceHigh,
    card              = AurumCard,
    cardBorder        = AurumCardBorder,
    accent            = AurumGold,
    accentLight       = AurumGoldLight,
    accentDark        = AurumGoldDark,
    accentMuted       = AurumGoldMuted,
    accentFaint       = AurumGoldFaint,
    textPrimary       = AurumTextPrimary,
    textSecondary     = AurumTextSecondary,
    textTertiary      = AurumTextTertiary,
    danger            = AurumDanger,
    success           = AurumSuccess,
    warning           = AurumWarning,
    divider           = AurumDivider,
    outline           = AurumOutline,
    scrim             = AurumScrim,
    navBackground     = AurumNavBackground,
    navActive         = AurumNavActive,
    navInactive       = AurumNavInactive,
    progressTrack     = AurumProgressTrack,
    progressFill      = AurumProgressFill,
    progressHandle    = AurumProgressBar,
    ripple            = AurumRipple,
    highlight         = AurumHighlight
)

private val LocalAuralisColors = staticCompositionLocalOf { AurumColors }

// ── Spacing system ───────────────────────────────────────────────────────────

object AurumSpacing {
    val xs   = 4.dp
    val sm   = 8.dp
    val md   = 12.dp
    val lg   = 16.dp
    val xl   = 24.dp
    val xxl  = 32.dp
    val xxxl = 48.dp

    val screenHorizontal = 16.dp
    val screenVertical   = 16.dp
    val cardPadding      = 16.dp
    val cardGap          = 12.dp
    val listItemHeight   = 56.dp
    val miniPlayerHeight = 64.dp
}

// ── Shapes system ────────────────────────────────────────────────────────────

object AurumShapes {
    val xs     = RoundedCornerShape(4.dp)
    val sm     = RoundedCornerShape(8.dp)
    val md     = RoundedCornerShape(12.dp)
    val lg     = RoundedCornerShape(16.dp)
    val xl     = RoundedCornerShape(24.dp)
    val full   = RoundedCornerShape(50)

    val card   = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(12.dp)
    val chip   = RoundedCornerShape(20.dp)
    val avatar = RoundedCornerShape(50)
    val bottomSheet = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
}

// ── Motion / Animation system ────────────────────────────────────────────────

object AurumMotion {
    /** Fast snap for immediate feedback (tab switches, toggles) */
    val fast = tween<Float>(
        durationMillis = 120,
        easing = LinearEasing
    )

    /** Default transition for most UI elements */
    val default = tween<Float>(
        durationMillis = 250,
        easing = LinearEasing
    )

    /** Smooth entrance/exit for sheets and overlays */
    val smooth = tween<Float>(
        durationMillis = 350,
        easing = LinearEasing
    )

    /** Spring for playful, bouncy interactions (FAB, play button) */
    val spring = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Gentle spring for subtle emphasis */
    val gentleSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessLow
    )

    /** Cross-fade duration for album art transitions */
    val crossfade = tween<Float>(
        durationMillis = 400,
        easing = LinearEasing
    )
}

// ── Typography shortcuts for time displays ───────────────────────────────────

object AurumType {
    /** Monospaced-feel text for time displays (mm:ss) */
    val timeDisplay = androidx.compose.ui.text.TextStyle(
        fontSize = 13.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        color = AurumTextSecondary,
        fontFeatureSettings = androidx.compose.ui.text.font.FontFeature("tnum")
    )

    /** Larger time display for now-playing screen */
    val timeDisplayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        color = AurumTextPrimary,
        fontFeatureSettings = androidx.compose.ui.text.font.FontFeature("tnum")
    )
}

// ── Material3 dark color scheme (gold-accented) ─────────────────────────────

private val AurumDarkColorScheme = darkColorScheme(
    primary               = AurumGold,
    onPrimary             = Color(0xFF000000),
    primaryContainer      = Color(0xFF2A2214),
    onPrimaryContainer    = AurumGoldLight,

    secondary             = AurumGoldLight,
    onSecondary           = Color(0xFF000000),
    secondaryContainer    = Color(0xFF1E1A14),
    onSecondaryContainer  = AurumTextPrimary,

    tertiary              = AurumGoldDark,
    onTertiary            = Color(0xFF000000),
    tertiaryContainer     = Color(0xFF1A1610),
    onTertiaryContainer   = AurumTextPrimary,

    background            = AurumBackground,
    onBackground          = AurumTextPrimary,

    surface               = AurumSurface,
    onSurface             = AurumTextPrimary,
    surfaceVariant        = AurumSurfaceHigh,
    onSurfaceVariant      = AurumTextSecondary,

    surfaceTint           = AurumGold,

    error                 = AurumDanger,
    onError               = Color(0xFFFFFFFF),
    errorContainer        = Color(0xFF3A1414),
    onErrorContainer      = Color(0xFFFFCCCC),

    outline               = AurumOutline,
    outlineVariant        = AurumDivider,

    inverseSurface        = AurumTextPrimary,
    inverseOnSurface      = AurumBackground,
    inversePrimary        = AurumGoldDark,

    scrim                 = AurumScrim
)

// ── Main theme composable ────────────────────────────────────────────────────

/**
 * Aurum premium dark AMOLED theme.
 *
 * Always applies a dark color scheme. The [themeMode] parameter only controls
 * the AMOLED-vs-dark distinction (pure black vs near-black background).
 * Light mode is intentionally not supported.
 *
 * Accent colors are always gold (#C0A060). The [accent], [customAccent], and
 * [dynamicColor] parameters exist for API compatibility but are ignored —
 * all surfaces use the gold palette.
 */
@Composable
fun AuralisTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    accent: AccentPalette = AccentPalette.VIOLET, // ignored — gold is fixed
    customAccent: Long = 0L,                       // ignored
    dynamicColor: Boolean = false,                  // ignored
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    CompositionLocalProvider(
        LocalAuralisColors provides AurumColors
    ) {
        MaterialTheme(
            colorScheme = AurumDarkColorScheme,
            typography = AuralisTypography,
            content = content
        )
    }
}

// ── Theme accessor object ────────────────────────────────────────────────────

/**
 * Accessible everywhere inside [AuralisTheme] as `AuralisTheme.colors`,
 * `AuralisTheme.spacing`, `AuralisTheme.shapes`, `AuralisTheme.motion`.
 *
 * Usage:
 * ```
 * Text(
 *     text = "Hello",
 *     color = AuralisTheme.colors.textPrimary,
 *     style = AuralisTheme.typography.bodyLarge
 * )
 * ```
 */
object AuralisTheme {
    /** Custom Aurum color palette — always dark AMOLED + gold. */
    val colors: AuralisColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAuralisColors.current

    /** Spacing constants (dp). */
    val spacing: AurumSpacing
        @Composable
        @ReadOnlyComposable
        get() = AurumSpacing

    /** Shape definitions. */
    val shapes: AurumShapes
        @Composable
        @ReadOnlyComposable
        get() = AurumShapes

    /** Motion / animation specifications. */
    val motion: AurumMotion
        @Composable
        @ReadOnlyComposable
        get() = AurumMotion

    /** Typography shortcuts (time displays, etc). */
    val typography: AurumType
        @Composable
        @ReadOnlyComposable
        get() = AurumType

    /** Material3 typography (delegates to MaterialTheme). */
    val materialTypography: androidx.compose.material3.Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    /** Material3 colorScheme (delegates to MaterialTheme). */
    val materialColors: androidx.compose.material3.ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme
}
