package com.auralis.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.ui.theme.AuralisTheme

/**
 * Artwork image tuned for list scrolling.
 *
 * - one plain [AsyncImage] instead of a subcomposing loader (no per-item
 *   subcomposition while scrolling)
 * - decode size capped per call site, so a 48dp row never decodes a full-size
 *   bitmap
 * - stable memory/disk cache keys, so re-visiting a row is a cache hit
 * - very short crossfade, so images do not "bloom" during fast flings
 */
@Composable
fun SongArtwork(
    songId: Long,
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.small,
    contentDescription: String? = null,
    fallbackIconSize: Dp = 22.dp,
    crossfadeMs: Int = 90,
    maxDecodeSize: Int = 256
) {
    val colors = AuralisTheme.colors
    val context = LocalContext.current
    val request = remember(songId, maxDecodeSize) {
        ImageRequest.Builder(context)
            .data(ArtworkLoader.uriFor(songId))
            .size(maxDecodeSize)
            .precision(Precision.INEXACT)
            .allowHardware(true)
            .crossfade(crossfadeMs)
            .memoryCacheKey("auralis-art-$songId-$maxDecodeSize")
            .diskCacheKey("auralis-art-$songId-$maxDecodeSize")
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(fallbackIconSize)
        )
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
