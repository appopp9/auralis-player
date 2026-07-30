package com.auralis.player.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
//  Aurum — Premium Dark AMOLED Color Palette
// ══════════════════════════════════════════════════════════════════════════════

// ── Backgrounds (pure black AMOLED) ──────────────────────────────────────────
val AurumBackground       = Color(0xFF000000)   // Pure black for true AMOLED
val AurumBackgroundDarker = Color(0xFF000000)   // No deeper black possible

// ── Surfaces (elevated from background) ──────────────────────────────────────
val AurumSurface          = Color(0xFF0A0A0A)   // Slightly raised
val AurumSurfaceMid       = Color(0xFF141414)   // Mid elevation
val AurumSurfaceHigh      = Color(0xFF1E1E1E)   // Higher elevation

// ── Cards ────────────────────────────────────────────────────────────────────
val AurumCard             = Color(0xFF1A1A1A)   // Card background
val AurumCardBorder       = Color(0xFF1A1A1A30) // Subtle gold-tinted border (24-bit + alpha)

// ── Accent / Gold ────────────────────────────────────────────────────────────
val AurumGold             = Color(0xFFC0A060)   // Primary gold
val AurumGoldLight        = Color(0xFFD4B878)   // Lighter gold (highlights)
val AurumGoldDark         = Color(0xFFA08840)   // Darker gold (depth)
val AurumGoldSubtle       = Color(0xFFC0A060)   // Alias for clarity
val AurumGoldMuted        = Color(0x33C0A060)   // Gold at 20% opacity (disabled states)
val AurumGoldFaint        = Color(0x14C0A060)   // Gold at 8% opacity (subtle tints)

// ── Text ─────────────────────────────────────────────────────────────────────
val AurumTextPrimary      = Color(0xFFF0F0F0)   // High-emphasis text
val AurumTextSecondary    = Color(0xFFA0A0A0)   // Medium-emphasis text
val AurumTextTertiary     = Color(0xFF606060)   // Low-emphasis / disabled text

// ── Semantic ─────────────────────────────────────────────────────────────────
val AurumDanger           = Color(0xFFE04040)   // Error / destructive actions
val AurumSuccess          = Color(0xFF4CAF50)   // Success / positive states
val AurumWarning          = Color(0xFFFFB300)   // Warning states

// ── Structural ───────────────────────────────────────────────────────────────
val AurumDivider          = Color(0xFF1E1E1E)   // Dividers / separators
val AurumOutline          = Color(0xFF2A2A2A)   // Outlines / borders
val AurumScrim            = Color(0x80000000)   // Overlay / scrim at 50%

// ── Navigation ───────────────────────────────────────────────────────────────
val AurumNavBackground    = Color(0xFF0A0A0A)   // Bottom nav / top bar
val AurumNavActive        = Color(0xFFC0A060)   // Active tab indicator
val AurumNavInactive      = Color(0xFF606060)   // Inactive tab icon/text

// ── Player-specific ──────────────────────────────────────────────────────────
val AurumProgressTrack    = Color(0xFF1E1E1E)   // Unplayed progress track
val AurumProgressFill     = Color(0xFFC0A060)   // Played progress fill
val AurumProgressBar      = Color(0xFFD4B878)   // Progress bar handle

// ── Ripple / Interaction ─────────────────────────────────────────────────────
val AurumRipple           = Color(0x1FC0A060)   // Ripple at ~12% gold
val AurumHighlight        = Color(0x0FC0A060)   // Selection highlight at 6%

// ── Legacy compatibility (kept for any direct references) ────────────────────
val DarkBackground        = AurumBackground
val DarkSurface           = AurumSurface
val DarkSurfaceVariant    = AurumSurfaceHigh
val DarkOnBackground      = AurumTextPrimary
val DarkOnSurface         = AurumTextPrimary
val DarkOnSurfaceVariant  = AurumTextSecondary
val DarkOutline           = AurumOutline

val AuralisPurple         = AurumGold
val AuralisPurpleDark     = AurumGoldDark
val AuralisPurpleLight    = AurumGoldLight
val AuralisViolet         = AurumGold
val AuralisCyan           = AurumGoldLight
val AuralisPink           = AurumGoldLight
