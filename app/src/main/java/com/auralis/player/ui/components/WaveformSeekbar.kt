package com.auralis.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.auralis.player.ui.theme.AuralisTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * Custom waveform scrubber. The waveform shape is derived deterministically
 * from the track id so every song keeps a stable, recognisable silhouette even
 * before decoding, and dragging seeks in real time.
 */
@Composable
fun WaveformSeekbar(
    progress: Float,
    seed: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    barCount: Int = 64,
    liveMagnitudes: FloatArray? = null,
    loopStart: Float? = null,
    loopEnd: Float? = null,
    contentDescription: String = "Playback position"
) {
    val colors = AuralisTheme.colors
    val haptic = LocalHapticFeedback.current
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableStateOf(1f) }

    val amplitudes = remember(seed, barCount) { waveformFor(seed, barCount) }
    val shown = dragProgress ?: progress
    val animatedProgress by animateFloatAsState(
        targetValue = shown,
        animationSpec = tween(if (dragProgress != null) 0 else 220),
        label = "waveProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(seed) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSeek(fraction)
                }
            }
            .pointerInput(seed) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val current = dragProgress ?: progress
                        dragProgress = (current + dragAmount / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragProgress?.let(onSeek)
                        dragProgress = null
                    },
                    onDragCancel = { dragProgress = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            width = size.width
            val count = amplitudes.size
            val slot = size.width / count
            val barWidth = (slot * 0.55f).coerceAtLeast(2f)
            val centerY = size.height / 2f
            val playedBrush = Brush.horizontalGradient(listOf(colors.accent, colors.accentGlow))

            if (loopStart != null && loopEnd != null && loopEnd > loopStart) {
                drawRoundRect(
                    color = colors.accent.copy(alpha = 0.14f),
                    topLeft = Offset(size.width * loopStart, 0f),
                    size = Size(size.width * (loopEnd - loopStart), size.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }

            for (i in 0 until count) {
                val live = liveMagnitudes?.getOrNull(i * (liveMagnitudes.size) / count)
                val amplitude = when {
                    live != null -> (amplitudes[i] * 0.45f + live * 0.75f).coerceIn(0.08f, 1f)
                    else -> amplitudes[i]
                }
                val barHeight = (size.height * 0.86f) * amplitude
                val x = i * slot + (slot - barWidth) / 2f
                val played = (i + 0.5f) / count <= animatedProgress
                drawBar(
                    x = x,
                    centerY = centerY,
                    barWidth = barWidth,
                    barHeight = barHeight,
                    brush = if (played) playedBrush else null,
                    color = if (played) null else colors.textTertiary.copy(alpha = 0.38f)
                )
            }

            // playhead
            val headX = (size.width * animatedProgress).coerceIn(0f, size.width)
            drawRoundRect(
                color = colors.textPrimary,
                topLeft = Offset(headX - 1.5f, size.height * 0.06f),
                size = Size(3f, size.height * 0.88f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

private fun DrawScope.drawBar(
    x: Float,
    centerY: Float,
    barWidth: Float,
    barHeight: Float,
    brush: Brush?,
    color: Color?
) {
    val top = centerY - barHeight / 2f
    val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
    if (brush != null) {
        drawRoundRect(brush = brush, topLeft = Offset(x, top), size = Size(barWidth, barHeight), cornerRadius = radius)
    } else if (color != null) {
        drawRoundRect(color = color, topLeft = Offset(x, top), size = Size(barWidth, barHeight), cornerRadius = radius)
    }
}

/** Deterministic pseudo-waveform so each track has a stable silhouette. */
fun waveformFor(seed: Long, count: Int): FloatArray {
    val random = java.util.Random(seed * 31 + count)
    return FloatArray(count) { index ->
        val phase = index.toFloat() / count * Math.PI.toFloat() * 6f
        val envelope = 0.45f + 0.35f * abs(sin(phase))
        val jitter = random.nextFloat() * 0.42f
        (envelope + jitter).coerceIn(0.12f, 1f)
    }
}
