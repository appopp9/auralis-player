package com.auralis.player.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.AppTheme

/**
 * One entry per design system. A spec is a *complete* visual identity:
 * both colour schemes, corner language, motion personality and the component
 * style knobs. `AuralisTheme` resolves the active spec every recomposition,
 * so switching themes repaints the whole app instantly — no restart.
 */
@Immutable
data class ThemeSpec(
    val theme: AppTheme,
    val label: String,
    val tagline: String,
    /** Signature accent. Null = follow the user's accent choice. */
    val signatureAccent: Color?,
    val style: AuralisStyle,
    val shapes: AuralisShapes,
    val motionSpeed: Float = 1f,
    val motionPlayful: Boolean = false,
    /** Builds the dark scheme for a given accent. */
    val dark: (accent: Color) -> AuralisColorScheme,
    /** Builds the light scheme for a given accent. */
    val light: (accent: Color) -> AuralisColorScheme
)

object ThemeSpecs {

    fun of(theme: AppTheme): ThemeSpec = when (theme) {
        AppTheme.AURORA -> Aurora
        AppTheme.LUXURY_GOLD -> LuxuryGold
        AppTheme.SCANDINAVIAN -> Scandinavian
        AppTheme.DYNAMIC -> Dynamic
        AppTheme.SOFT_UI -> SoftUi
        AppTheme.AMOLED_MINIMAL -> AmoledMinimal
        AppTheme.EXPERIMENTAL -> Experimental
    }

    val all: List<ThemeSpec> by lazy {
        AppTheme.entries.map(::of)
    }

    // ------------------------------------------------------------------
    // 1 · AURORA — default. Vivid blue, white / true-black, floating chrome.
    // ------------------------------------------------------------------
    private val Aurora = ThemeSpec(
        theme = AppTheme.AURORA,
        label = "Aurora",
        tagline = "Vivid blue · black & white",
        signatureAccent = null, // respects the accent picker (default blue)
        style = AuralisStyle(
            theme = AppTheme.AURORA,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.ARTWORK_BLUR,
            gradientAccent = true,
            artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 1.15f,
        dark = { accent ->
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = Color(0xFF000000),
                backgroundElevated = Color(0xFF0A0C12),
                surface = Color(0xFF10131B),
                surfaceMuted = Color(0xFF181C26),
                surfaceGlass = Color(0xFF1B2130).copy(alpha = 0.72f),
                outline = Color(0xFF262C3A),
                textPrimary = Color(0xFFF4F6FB),
                textSecondary = Color(0xFF9AA3B8),
                textTertiary = Color(0xFF636C82),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = accent.copy(alpha = 0.16f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = AuralisColors.Success,
                scrim = Color.Black.copy(alpha = 0.55f),
                accentAlt = Color(0xFF38C8F0),
                hairline = Color(0xFF262C3A)
            )
        },
        light = { accent ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = Color(0xFFFFFFFF),
                backgroundElevated = Color(0xFFFFFFFF),
                surface = Color(0xFFF4F6FB),
                surfaceMuted = Color(0xFFEAEEF6),
                surfaceGlass = Color.White.copy(alpha = 0.78f),
                outline = Color(0xFFDDE3EF),
                textPrimary = Color(0xFF0B0E14),
                textSecondary = Color(0xFF525B70),
                textTertiary = Color(0xFF8A93A8),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = accent.copy(alpha = 0.13f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = Color(0xFF16A971),
                scrim = Color.Black.copy(alpha = 0.35f),
                accentAlt = Color(0xFF00A6D6),
                hairline = Color(0xFFDDE3EF)
            )
        }
    )

    // ------------------------------------------------------------------
    // 2 · LUXURY GOLD — black marble + champagne, serif voice, unhurried.
    // ------------------------------------------------------------------
    private val Gold = Color(0xFFD9B45B)
    private val GoldDeep = Color(0xFFA8842E)

    private val LuxuryGold = ThemeSpec(
        theme = AppTheme.LUXURY_GOLD,
        label = "Luxury Gold",
        tagline = "Black marble · champagne",
        signatureAccent = Gold,
        style = AuralisStyle(
            theme = AppTheme.LUXURY_GOLD,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.LUXE_VIGNETTE,
                        artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 0.8f,
        dark = { _ ->
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = Color(0xFF070605),
                backgroundElevated = Color(0xFF0D0B08),
                surface = Color(0xFF14100B),
                surfaceMuted = Color(0xFF1D1811),
                surfaceGlass = Color(0xFF201A11).copy(alpha = 0.78f),
                outline = Color(0xFF2E2718),
                textPrimary = Color(0xFFF5EFE2),
                textSecondary = Color(0xFFB0A48A),
                textTertiary = Color(0xFF776D55),
                accent = Gold,
                accentGlow = Color(0xFFF2D48A),
                accentSoft = Gold.copy(alpha = 0.14f),
                onAccent = Color(0xFF1C1607),
                danger = Color(0xFFE86A6A),
                success = Color(0xFF9BBB6E),
                scrim = Color.Black.copy(alpha = 0.6f),
                accentAlt = Color(0xFFF2D48A),
                hairline = Gold.copy(alpha = 0.35f)
            )
        },
        light = { _ ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = Color(0xFFFAF6ED),
                backgroundElevated = Color(0xFFFFFDF7),
                surface = Color(0xFFF3EDDE),
                surfaceMuted = Color(0xFFEAE2CD),
                surfaceGlass = Color(0xFFFFFDF7).copy(alpha = 0.8f),
                outline = Color(0xFFD9CDAC),
                textPrimary = Color(0xFF1E1809),
                textSecondary = Color(0xFF6E6448),
                textTertiary = Color(0xFF9C9174),
                accent = GoldDeep,
                accentGlow = Gold,
                accentSoft = GoldDeep.copy(alpha = 0.13f),
                onAccent = Color(0xFFFFFDF7),
                danger = Color(0xFFC94F4F),
                success = Color(0xFF5F7F3A),
                scrim = Color.Black.copy(alpha = 0.35f),
                accentAlt = Gold,
                hairline = GoldDeep.copy(alpha = 0.4f)
            )
        }
    )

    // ------------------------------------------------------------------
    // 3 · SCANDINAVIAN — paper, sage, flat outlines, air.
    // ------------------------------------------------------------------
    private val Sage = Color(0xFF52796F)

    private val Scandinavian = ThemeSpec(
        theme = AppTheme.SCANDINAVIAN,
        label = "Scandinavian",
        tagline = "Paper · sage · calm",
        signatureAccent = Sage,
        style = AuralisStyle(
            theme = AppTheme.SCANDINAVIAN,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.FLAT,
                                    artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 1.25f,
        dark = { _ ->
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = Color(0xFF16181A),
                backgroundElevated = Color(0xFF1B1E20),
                surface = Color(0xFF212528),
                surfaceMuted = Color(0xFF292E32),
                surfaceGlass = Color(0xFF24282B).copy(alpha = 0.85f),
                outline = Color(0xFF383E43),
                textPrimary = Color(0xFFECEDE9),
                textSecondary = Color(0xFF9DA39D),
                textTertiary = Color(0xFF6C726C),
                accent = Color(0xFF84A98C),
                accentGlow = Color(0xFFA9C5AF),
                accentSoft = Color(0xFF84A98C).copy(alpha = 0.15f),
                onAccent = Color(0xFF10140F),
                danger = Color(0xFFD97B7B),
                success = Color(0xFF84A98C),
                scrim = Color.Black.copy(alpha = 0.5f),
                accentAlt = Color(0xFFA9C5AF),
                hairline = Color(0xFF383E43)
            )
        },
        light = { _ ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = Color(0xFFFAFAF7),
                backgroundElevated = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                surfaceMuted = Color(0xFFF0F0EA),
                surfaceGlass = Color.White.copy(alpha = 0.85f),
                outline = Color(0xFFE3E3DA),
                textPrimary = Color(0xFF16181A),
                textSecondary = Color(0xFF666C66),
                textTertiary = Color(0xFF9AA09A),
                accent = Sage,
                accentGlow = Color(0xFF84A98C),
                accentSoft = Sage.copy(alpha = 0.12f),
                onAccent = Color(0xFFFDFDFB),
                danger = Color(0xFFC05B5B),
                success = Color(0xFF52796F),
                scrim = Color.Black.copy(alpha = 0.3f),
                accentAlt = Color(0xFF84A98C),
                hairline = Color(0xFFE3E3DA)
            )
        }
    )

    // ------------------------------------------------------------------
    // 4 · DYNAMIC — Material You. Accent injected from wallpaper/artwork
    //     at the Theme level; schemes here are neutral canvases tinted by it.
    // ------------------------------------------------------------------
    private val Dynamic = ThemeSpec(
        theme = AppTheme.DYNAMIC,
        label = "Dynamic",
        tagline = "Material You · wallpaper",
        signatureAccent = null,
        style = AuralisStyle(
            theme = AppTheme.DYNAMIC,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.ARTWORK_BLUR,
            artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 1f,
        motionPlayful = true,
        dark = { accent ->
            val tint = accent.copy(alpha = 0.08f)
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = AuralisColors.mix(Color(0xFF101116), accent, 0.045f),
                backgroundElevated = AuralisColors.mix(Color(0xFF15161C), accent, 0.06f),
                surface = AuralisColors.mix(Color(0xFF1B1C23), accent, 0.08f),
                surfaceMuted = AuralisColors.mix(Color(0xFF23242C), accent, 0.10f),
                surfaceGlass = AuralisColors.mix(Color(0xFF1E1F27), accent, 0.10f).copy(alpha = 0.8f),
                outline = AuralisColors.mix(Color(0xFF31323C), accent, 0.12f),
                textPrimary = Color(0xFFF2F1F7),
                textSecondary = AuralisColors.mix(Color(0xFF9EA0AC), accent, 0.15f),
                textTertiary = Color(0xFF6B6D7A),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = tint.copy(alpha = 0.18f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = AuralisColors.Success,
                scrim = Color.Black.copy(alpha = 0.55f),
                accentAlt = AuralisColors.accentGlow(accent),
                hairline = AuralisColors.mix(Color(0xFF31323C), accent, 0.2f)
            )
        },
        light = { accent ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = AuralisColors.mix(Color(0xFFFDFBFF), accent, 0.03f),
                backgroundElevated = Color(0xFFFFFFFF),
                surface = AuralisColors.mix(Color(0xFFF4F2FA), accent, 0.06f),
                surfaceMuted = AuralisColors.mix(Color(0xFFEBE9F2), accent, 0.08f),
                surfaceGlass = Color.White.copy(alpha = 0.8f),
                outline = AuralisColors.mix(Color(0xFFDCDAE6), accent, 0.1f),
                textPrimary = Color(0xFF17161C),
                textSecondary = Color(0xFF57565F),
                textTertiary = Color(0xFF8B8A94),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = accent.copy(alpha = 0.14f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = Color(0xFF16A971),
                scrim = Color.Black.copy(alpha = 0.32f),
                accentAlt = AuralisColors.accentGlow(accent),
                hairline = AuralisColors.mix(Color(0xFFDCDAE6), accent, 0.16f)
            )
        }
    )

    // ------------------------------------------------------------------
    // 5 · SOFT UI — neumorphism. Surfaces share the canvas colour and are
    //     modelled purely with dual light/shadow extrusion.
    // ------------------------------------------------------------------
    private val Periwinkle = Color(0xFF6C8DFF)

    private val SoftUi = ThemeSpec(
        theme = AppTheme.SOFT_UI,
        label = "Soft UI",
        tagline = "Pillowy neumorphic calm",
        signatureAccent = Periwinkle,
        style = AuralisStyle(
            theme = AppTheme.SOFT_UI,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.NEUMORPH_FRAME,
            artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 0.9f,
        dark = { _ ->
            val canvas = Color(0xFF262B33)
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = canvas,
                backgroundElevated = canvas,
                surface = Color(0xFF2A303A),
                surfaceMuted = Color(0xFF303743),
                surfaceGlass = canvas.copy(alpha = 0.9f),
                outline = Color(0xFF3A424F),
                textPrimary = Color(0xFFE8ECF4),
                textSecondary = Color(0xFF97A0B2),
                textTertiary = Color(0xFF6A7386),
                accent = Color(0xFF7C9AFF),
                accentGlow = Color(0xFFA3B8FF),
                accentSoft = Color(0xFF7C9AFF).copy(alpha = 0.16f),
                onAccent = Color(0xFF0E1320),
                danger = Color(0xFFE87C8B),
                success = Color(0xFF71C99B),
                scrim = Color.Black.copy(alpha = 0.5f),
                accentAlt = Color(0xFFA3B8FF),
                hairline = Color(0xFF3A424F),
                neumorphHi = Color(0xFF333B48),
                neumorphLo = Color(0xFF1A1E25)
            )
        },
        light = { _ ->
            val canvas = Color(0xFFE8EDF5)
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = canvas,
                backgroundElevated = canvas,
                surface = Color(0xFFECF0F7),
                surfaceMuted = Color(0xFFDFE5EF),
                surfaceGlass = canvas.copy(alpha = 0.92f),
                outline = Color(0xFFD3DAE7),
                textPrimary = Color(0xFF272E40),
                textSecondary = Color(0xFF6F7890),
                textTertiary = Color(0xFF9BA3B8),
                accent = Periwinkle,
                accentGlow = Color(0xFF93AAFF),
                accentSoft = Periwinkle.copy(alpha = 0.14f),
                onAccent = Color(0xFFFDFEFF),
                danger = Color(0xFFDB6B7C),
                success = Color(0xFF4CAE7F),
                scrim = Color(0xFF3C465C).copy(alpha = 0.3f),
                accentAlt = Color(0xFF93AAFF),
                hairline = Color(0xFFD3DAE7),
                neumorphHi = Color(0xFFFFFFFF),
                neumorphLo = Color(0xFFC3CCDC)
            )
        }
    )

    // ------------------------------------------------------------------
    // 6 · AMOLED MINIMAL — pure black, hairline dividers, zero decoration.
    // ------------------------------------------------------------------
    private val AmoledMinimal = ThemeSpec(
        theme = AppTheme.AMOLED_MINIMAL,
        label = "AMOLED",
        tagline = "Pure black · zero noise",
        signatureAccent = null, // user accent, defaults to blue
        style = AuralisStyle(
            theme = AppTheme.AMOLED_MINIMAL,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.PURE_BLACK,
                        artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 1.45f,
        dark = { accent ->
            AuralisColorScheme(
                isDark = true,
                isAmoled = true,
                background = Color(0xFF000000),
                backgroundElevated = Color(0xFF000000),
                surface = Color(0xFF0A0A0B),
                surfaceMuted = Color(0xFF121214),
                surfaceGlass = Color(0xFF0C0C0E).copy(alpha = 0.92f),
                outline = Color(0xFF1E1E22),
                textPrimary = Color(0xFFF2F2F5),
                textSecondary = Color(0xFF8E8E99),
                textTertiary = Color(0xFF55555E),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = accent.copy(alpha = 0.15f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = AuralisColors.Success,
                scrim = Color.Black.copy(alpha = 0.7f),
                accentAlt = AuralisColors.accentGlow(accent),
                hairline = Color(0xFF1E1E22)
            )
        },
        light = { accent ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = Color(0xFFFFFFFF),
                backgroundElevated = Color(0xFFFFFFFF),
                surface = Color(0xFFF7F7F8),
                surfaceMuted = Color(0xFFEEEEF0),
                surfaceGlass = Color.White.copy(alpha = 0.92f),
                outline = Color(0xFFE2E2E6),
                textPrimary = Color(0xFF0B0B0D),
                textSecondary = Color(0xFF5F5F68),
                textTertiary = Color(0xFF97979F),
                accent = accent,
                accentGlow = AuralisColors.accentGlow(accent),
                accentSoft = accent.copy(alpha = 0.12f),
                onAccent = AuralisColors.readableOn(accent),
                danger = AuralisColors.Danger,
                success = Color(0xFF16A971),
                scrim = Color.Black.copy(alpha = 0.3f),
                accentAlt = AuralisColors.accentGlow(accent),
                hairline = Color(0xFFE2E2E6)
            )
        }
    )

    // ------------------------------------------------------------------
    // 7 · EXPERIMENTAL — deep space glass, violet→cyan gradient identity.
    // ------------------------------------------------------------------
    private val Violet = Color(0xFF8B5CF6)
    private val Cyan = Color(0xFF22D3EE)

    private val Experimental = ThemeSpec(
        theme = AppTheme.EXPERIMENTAL,
        label = "Experimental",
        tagline = "Deep-space glass · gradients",
        signatureAccent = Violet,
        style = AuralisStyle(
            theme = AppTheme.EXPERIMENTAL,
            panel = PanelStyle.TONAL,
            navBar = NavBarStyle.FLOATING,
            backdrop = BackdropStyle.AURORA_MESH,
            gradientAccent = true,
            artworkAura = true
        ),
        shapes = AuralisShapes(),
        motionSpeed = 1.05f,
        motionPlayful = true,
        dark = { _ ->
            AuralisColorScheme(
                isDark = true,
                isAmoled = false,
                background = Color(0xFF07060F),
                backgroundElevated = Color(0xFF0D0B1C),
                surface = Color(0xFF13102A),
                surfaceMuted = Color(0xFF1B1740),
                surfaceGlass = Color(0xFF171334).copy(alpha = 0.62f),
                outline = Color(0xFF2C2752),
                textPrimary = Color(0xFFF2F0FF),
                textSecondary = Color(0xFF9E97C7),
                textTertiary = Color(0xFF676088),
                accent = Violet,
                accentGlow = Color(0xFFB39DFB),
                accentSoft = Violet.copy(alpha = 0.18f),
                onAccent = Color(0xFFFAF8FF),
                danger = Color(0xFFFB7185),
                success = Color(0xFF34D399),
                scrim = Color(0xFF050310).copy(alpha = 0.65f),
                accentAlt = Cyan,
                hairline = Color(0xFF3A3268)
            )
        },
        light = { _ ->
            AuralisColorScheme(
                isDark = false,
                isAmoled = false,
                background = Color(0xFFF7F5FF),
                backgroundElevated = Color(0xFFFFFFFF),
                surface = Color(0xFFEFEBFD),
                surfaceMuted = Color(0xFFE5DFF9),
                surfaceGlass = Color.White.copy(alpha = 0.7f),
                outline = Color(0xFFD7CFF2),
                textPrimary = Color(0xFF171233),
                textSecondary = Color(0xFF5C5480),
                textTertiary = Color(0xFF938CB4),
                accent = Color(0xFF7C3AED),
                accentGlow = Violet,
                accentSoft = Color(0xFF7C3AED).copy(alpha = 0.13f),
                onAccent = Color(0xFFFAF8FF),
                danger = Color(0xFFE1566C),
                success = Color(0xFF0EA371),
                scrim = Color(0xFF171233).copy(alpha = 0.35f),
                accentAlt = Color(0xFF0891B2),
                hairline = Color(0xFFD7CFF2)
            )
        }
    )
}
