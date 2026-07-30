package com.auralis.player.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.auralis.player.data.artwork.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ArtworkColorScheme(
    val primary: Color,
    val secondary: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color
)

/** Extracts a gradient + accent from the current artwork. */
@Singleton
class ArtworkColorExtractor @Inject constructor(
    private val artworkLoader: ArtworkLoader
) {
    private val cache = HashMap<Long, ArtworkColorScheme>()

    suspend fun schemeFor(songId: Long, isDark: Boolean): ArtworkColorScheme? {
        if (songId <= 0) return null
        cache[songId]?.let { return it }
        val bitmap = artworkLoader.load(songId, 256) ?: return null
        val scheme = withContext(Dispatchers.Default) { extract(bitmap, isDark) }
        cache[songId] = scheme
        return scheme
    }

    private fun extract(bitmap: Bitmap, isDark: Boolean): ArtworkColorScheme {
        val palette = Palette.from(bitmap).clearFilters().maximumColorCount(20).generate()
        val vibrant = palette.vibrantSwatch?.rgb
            ?: palette.lightVibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: 0xFF7C5CFF.toInt()
        val muted = palette.darkMutedSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: vibrant

        var primary = Color(vibrant)
        // keep accents readable regardless of theme
        val luminance = AuralisColors.luminance(primary)
        primary = when {
            isDark && luminance < 0.25f -> AuralisColors.mix(primary, Color.White, 0.35f)
            !isDark && luminance > 0.75f -> AuralisColors.mix(primary, Color.Black, 0.30f)
            else -> primary
        }
        val secondary = AuralisColors.accentGlow(primary)
        val base = Color(muted)
        return ArtworkColorScheme(
            primary = primary,
            secondary = secondary,
            backgroundTop = if (isDark) AuralisColors.mix(base, Color.Black, 0.45f)
            else AuralisColors.mix(base, Color.White, 0.72f),
            backgroundBottom = if (isDark) AuralisColors.mix(base, Color.Black, 0.86f)
            else AuralisColors.mix(base, Color.White, 0.94f)
        )
    }
}
