package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackPosition
import com.auralis.player.ui.i18n.LocalStrings
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.PanelStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * Floating mini player. The whole bar opens the full player on tap; horizontal
 * drags change track.
 *
 * Performance contract: the play position is collected *inside*
 * [MiniProgressBar], so the 4 Hz position tick repaints two hairline boxes —
 * never this bar, never the scaffold, never the screen behind it.
 */
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<PlaybackPosition>,
    onExpand: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val motion = AuralisTheme.motion
    val style = AuralisTheme.style
    val strings = LocalStrings.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.985f else 1f,
        animationSpec = motion.popSpring(),
        label = "miniPress"
    )
    val haptic = LocalHapticFeedback.current
    val dragTotal = remember { FloatHolder(0f) }

    val shape = when (style.panel) {
        PanelStyle.OUTLINE -> AuralisTheme.shapes.small
        else -> AuralisTheme.shapes.card
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .themedContainer(shape)
            .combinedClickableCompat(
                interactionSource = interaction,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExpand()
                }
            )
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val total = dragTotal.value
                        dragTotal.value = 0f
                        when {
                            total <= -70f -> onNext()
                            total >= 70f -> onPrevious()
                        }
                    },
                    onDragCancel = { dragTotal.value = 0f }
                ) { _, dragAmount ->
                    dragTotal.value += dragAmount
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            SongArtwork(
                songId = song.id,
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(10.dp),
                contentDescription = null,
                maxDecodeSize = 192
            )
            AnimatedContent(
                targetState = song.id,
                transitionSpec = {
                    (slideInVertically(motion.tweenMedium()) { it / 3 } + fadeIn(motion.tweenFast())) togetherWith
                        (slideOutVertically(motion.tweenFast()) { -it / 3 } + fadeOut(motion.tweenFast()))
                },
                label = "miniMeta",
                modifier = Modifier.weight(1f)
            ) { _ ->
                Column {
                    Text(
                        text = song.title,
                        style = AuralisType.body,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.displayArtist,
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AccentIconButton(
                icon = Icons.Rounded.SkipPrevious,
                contentDescription = strings.previous,
                size = 38.dp
            ) { onPrevious() }
            PlayPauseButton(
                isPlaying = isPlaying,
                onClick = onTogglePlay,
                size = 44.dp,
                filled = false
            )
            AccentIconButton(
                icon = Icons.Rounded.SkipNext,
                contentDescription = strings.next,
                size = 38.dp
            ) { onNext() }
        }
        MiniProgressBar(positionFlow)
    }
}

/** Isolated 4 Hz consumer: only these two boxes repaint while music plays. */
@Composable
private fun MiniProgressBar(positionFlow: StateFlow<PlaybackPosition>) {
    val colors = AuralisTheme.colors
    val position by positionFlow.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(
        targetValue = position.progress,
        animationSpec = tween(260, easing = LinearEasing),
        label = "miniProgress"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .background(colors.surfaceMuted)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.5.dp)
                .background(accentBrush())
        )
    }
}

/** Play / pause control with a crisp icon morph, shared by mini and full player. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    filled: Boolean = true
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val style = AuralisTheme.style
    val strings = LocalStrings.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.9f else 1f,
        animationSpec = motion.popSpring(),
        label = "playPress"
    )
    val haptic = LocalHapticFeedback.current

    val fill = when {
        filled && style.gradientAccent -> Modifier.background(accentBrush())
        filled -> Modifier.background(colors.accent)
        else -> Modifier.background(colors.surfaceMuted)
    }
    val neumorphMod = if (style.panel == PanelStyle.NEUMORPH) {
        Modifier.neumorph(cornerRadius = size / 2, hi = colors.neumorphHi, lo = colors.neumorphLo, elevation = 5.dp)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(neumorphMod)
            .clip(CircleShape)
            .then(fill)
            .combinedClickableCompat(
                interactionSource = interaction,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (scaleIn(motion.popSpring(), initialScale = 0.6f) + fadeIn(motion.tweenFast())) togetherWith
                    (scaleOut(motion.tweenFast(), targetScale = 0.6f) + fadeOut(motion.tweenFast()))
            },
            label = "playPause"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) strings.pause else strings.play,
                tint = when {
                    filled && style.gradientAccent -> Color.White
                    filled -> colors.onAccent
                    else -> colors.textPrimary
                },
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

/** Tiny mutable float holder so drag totals survive gesture callbacks. */
internal class FloatHolder(var value: Float)
