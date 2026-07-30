package com.auralis.player.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Resolves the app font family at runtime.
 *
 * Drop a Persian font into `app/src/main/res/font/` using any of the names
 * below (for example `vazirmatn_regular.ttf`, optionally plus `_medium` and
 * `_bold`) and the whole app switches to it — no code change needed. If no font
 * file is bundled, the platform sans family is used instead, so the project
 * always compiles.
 */
object AuralisFonts {

    private val regularCandidates = listOf(
        "vazirmatn_regular", "vazirmatn", "estedad_regular", "estedad",
        "sahel", "iransans", "yekan", "app_font"
    )
    private val mediumCandidates = listOf(
        "vazirmatn_medium", "estedad_medium", "sahel_medium", "app_font_medium"
    )
    private val boldCandidates = listOf(
        "vazirmatn_bold", "estedad_bold", "sahel_bold", "app_font_bold"
    )

    fun resolve(context: Context): FontFamily {
        val regular = firstResource(context, regularCandidates) ?: return FontFamily.SansSerif
        val medium = firstResource(context, mediumCandidates)
        val bold = firstResource(context, boldCandidates)

        val fonts = mutableListOf(
            Font(resId = regular, weight = FontWeight.Normal)
        )
        fonts += Font(resId = medium ?: regular, weight = FontWeight.Medium)
        fonts += Font(resId = medium ?: bold ?: regular, weight = FontWeight.SemiBold)
        fonts += Font(resId = bold ?: regular, weight = FontWeight.Bold)
        return FontFamily(fonts)
    }

    private fun firstResource(context: Context, names: List<String>): Int? {
        names.forEach { name ->
            val id = runCatching {
                context.resources.getIdentifier(name, "font", context.packageName)
            }.getOrDefault(0)
            if (id != 0) return id
        }
        return null
    }
}
