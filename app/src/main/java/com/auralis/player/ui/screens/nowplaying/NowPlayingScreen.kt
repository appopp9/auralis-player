package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.playback.AbLoopState
import com.auralis.player.playback.PlayerUiState
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AudioVisualizer
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.WaveformSeekbar
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlin.math.abs

data class NowPlayingCallbacks(
    val onCollapse: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekFraction: (Float) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onToggleFavorite: () -> Unit,
    val onOpenQueue: () -> Unit,
    val onOpenSleepTimer: () -> Unit,
    val onOpenEqualizer: () -> Unit,
    val onOpenLyrics: () -> Unit,
    val onSongMenu: (Song) -> Unit,
    val onMarkLoopStart: () -> Unit,
    val onMarkLoopEnd: () -> Unit,
    val onToggleLoop: (Boolean) -> Unit,
    val onClearLoop: () -> Unit
)

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    isFavorite: Boolean,
    magnitudes: FloatArray,
    visualizerMode: VisualizerMode,
    visualizerIntensity: Float,
    visualizerSpeed: Float,
    loop: AbLoopState,
    sleepActive: Boolean,
    dynamicColor: Color?,
    callbacks: NowPlayingCallbacks,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val haptics = LocalHapticFeedback.current
    val song = state.currentSong
    var dragAccumulator by remember { mutableStateOf(0f) }

    val glow = dynamicColor ?: colors.accent
    val backgroundBrush = Brush.verticalGradient(
        listOf(
            glow.copy(alpha = if (colors.isDark) 0.34f else 0.20f),
            colors.background,
            colors.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .background(backgroundBrush)
            .pointerInput(song?.id) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator > 140f) callbacks.onCollapse()
                        if (dragAccumulator < -140f) callbacks.onOpenLyrics()
                        dragAccumulator = 0f
                    }
                ) { _, delta -> dragAccumulator += delta }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AccentIconButton(Icons.Rounded.ExpandMore, "Close now playing", callbacks.onCollapse)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Now playing", style = AuralisType.overline, color = colors.textTertiary)
                    Text(
                        text = song?.displayAlbum ?: "—",
                        style = AuralisType.label,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AccentIconButton(Icons.Rounded.MoreVert, "Track options") {
                    song?.let(callbacks.onSongMenu)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = spacing.md),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = song?.id ?: -1L,
                    transitionSpec = {
                        (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.92f))
                            .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.04f))
                    },
                    label = "artwork"
                ) { songId ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f)
                            .pointerInput(songId) {
                                var horizontal = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (horizontal < -120f) callbacks.onNext()
                                        if (horizontal > 120f) callbacks.onPrevious()
                                        horizontal = 0f
                                    }
                                ) { _, delta -> horizontal += delta }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SongArtwork(
                            songId = songId,
                            modifier = Modifier.fillMaxSize(),
                            shape = AuralisTheme.shapes.artwork,
                            fallbackIconSize = 72.dp,
                            contentDescription = song?.let { "Artwork for ${it.title}" }
                        )
                    }
                }
            }

            if (visualizerMode != VisualizerMode.OFF) {
                AudioVisualizer(
                    magnitudes = magnitudes,
                    mode = visualizerMode,
                    isPlaying = state.isPlaying,
                    primary = glow,
                    intensity = visualizerIntensity,
                    animationSpeed = visualizerSpeed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = spacing.sm)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: "Nothing playing",
                        style = AuralisType.headline,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song?.displayArtist ?: "Pick a track to start",
                        style = AuralisType.body,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Like button: the icon, the tint and the pop all react to the
                // state the very frame it flips, so taps never feel delayed.
                val likeScale = remember { Animatable(1f) }
                val likeTint by animateColorAsState(
                    targetValue = if (isFavorite) colors.accent else colors.textSecondary,
                    animationSpec = AuralisTheme.motion.tweenFast(),
                    label = "likeTint"
                )
                LaunchedEffect(isFavorite) {
                    if (isFavorite) {
                        likeScale.animateTo(1.32f, AuralisTheme.motion.popSpring())
                        likeScale.animateTo(1f, AuralisTheme.motion.bouncySpring())
                    } else {
                        likeScale.animateTo(1f, AuralisTheme.motion.softSpring())
                    }
                }
                AccentIconButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = likeTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = likeScale.value
                        scaleY = likeScale.value
                    },
                    onClick = callbacks.onToggleFavorite
                )
            }

            WaveformSeekbar(
                progress = state.progress,
                seed = song?.id ?: 0L,
                onSeek = callbacks.onSeekFraction,
                liveMagnitudes = if (state.isPlaying && magnitudes.isNotEmpty()) magnitudes else null,
                loopStart = loop.startMs?.let { start ->
                    if (state.durationMs > 0) start.toFloat() / state.durationMs else null
                },
                loopEnd = loop.endMs?.let { end ->
                    if (state.durationMs > 0) end.toFloat() / state.durationMs else null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.sm)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Formatters.duration(state.positionMs), style = AuralisType.numeric, color = colors.textSecondary)
                Text(Formatters.duration(state.durationMs), style = AuralisType.numeric, color = colors.textSecondary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccentIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                    tint = if (state.shuffle) colors.accent else colors.textTertiary,
                    onClick = callbacks.onToggleShuffle
                )
                AccentIconButton(Icons.Rounded.SkipPrevious, "Previous track", size = 56.dp, onClick = callbacks.onPrevious)
                AccentIconButton(
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    size = 72.dp,
                    filled = true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        callbacks.onPlayPause()
                    }
                )
                AccentIconButton(Icons.Rounded.SkipNext, "Next track", size = 56.dp, onClick = callbacks.onNext)
                AccentIconButton(
                    icon = if (state.repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when (state.repeatMode) {
                        1 -> "Repeat one"
                        2 -> "Repeat all"
                        else -> "Repeat off"
                    },
                    tint = if (state.repeatMode != 0) colors.accent else colors.textTertiary,
                    onClick = callbacks.onCycleRepeat
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AccentIconButton(Icons.Rounded.QueueMusic, "Queue", onClick = callbacks.onOpenQueue)
                AccentIconButton(Icons.Rounded.Lyrics, "Lyrics", onClick = callbacks.onOpenLyrics)
                AccentIconButton(
                    icon = Icons.Rounded.Bedtime,
                    contentDescription = "Sleep timer",
                    tint = if (sleepActive) colors.accent else null,
                    onClick = callbacks.onOpenSleepTimer
                )
                AccentIconButton(Icons.Rounded.Equalizer, "Equalizer", onClick = callbacks.onOpenEqualizer)
                LoopButton(loop = loop, callbacks = callbacks)
            }
        }
    }
}

@Composable
private fun LoopButton(loop: AbLoopState, callbacks: NowPlayingCallbacks) {
    val colors = AuralisTheme.colors
    val label = when {
        loop.ready && loop.enabled -> "A-B on"
        loop.ready -> "A-B"
        loop.startMs != null -> "Set B"
        else -> "Set A"
    }
    com.auralis.player.ui.components.AuralisChip(
        label = label,
        selected = loop.enabled,
        onClick = {
            when {
                loop.ready -> callbacks.onToggleLoop(!loop.enabled)
                loop.startMs != null -> callbacks.onMarkLoopEnd()
                else -> callbacks.onMarkLoopStart()
            }
        }
    )
}
