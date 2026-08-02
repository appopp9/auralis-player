package com.auralis.player.ui.theme

import androidx.compose.ui.graphics.Color
import com.auralis.player.domain.model.AccentPalette

/**
 * Auralis colour language. Deep ink neutrals with a luminous accent ramp —
 * intentionally distinct from stock Material palettes.
 */
object AuralisColors {

    // Neutral ink ramp (dark surfaces)
    val Ink0 = Color(0xFF05060A)
    val Ink1 = Color(0xFF0A0C13)
    val Ink2 = Color(0xFF11141C)
    val Ink3 = Color(0xFF171B25)
    val Ink4 = Color(0xFF1F2430)
    val Ink5 = Color(0xFF2A303E)

    // Neutral paper ramp (light surfaces)
    val Paper0 = Color(0xFFFBFBFD)
    val Paper1 = Color(0xFFF3F4F8)
    val Paper2 = Color(0xFFEAECF3)
    val Paper3 = Color(0xFFDFE2EC)
    val Paper4 = Color(0xFFCBD0DE)

    val TextOnDark = Color(0xFFF4F5FA)
    val TextOnDarkMuted = Color(0xFF9AA1B4)
    val TextOnLight = Color(0xFF12141A)
    val TextOnLightMuted = Color(0xFF5C6377)

    val Danger = Color(0xFFFF5A6E)
    val Success = Color(0xFF3ED598)

    val Blue = Color(0xFF2979FF)
    val Purple = Color(0xFF9B6BFF)
    val Violet = Color(0xFF7C5CFF)
    val Cyan = Color(0xFF32D6E0)
    val Pink = Color(0xFFFF6FB5)
    val Peach = Color(0xFFFF9E7A)
    val Green = Color(0xFF43D98B)
    val Orange = Color(0xFFFFA23A)

    fun accentSeed(palette: AccentPalette, customArgb: Long): Color = when (palette) {
        AccentPalette.BLUE -> Blue
        AccentPalette.PURPLE -> Purple
        AccentPalette.VIOLET -> Violet
        AccentPalette.CYAN -> Cyan
        AccentPalette.PINK -> Pink
        AccentPalette.PEACH -> Peach
        AccentPalette.GREEN -> Green
        AccentPalette.ORANGE -> Orange
        AccentPalette.CUSTOM -> Color(customArgb.toInt())
    }

    fun swatch(palette: AccentPalette, customArgb: Long): Color = accentSeed(palette, customArgb)

    /** Secondary tone used for gradients and highlights. */
    fun accentGlow(seed: Color): Color = Color(
        red = (seed.red * 0.55f + 0.40f).coerceIn(0f, 1f),
        green = (seed.green * 0.45f + 0.28f).coerceIn(0f, 1f),
        blue = (seed.blue * 0.60f + 0.38f).coerceIn(0f, 1f),
        alpha = 1f
    )

    fun shade(color: Color, factor: Float): Color = Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )

    fun mix(a: Color, b: Color, ratio: Float): Color {
        val t = ratio.coerceIn(0f, 1f)
        return Color(
            red = a.red + (b.red - a.red) * t,
            green = a.green + (b.green - a.green) * t,
            blue = a.blue + (b.blue - a.blue) * t,
            alpha = a.alpha + (b.alpha - a.alpha) * t
        )
    }

    fun luminance(color: Color): Float =
        0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue

    fun readableOn(background: Color): Color =
        if (luminance(background) > 0.55f) TextOnLight else TextOnDark
}
