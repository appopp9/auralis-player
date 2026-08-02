package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsCar
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.core.Formatters
import com.auralis.player.data.lyrics.LyricsParser
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.playback.AbLoopState
import com.auralis.player.playback.PlaybackPosition
import com.auralis.player.playback.PlayerUiState
import androidx.compose.foundation.gestures.detectTapGestures
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AudioVisualizer
import com.auralis.player.ui.components.HoldSeekButton
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.WaveformSeekbar
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.components.neumorph
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.BackdropStyle
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle
import kotlinx.coroutines.flow.StateFlow

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
    val onOpenSpeed: () -> Unit,
    val onOpenAbLoop: () -> Unit,
    val onSongMenu: (Song) -> Unit,
    val onMarkLoopStart: () -> Unit,
    val onMarkLoopEnd: () -> Unit,
    val onToggleLoop: (Boolean) -> Unit,
    val onClearLoop: () -> Unit,
    val onToggleLyricsOverlay: () -> Unit,
    val onSeekForward: () -> Unit,
    val onSeekBackward: () -> Unit,
    /** Relative seek in milliseconds; negative rewinds. Used by hold-to-scrub. */
    val onSeekBy: (Long) -> Unit,
    val onOpenCarMode: () -> Unit
)

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    positionFlow: StateFlow<PlaybackPosition>,
    isFavorite: Boolean,
    magnitudes: FloatArray,
    visualizerMode: VisualizerMode,
    visualizerIntensity: Float,
    visualizerSpeed: Float,
    loop: AbLoopState,
    sleepActive: Boolean,
    dynamicColor: Color?,
    callbacks: NowPlayingCallbacks,
    modifier: Modifier = Modifier,
    inlineLyrics: Lyrics? = null,
    showInlineLyrics: Boolean = false,
    inlineLyricsSettings: AppSettings = AppSettings(),
    /** Step applied per repeat while holding the skip buttons to scrub. */
    seekStepMs: Long = 10_000L
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val style = AuralisTheme.style
    val haptics = LocalHapticFeedback.current
    val song = state.currentSong
    var dragAccumulator by remember { mutableStateOf(0f) }

    // Artwork colour drives the stage only on artwork-driven backdrops;
    // signature themes (Luxury, Scandinavian, AMOLED...) keep their identity.
    val glow = if (style.backdrop == BackdropStyle.ARTWORK_BLUR) {
        dynamicColor ?: colors.accent
    } else {
        colors.accent
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
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
        PlayerBackdrop(songId = song?.id ?: -1L, glow = glow)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AccentIconButton(Icons.Rounded.ExpandMore, "Close now playing") {
                    callbacks.onCollapse()
                }
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
                // Drive mode also lives in the top bar: it is the one action a
                // user reaches for while already holding the phone in a car.
                AccentIconButton(Icons.Rounded.DirectionsCar, "Drive mode") {
                    callbacks.onOpenCarMode()
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
                val lyricsAvailable = inlineLyrics != null && inlineLyrics.lines.isNotEmpty()
                AnimatedContent(
                    targetState = showInlineLyrics && lyricsAvailable,
                    transitionSpec = {
                        (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.94f))
                            .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 1.04f))
                    },
                    label = "artworkLyrics"
                ) { lyricsMode ->
                    if (lyricsMode && inlineLyrics != null) {
                        InlineLyricsOverlay(
                            lyrics = inlineLyrics,
                            positionFlow = positionFlow,
                            settings = inlineLyricsSettings,
                            accent = glow,
                            onClick = callbacks.onToggleLyricsOverlay,
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .aspectRatio(1f)
                        )
                        return@AnimatedContent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .aspectRatio(1f)
                            .pointerInput(song?.id ?: -1L) {
                                var horizontal = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (horizontal < -120f) callbacks.onNext()
                                        if (horizontal > 120f) callbacks.onPrevious()
                                        horizontal = 0f
                                    }
                                ) { _, delta -> horizontal += delta }
                            }
                            .pointerInput(lyricsAvailable) {
                                detectTapGestures(
                                    onTap = { if (lyricsAvailable) callbacks.onToggleLyricsOverlay() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Colour aura behind the artwork (theme-gated).
                        if (style.artworkAura) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.94f)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(glow.copy(alpha = 0.45f), Color.Transparent)
                                        )
                                    )
                            )
                        }
                        val frame = when {
                            colors.neumorphHi.alpha > 0f -> Modifier.neumorph(
                                cornerRadius = 24.dp,
                                hi = colors.neumorphHi,
                                lo = colors.neumorphLo,
                                elevation = 10.dp
                            )
                            style.backdrop == BackdropStyle.LUXE_VIGNETTE -> Modifier.border(
                                1.dp, colors.hairline, AuralisTheme.shapes.artwork
                            )
                            else -> Modifier
                        }
                        SongArtwork(
                            songId = song?.id ?: -1L,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(frame),
                            shape = AuralisTheme.shapes.artwork,
                            fallbackIconSize = 72.dp,
                            contentDescription = song?.let { "Artwork for ${it.title}" },
                            maxDecodeSize = 1024
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
                        style = style.display(AuralisType.headline),
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
                val motion = AuralisTheme.motion
                val likeScale = remember { Animatable(1f) }
                val likeTint by animateColorAsState(
                    targetValue = if (isFavorite) colors.accent else colors.textSecondary,
                    animationSpec = motion.tweenFast(),
                    label = "likeTint"
                )
                LaunchedEffect(isFavorite) {
                    if (isFavorite) {
                        likeScale.animateTo(1.32f, motion.popSpring())
                        likeScale.animateTo(1f, motion.bouncySpring())
                    } else {
                        likeScale.animateTo(1f, motion.softSpring())
                    }
                }
                AccentIconButton(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = likeTint,
                    modifier = Modifier.graphicsLayer {
                        scaleX = likeScale.value
                        scaleY = likeScale.value
                    }
                ) { callbacks.onToggleFavorite() }
            }

            // Floating synced lyric line (SoundCloud-style), toggled in settings.
            if (inlineLyricsSettings.floatingLyricsEnabled && !showInlineLyrics) {
                FloatingLyricsLayer(
                    lyrics = inlineLyrics,
                    positionFlow = positionFlow,
                    settings = inlineLyricsSettings,
                    onOpenLyrics = callbacks.onOpenLyrics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.xs)
                )
            }

            // Seekbar + time labels collect the 4 Hz position stream in an
            // isolated subtree: playback never repaints the rest of the player.
            SeekSection(
                positionFlow = positionFlow,
                songId = song?.id ?: 0L,
                isPlaying = state.isPlaying,
                magnitudes = magnitudes,
                loop = loop,
                onSeekFraction = callbacks.onSeekFraction
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransportToggleButton(
                    active = state.shuffle,
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off"
                ) { callbacks.onToggleShuffle() }
                HoldSeekButton(
                    icon = Icons.Rounded.SkipPrevious,
                    label = "Previous track, hold to rewind",
                    stepMs = -seekStepMs,
                    size = 56.dp,
                    onRepeatSeek = { callbacks.onSeekBy(it) },
                    onTap = { callbacks.onPrevious() }
                )
                AccentIconButton(
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    size = 72.dp,
                    filled = true
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    callbacks.onPlayPause()
                }
                HoldSeekButton(
                    icon = Icons.Rounded.SkipNext,
                    label = "Next track, hold to fast forward",
                    stepMs = seekStepMs,
                    size = 56.dp,
                    onRepeatSeek = { callbacks.onSeekBy(it) },
                    onTap = { callbacks.onNext() }
                )
                TransportToggleButton(
                    active = state.repeatMode != 0,
                    icon = when (state.repeatMode) {
                        2 -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    badge = state.repeatMode == 1,
                    contentDescription = when (state.repeatMode) {
                        2 -> "Repeat one"
                        1 -> "Repeat all"
                        else -> "Repeat off"
                    }
                ) { callbacks.onCycleRepeat() }
            }

            PlayerActionBar(
                speed = state.speed,
                loopActive = loop.enabled,
                sleepActive = sleepActive,
                callbacks = callbacks,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md)
            )
        }
    }
}

/**
 * Bottom utility bar of the player: consistent icon + label actions with
 * clear active states. Speed shows its live value; A-B lights up while
 * looping; Sleep glows while a timer runs.
 */
@Composable
private fun PlayerActionBar(
    speed: Float,
    loopActive: Boolean,
    sleepActive: Boolean,
    callbacks: NowPlayingCallbacks,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    // Seven actions do not fit across a narrow phone. Without a scroll the Row
    // silently clips its last children off the right edge, which is exactly how
    // the Drive action became unreachable. Scrolling guarantees every action is
    // always reachable, whatever the screen width or font scale.
    Row(
        modifier = modifier
            .clip(AuralisTheme.shapes.card)
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.55f else 0.7f))
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerAction(icon = Icons.Rounded.QueueMusic, label = "Queue", active = false) {
            callbacks.onOpenQueue()
        }
        PlayerAction(icon = Icons.Rounded.Lyrics, label = "Lyrics", active = false) {
            callbacks.onOpenLyrics()
        }
        PlayerAction(
            text = formatSpeed(speed),
            label = "Speed",
            active = speed != 1f
        ) { callbacks.onOpenSpeed() }
        PlayerAction(text = "A·B", label = "Repeat", active = loopActive) {
            callbacks.onOpenAbLoop()
        }
        PlayerAction(icon = Icons.Rounded.Bedtime, label = "Sleep", active = sleepActive) {
            callbacks.onOpenSleepTimer()
        }
        PlayerAction(icon = Icons.Rounded.Equalizer, label = "EQ", active = false) {
            callbacks.onOpenEqualizer()
        }
        PlayerAction(icon = Icons.Rounded.DirectionsCar, label = "Drive", active = false) {
            callbacks.onOpenCarMode()
        }
    }
}

private fun formatSpeed(speed: Float): String {
    val text = if (speed == speed.toInt().toFloat()) {
        speed.toInt().toString()
    } else {
        String.format("%.2f", speed).trimEnd('0').trimEnd('.')
    }
    return "$text×"
}

@Composable
private fun PlayerAction(
    label: String,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    text: String? = null,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val tint by animateColorAsState(
        if (active) colors.accent else colors.textSecondary,
        animationSpec = motion.tweenFast(),
        label = "playerAction"
    )
    val pillAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = motion.tweenFast(),
        label = "playerActionPill"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AuralisTheme.shapes.small)
            .combinedClickableCompat { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 28.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(AuralisTheme.shapes.chip)
                    .background(colors.accentSoft)
            )
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(21.dp)
                )
            } else {
                Text(
                    text = text.orEmpty(),
                    style = AuralisType.label,
                    color = tint
                )
            }
        }
        Text(
            text = label,
            style = AuralisType.overline,
            color = tint.copy(alpha = 0.85f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/** Isolated 4 Hz subtree: seek bar plus the two time labels. */
@Composable
private fun SeekSection(
    positionFlow: StateFlow<PlaybackPosition>,
    songId: Long,
    isPlaying: Boolean,
    magnitudes: FloatArray,
    loop: AbLoopState,
    onSeekFraction: (Float) -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val position by positionFlow.collectAsStateWithLifecycle()

    WaveformSeekbar(
        progress = position.progress,
        seed = songId,
        onSeek = onSeekFraction,
        liveMagnitudes = if (isPlaying && magnitudes.isNotEmpty()) magnitudes else null,
        loopStart = loop.startMs?.let { start ->
            if (position.durationMs > 0) start.toFloat() / position.durationMs else null
        },
        loopEnd = loop.endMs?.let { end ->
            if (position.durationMs > 0) end.toFloat() / position.durationMs else null
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.sm)
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(Formatters.duration(position.positionMs), style = AuralisType.numeric, color = colors.textSecondary)
        Text(Formatters.duration(position.durationMs), style = AuralisType.numeric, color = colors.textSecondary)
    }
}

/**
 * Theme-defining player backdrop. Each design system stages the player
 * differently — this is the single most theme-expressive surface in the app.
 */
@Composable
private fun PlayerBackdrop(songId: Long, glow: Color) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style

    when (style.backdrop) {
        BackdropStyle.ARTWORK_BLUR -> {
            // Blurred artwork wash (Android 12+; older devices just see the
            // gradient) + readability scrim.
            if (songId > 0) {
                SongArtwork(
                    songId = songId,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(64.dp)
                        .graphicsLayer { alpha = if (colors.isDark) 0.55f else 0.4f },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                    crossfadeMs = 400,
                    maxDecodeSize = 96
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                glow.copy(alpha = if (colors.isDark) 0.30f else 0.18f),
                                colors.background.copy(alpha = 0.88f),
                                colors.background
                            )
                        )
                    )
            )
        }

        BackdropStyle.LUXE_VIGNETTE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(
                                    colors.accent.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.1f),
                                radius = size.width * 1.3f
                            )
                        )
                    }
            )
        }

        BackdropStyle.AURORA_MESH -> AuroraMesh()

        BackdropStyle.NEUMORPH_FRAME, BackdropStyle.FLAT, BackdropStyle.PURE_BLACK -> {
            // The plain canvas *is* the statement.
        }
    }
}

/**
 * Animated toggle used for shuffle / repeat. Cross-fades + pops the icon on
 * state change and lifts an accent pill behind it while active, so the
 * active/inactive state is obvious and the transition feels premium.
 */
@Composable
private fun TransportToggleButton(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    badge: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val haptic = LocalHapticFeedback.current

    val pillAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = motion.tweenFast(),
        label = "togglePill"
    )
    val tint by animateColorAsState(
        targetValue = if (active) colors.accent else colors.textTertiary,
        animationSpec = motion.tweenFast(),
        label = "toggleTint"
    )
    val popScale = remember { Animatable(1f) }
    LaunchedEffect(active, icon) {
        if (motion.enabled) {
            popScale.snapTo(0.7f)
            popScale.animateTo(1f, motion.popSpring())
        }
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = popScale.value
                scaleY = popScale.value
            }
            .clip(CircleShape)
            .combinedClickableCompat {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer { alpha = pillAlpha }
                .clip(CircleShape)
                .background(colors.accentSoft)
        )
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (scaleIn(motion.popSpring(), initialScale = 0.6f) + fadeIn(motion.tweenFast()))
                    .togetherWith(scaleOut(motion.tweenFast(), targetScale = 0.6f) + fadeOut(motion.tweenFast()))
            },
            label = "toggleIcon"
        ) { current ->
            Icon(
                imageVector = current,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 7.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(tint)
            )
        }
    }
}

/**
 * Floating synced-lyrics overlay that replaces the artwork on tap. Shows a
 * centered window of lines around the active one — bright, accented and
 * enlarged in the middle, fading with distance — over a soft scrim. Tapping
 * returns to the artwork.
 */
@Composable
private fun InlineLyricsOverlay(
    lyrics: Lyrics,
    positionFlow: StateFlow<PlaybackPosition>,
    settings: AppSettings,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    // Collect the 4 Hz position only while this overlay is composed (shown),
    // and derive the active synced line from it — this is what makes the
    // floating lyrics advance in time with the song.
    val position by positionFlow.collectAsStateWithLifecycle()
    val activeIndex = remember(lyrics, position.positionMs) {
        if (!lyrics.synchronized) {
            -1
        } else {
            var idx = -1
            val lines = lyrics.lines
            for (i in lines.indices) {
                if (lines[i].timeMs <= position.positionMs) idx = i else break
            }
            idx
        }
    }
    val window = remember(lyrics, activeIndex) {
        if (!lyrics.synchronized) {
            lyrics.lines.take(1).mapIndexed { i, line -> i to line }
        } else {
            val center = if (activeIndex < 0) 0 else activeIndex
            ((-2..2)).mapNotNull { off ->
                val idx = center + off
                lyrics.lines.getOrNull(idx)?.let { idx to it }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(AuralisTheme.shapes.artwork)
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.5f else 0.66f))
            .combinedClickableCompat { onClick() },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = activeIndex,
            transitionSpec = {
                (slideInVertically(motion.tweenMedium()) { it / 3 } + fadeIn(motion.tweenFast()))
                    .togetherWith(slideOutVertically(motion.tweenFast()) { -it / 3 } + fadeOut(motion.tweenFast()))
            },
            label = "inlineWindow",
            modifier = Modifier.align(Alignment.Center)
        ) { _ ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                window.forEach { (index, line) ->
                val active = index == activeIndex
                val distance = kotlin.math.abs(index - activeIndex)
                val alpha by animateFloatAsState(
                    targetValue = when {
                        active -> 1f
                        distance == 1 -> 0.55f
                        else -> 0.28f
                    },
                    animationSpec = motion.tweenMedium(),
                    label = "inlineAlpha$index"
                )
                val scale by animateFloatAsState(
                    targetValue = if (active) 1.04f else 0.94f,
                    animationSpec = motion.softSpring(),
                    label = "inlineScale$index"
                )
                val translation = if (settings.lyricsShowTranslation) {
                    LyricsParser.translationAt(lyrics.translationLines, line.timeMs)
                } else {
                    null
                }
                val pillColor by animateColorAsState(
                    targetValue = if (active) colors.accentSoft else Color.Transparent,
                    animationSpec = motion.tweenMedium(),
                    label = "inlinePill$index"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            this.alpha = alpha
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(AuralisTheme.shapes.small)
                        .background(pillColor)
                        .padding(vertical = 5.dp, horizontal = 10.dp)
                ) {
                    Text(
                        text = line.text.ifBlank { "♪" },
                        style = localizedStyle(
                            AuralisType.title.copy(
                                fontWeight = if (active) {
                                    if (settings.lyricsBoldActive) {
                                        androidx.compose.ui.text.font.FontWeight.Bold
                                    } else {
                                        androidx.compose.ui.text.font.FontWeight.SemiBold
                                    }
                                } else {
                                    androidx.compose.ui.text.font.FontWeight.Medium
                                },
                                fontSize = if (active) {
                                    (settings.lyricsFontSize * 0.92f).coerceIn(16f, 30f).sp
                                } else {
                                    (settings.lyricsFontSize * 0.74f).coerceIn(13f, 24f).sp
                                },
                                lineHeight = (settings.lyricsFontSize * settings.lyricsLineSpacing * 0.95f).sp
                            ),
                            line.text,
                            settings.lyricsPersianFont
                        ),
                        color = if (active) accent else colors.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (active && !translation.isNullOrBlank()) {
                        Text(
                            text = translation,
                            style = localizedStyle(
                                AuralisType.bodySmall.copy(
                                    fontSize = (settings.lyricsFontSize * settings.lyricsTranslationScale * 0.82f).sp
                                ),
                                translation,
                                true
                            ),
                            color = colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
            }
        }
    }
}

/** Slow-drifting gradient blobs for the Experimental theme. GPU-only work. */
@Composable
private fun AuroraMesh() {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    if (!motion.enabled) return
    val transition = rememberInfiniteTransition(label = "mesh")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meshT"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = t * 120f - 60f }
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accent.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * (0.15f + t * 0.2f), size.height * 0.12f),
                        radius = size.width * 1.1f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(colors.accentAlt.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * (0.85f - t * 0.2f), size.height * 0.4f),
                        radius = size.width * 1.0f
                    )
                )
            }
    )
}

