package com.auralis.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Auralis type scale.
 *
 * Every style pins `TextDirection.Content` so each text run resolves its own
 * direction from its own characters. That is what keeps Persian input rendering
 * right-to-left and Latin input left-to-right inside the same field, instead of
 * inheriting the surrounding layout direction and appearing reversed.
 *
 * The family is installed once at startup (see [install]) so a bundled Persian
 * font in `res/font` is used everywhere, with a graceful fallback to the
 * platform sans family when no font file is bundled.
 */
object AuralisType {

    private class Styles(family: FontFamily) {
        val display = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 40.sp,
            textDirection = TextDirection.Content
        )
        val headline = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
            lineHeight = 30.sp,
            textDirection = TextDirection.Content
        )
        val title = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            textDirection = TextDirection.Content
        )
        val sectionTitle = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textDirection = TextDirection.Content
        )
        val body = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            textDirection = TextDirection.Content
        )
        val bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            textDirection = TextDirection.Content
        )
        val label = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textDirection = TextDirection.Content
        )
        val overline = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textDirection = TextDirection.Content
        )
        val numeric = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textDirection = TextDirection.Ltr
        )
    }

    @Volatile
    private var styles: Styles = Styles(FontFamily.SansSerif)

    /** Installs the resolved font family. Call once before `setContent`. */
    fun install(family: FontFamily) {
        styles = Styles(family)
    }

    val display: TextStyle get() = styles.display
    val headline: TextStyle get() = styles.headline
    val title: TextStyle get() = styles.title
    val sectionTitle: TextStyle get() = styles.sectionTitle
    val body: TextStyle get() = styles.body
    val bodySmall: TextStyle get() = styles.bodySmall
    val label: TextStyle get() = styles.label
    val overline: TextStyle get() = styles.overline
    val numeric: TextStyle get() = styles.numeric

    val overflow = TextOverflow.Ellipsis

    fun material(): Typography = Typography(
        displaySmall = display,
        headlineMedium = headline,
        headlineSmall = title,
        titleLarge = title,
        titleMedium = sectionTitle,
        titleSmall = label,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = bodySmall,
        labelLarge = label,
        labelMedium = label,
        labelSmall = overline
    )
}
