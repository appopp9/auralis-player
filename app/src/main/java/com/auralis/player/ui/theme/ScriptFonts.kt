package com.auralis.player.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Script-aware font selection.
 *
 * The bundled Vazir face is used only for text that actually contains Persian
 * / Arabic characters; Latin text keeps the platform family untouched, so an
 * English title never picks up the Persian face. Detection is per string, so a
 * mixed library renders each row with the correct family.
 */
object ScriptFonts {

    @Volatile
    private var persian: FontFamily? = null

    /** Resolves (and caches) the Persian font family from `res/font`. */
    fun persianFamily(context: Context): FontFamily {
        persian?.let { return it }
        val family = AuralisFonts.resolve(context.applicationContext)
        persian = family
        return family
    }

    /** True when the string holds at least one Persian / Arabic character. */
    fun containsRtl(text: String): Boolean {
        for (c in text) {
            if (c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F' ||
                c in '\u08A0'..'\u08FF' || c in '\uFB50'..'\uFDFF' ||
                c in '\uFE70'..'\uFEFF'
            ) {
                return true
            }
        }
        return false
    }
}

/** Remembers the resolved Persian (Vazir) font family. */
@Composable
fun rememberPersianFontFamily(): FontFamily {
    val context = LocalContext.current
    return remember { ScriptFonts.persianFamily(context) }
}

/**
 * Returns [base] with the Persian font applied when [text] contains RTL
 * characters (and [enabled]); otherwise returns [base] unchanged. Use for
 * song titles, artist names, lyrics and the alphabetical index.
 */
@Composable
fun localizedStyle(base: TextStyle, text: String, enabled: Boolean = true): TextStyle {
    if (!enabled) return base
    val persian = rememberPersianFontFamily()
    return remember(base, text, persian) {
        if (ScriptFonts.containsRtl(text)) base.copy(fontFamily = persian) else base
    }
}
