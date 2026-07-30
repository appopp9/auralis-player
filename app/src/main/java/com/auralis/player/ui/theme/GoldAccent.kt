package com.auralis.player.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Gold accent palette for the premium dark AMOLED theme.
 * Delegates to the centralized Aurum palette in Color.kt.
 *
 * Kept for backward compatibility with screens that reference GoldAccent.*.
 * New code should prefer AuralisTheme.colors.* or the Aurum* constants directly.
 */
object GoldAccent {
    val Primary          = AurumGold
    val PrimaryLight     = AurumGoldLight
    val PrimaryDark      = AurumGoldDark
    val OnPrimary        = Color(0xFF000000)

    val Surface          = AurumSurface
    val SurfaceElevated  = AurumSurfaceMid
    val SurfaceCard      = AurumCard

    val TextPrimary      = AurumTextPrimary
    val TextSecondary    = AurumTextSecondary
    val TextTertiary     = AurumTextTertiary

    val Divider          = AurumDivider
    val BadgeBackground  = AurumCard
}
