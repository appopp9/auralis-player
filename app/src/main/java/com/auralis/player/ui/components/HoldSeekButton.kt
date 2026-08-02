package com.auralis.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import kotlinx.coroutines.delay

private const val HOLD_THRESHOLD_MS = 300L
private const val REPEAT_INTERVAL_MS = 220L

/**
 * Transport button with press-and-hold scrubbing.
 *
 * A short tap skips a track. Holding starts a repeating seek that keeps running
 * for as long as the finger stays down and accelerates the longer it is held,
 * like a car stereo's fast-forward. Releasing stops it immediately, and the tap
 * action is suppressed so the user never skips a track by accident after
 * scrubbing.
 */
@Composable
fun HoldSeekButton(
    icon: ImageVector,
    label: String,
    stepMs: Long,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 30.dp,
    tint: Color? = null,
    onRepeatSeek: (Long) -> Unit,
    onTap: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val haptic = LocalHapticFeedback.current

    var holding by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val currentSeek by rememberUpdatedState(onRepeatSeek)
    val currentStep by rememberUpdatedState(stepMs)
    val currentTap by rememberUpdatedState(onTap)

    // The repeat loop is tied to the hold state, so it can never outlive the
    // gesture: releasing cancels the coroutine and the seeking stops at once.
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        // Wait out the hold threshold first, otherwise a normal tap would both
        // seek and skip.
        delay(HOLD_THRESHOLD_MS)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        var elapsed = 0L
        while (true) {
            val multiplier = when {
                elapsed < 1_000 -> 1
                elapsed < 3_000 -> 2
                else -> 4
            }
            currentSeek(currentStep * multiplier)
            delay(REPEAT_INTERVAL_MS)
            elapsed += REPEAT_INTERVAL_MS
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.86f else 1f,
        animationSpec = motion.popSpring(),
        label = "holdSeekPress"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(if (pressed) colors.accentSoft else Color.Transparent)
            .semantics {
                this.role = Role.Button
                this.contentDescription = label
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        // Finger down.
                        var event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) continue
                        pressed = true
                        holding = true
                        val downAt = System.currentTimeMillis()

                        // Hold until every pointer is lifted.
                        while (event.changes.any { it.pressed }) {
                            event = awaitPointerEvent()
                        }

                        pressed = false
                        holding = false
                        if (System.currentTimeMillis() - downAt < HOLD_THRESHOLD_MS) {
                            currentTap()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}
