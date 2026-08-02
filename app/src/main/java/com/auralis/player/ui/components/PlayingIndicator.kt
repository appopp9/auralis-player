package com.auralis.player.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme

/**
 * Animated equalizer bars shown for the currently-playing track (the Spotify /
 * Apple Music "now playing" signature). Three bars bounce out of phase while
 * the track plays and settle to a low, even level when paused.
 */
@Composable
fun PlayingIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = AuralisTheme.colors.accent,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 14.dp,
    minHeight: Dp = 3.dp
) {
    val motion = AuralisTheme.motion
    val transition = rememberInfiniteTransition(label = "playingBars")

    Row(
        modifier = modifier.height(maxHeight),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        repeat(3) { index ->
            val phase by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 420 + index * 130,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(index * 140)
                ),
                label = "bar$index"
            )
            val fraction = when {
                !motion.enabled -> 0.55f
                isPlaying -> phase
                else -> 0.28f
            }
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(minHeight + (maxHeight - minHeight) * fraction)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
