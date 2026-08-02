package com.auralis.player.ui.screens.nowplaying

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackPosition
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.localizedStyle
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Drive Mode — a deliberately minimal, glanceable driving interface.
 *
 * Everything that needs a careful aim (queue, equaliser, visualiser, small
 * icons) is gone. What is left is oversized: a full-width play target, big
 * skip and ±seek buttons, a fat progress bar with elapsed/remaining time,
 * and optionally a single large lyric line. Swiping anywhere left or right
 * changes track, so the driver never has to hit a specific point. The layout
 * adapts to landscape, which is how most phone car mounts sit.
 */
@Composable
fun DriveModeScreen(
    song: Song?,
    isPlaying: Boolean,
    positionFlow: StateFlow<PlaybackPosition>,
    lyricLine: String?,
    showLyricLine: Boolean,
    keepScreenOn: Boolean,
    swipeGestures: Boolean,
    seekSeconds: Int,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    DisposableEffect(keepScreenOn) {
        val window = (context as? Activity)?.window
        if (keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val position by positionFlow.collectAsStateWithLifecycle()

    // High-contrast dark surface regardless of theme, for daylight legibility.
    val bg = Color(0xFF07090C)
    val onBg = Color(0xFFF4F6F9)
    val accent = AuralisTheme.colors.accent
    val seekMs = seekSeconds * 1000L

    val swipeModifier = if (swipeGestures) {
        Modifier.pointerInput(song?.id) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    if (abs(total) > 120f) {
                        if (total < 0) onNext() else onPrevious()
                    }
                }
            ) { _, dragAmount -> total += dragAmount }
        }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .then(swipeModifier)
    ) {
        val landscape = maxWidth > maxHeight
        val artSize = if (landscape) {
            (maxHeight * 0.62f).coerceAtMost(260.dp)
        } else {
            (maxWidth * 0.62f).coerceAtMost(320.dp)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ---- Minimal chrome: exit + favourite only -------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DriveIconButton(
                    icon = Icons.Rounded.Close,
                    label = "Exit Drive Mode",
                    tint = onBg,
                    size = 64.dp,
                    iconSize = 30.dp,
                    onClick = onExit
                )
                Text(
                    "DRIVE MODE",
                    style = AuralisType.overline,
                    color = onBg.copy(alpha = 0.45f)
                )
                DriveIconButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = "Favourite",
                    tint = if (isFavorite) accent else onBg,
                    size = 64.dp,
                    iconSize = 30.dp,
                    onClick = onToggleFavorite
                )
            }

            if (landscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    SongArtwork(
                        songId = song?.id ?: -1L,
                        modifier = Modifier
                            .size(artSize)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        TrackText(song, onBg, lyricLine, showLyricLine, compact = true)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SongArtwork(
                        songId = song?.id ?: -1L,
                        modifier = Modifier
                            .size(artSize)
                            .clip(RoundedCornerShape(28.dp))
                    )
                    Spacer(Modifier.height(24.dp))
                    TrackText(song, onBg, lyricLine, showLyricLine, compact = false)
                }
            }

            // ---- Progress + times -----------------------------------------
            val duration = position.durationMs.coerceAtLeast(0L)
            val fraction = if (duration > 0) {
                (position.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(onBg.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    Formatters.duration(position.positionMs),
                    style = AuralisType.numeric,
                    color = onBg.copy(alpha = 0.7f)
                )
                Text(
                    "-" + Formatters.duration((duration - position.positionMs).coerceAtLeast(0L)),
                    style = AuralisType.numeric,
                    color = onBg.copy(alpha = 0.7f)
                )
            }

            // ---- Oversized transport --------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DriveIconButton(
                    icon = Icons.Rounded.SkipPrevious,
                    label = "Previous",
                    tint = onBg,
                    size = 92.dp,
                    iconSize = 56.dp,
                    onClick = onPrevious
                )
                Box(
                    modifier = Modifier
                        .size(if (landscape) 96.dp else 120.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .combinedClickableCompat(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(if (landscape) 56.dp else 72.dp)
                    )
                }
                DriveIconButton(
                    icon = Icons.Rounded.SkipNext,
                    label = "Next",
                    tint = onBg,
                    size = 92.dp,
                    iconSize = 56.dp,
                    onClick = onNext
                )
            }

            // ---- Big ± seek ------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeekBlock(
                    icon = Icons.Rounded.Replay30,
                    label = "-${seekSeconds}s",
                    tint = onBg,
                    modifier = Modifier.weight(1f)
                ) { onSeekTo((position.positionMs - seekMs).coerceAtLeast(0L)) }
                SeekBlock(
                    icon = Icons.Rounded.Forward30,
                    label = "+${seekSeconds}s",
                    tint = onBg,
                    modifier = Modifier.weight(1f)
                ) {
                    val target = position.positionMs + seekMs
                    onSeekTo(if (duration > 0) target.coerceAtMost(duration) else target)
                }
            }
        }
    }
}

@Composable
private fun TrackText(
    song: Song?,
    onBg: Color,
    lyricLine: String?,
    showLyricLine: Boolean,
    compact: Boolean
) {
    val title = song?.title.orEmpty()
    val artist = song?.displayArtist.orEmpty()
    Text(
        text = title.ifBlank { "Nothing playing" },
        style = localizedStyle(
            if (compact) AuralisType.headline else AuralisType.display,
            title
        ).copy(fontWeight = FontWeight.Bold),
        color = onBg,
        textAlign = if (compact) TextAlign.Start else TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = artist,
        style = localizedStyle(AuralisType.title, artist),
        color = onBg.copy(alpha = 0.66f),
        textAlign = if (compact) TextAlign.Start else TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    )
    if (showLyricLine && !lyricLine.isNullOrBlank()) {
        Text(
            text = lyricLine,
            style = localizedStyle(AuralisType.headline, lyricLine),
            color = AuralisTheme.colors.accent,
            textAlign = if (compact) TextAlign.Start else TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
        )
    }
}

/** A large, forgiving tap target — no ripple, no small icons. */
@Composable
private fun DriveIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickableCompat(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun SeekBlock(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = 0.08f))
            .combinedClickableCompat(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(34.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, style = AuralisType.title, color = tint)
    }
}
