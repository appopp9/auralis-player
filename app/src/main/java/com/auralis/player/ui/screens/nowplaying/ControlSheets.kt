package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.core.Formatters
import com.auralis.player.playback.AbLoopState
import com.auralis.player.playback.PlaybackPosition
import com.auralis.player.playback.SleepTimerState
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.AuralisSlider
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SheetHandle
import com.auralis.player.ui.components.SwitchSetting
import com.auralis.player.ui.components.accentBrush
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlinx.coroutines.flow.StateFlow

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun SheetTitle(title: String, subtitle: String) {
    val colors = AuralisTheme.colors
    Column {
        Text(title, style = AuralisType.headline, color = colors.textPrimary)
        Text(subtitle, style = AuralisType.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun PrimaryPillButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        shape = AuralisTheme.shapes.chip,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(AuralisTheme.shapes.chip)
                .background(accentBrush())
                .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
                .padding(horizontal = 26.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = AuralisType.label, color = colors.onAccent, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GhostPillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = AuralisTheme.colors
    PressableSurface(onClick = onClick, shape = AuralisTheme.shapes.chip, modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(AuralisTheme.shapes.chip)
                .background(colors.surfaceMuted)
                .padding(horizontal = 22.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = AuralisType.label, color = colors.textPrimary)
        }
    }
}

// ---------------------------------------------------------------------------
// Sleep timer — slider-first design with a big live readout.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var minutes by rememberSaveable { mutableFloatStateOf(30f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            SheetTitle(
                title = "Sleep timer",
                subtitle = when {
                    state.stopAtTrackEnd -> "Playback stops at the end of this track"
                    state.active -> "Stopping in ${Formatters.duration(state.remainingMs)}"
                    else -> "Playback fades out and pauses when time is up"
                }
            )

            // Big readout
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = spacing.xs)) {
                Text(
                    text = minutes.toInt().toString(),
                    style = AuralisType.display,
                    color = colors.accent
                )
                Text(
                    text = "  minutes",
                    style = AuralisType.body,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            AuralisSlider(
                value = minutes,
                onValueChange = { raw ->
                    // snap to 5-minute steps
                    minutes = ((raw / 5f).toInt() * 5f).coerceIn(5f, 120f)
                },
                valueRange = 5f..120f
            )

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                listOf(15, 30, 45, 60, 90).forEach { preset ->
                    AuralisChip(label = "$preset", selected = minutes.toInt() == preset) {
                        minutes = preset.toFloat()
                    }
                }
            }

            SwitchSetting(
                label = "Stop at end of track",
                checked = state.stopAtTrackEnd,
                description = "Finishes the current song, then pauses"
            ) { wantOn ->
                if (wantOn) onEndOfTrack() else onCancel()
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryPillButton(
                    label = if (state.active) "Update timer" else "Start timer",
                    modifier = Modifier.weight(1f)
                ) {
                    onStart(minutes.toInt())
                    onDismiss()
                }
                if (state.active || state.stopAtTrackEnd) {
                    GhostPillButton(label = "Cancel", modifier = Modifier.weight(0.6f)) {
                        onCancel()
                        onDismiss()
                    }
                }
            }
            Box(modifier = Modifier.height(spacing.md))
        }
    }
}

// ---------------------------------------------------------------------------
// Playback speed
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeed: (Float) -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            SheetTitle(title = "Playback speed", subtitle = "Applies instantly, remembered across sessions")

            Text(
                text = String.format("%.2f×", currentSpeed),
                style = AuralisType.display,
                color = colors.accent
            )

            AuralisSlider(
                value = currentSpeed,
                onValueChange = { raw ->
                    // snap to 0.25 steps across the full 0.25×–4× range
                    val snapped = (raw * 4).toInt() / 4f
                    onSpeed(snapped.coerceIn(0.25f, 4f))
                },
                valueRange = 0.25f..4f
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f).forEach { preset ->
                    AuralisChip(
                        label = if (preset == preset.toInt().toFloat()) "${preset.toInt()}×" else "$preset×",
                        selected = kotlin.math.abs(currentSpeed - preset) < 0.011f
                    ) { onSpeed(preset) }
                }
            }
            Box(modifier = Modifier.height(spacing.lg))
        }
    }
}

// ---------------------------------------------------------------------------
// A-B loop — visual range over the track timeline.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbLoopSheet(
    loop: AbLoopState,
    positionFlow: StateFlow<PlaybackPosition>,
    onDismiss: () -> Unit,
    onMarkStart: () -> Unit,
    onMarkEnd: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onClear: () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            SheetTitle(
                title = "A-B repeat",
                subtitle = when {
                    loop.ready && loop.enabled -> "Looping between the two markers"
                    loop.ready -> "Range set — flip the switch to start looping"
                    loop.startMs != null -> "Now play to the end point and set B"
                    else -> "Repeat any section of this track"
                }
            )

            AbTimeline(loop = loop, positionFlow = positionFlow)

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                PrimaryPillButton(
                    label = loop.startMs?.let { "A · ${Formatters.duration(it)}" } ?: "Set A here",
                    modifier = Modifier.weight(1f)
                ) { onMarkStart() }
                PrimaryPillButton(
                    label = loop.endMs?.let { "B · ${Formatters.duration(it)}" } ?: "Set B here",
                    modifier = Modifier.weight(1f),
                    enabled = loop.startMs != null
                ) { onMarkEnd() }
            }

            SwitchSetting(
                label = "Loop enabled",
                checked = loop.enabled,
                description = if (loop.ready) null else "Set both markers first"
            ) { if (loop.ready) onToggle(it) }

            if (loop.startMs != null || loop.endMs != null) {
                GhostPillButton(label = "Clear markers", modifier = Modifier.fillMaxWidth()) {
                    onClear()
                }
            }
            Box(modifier = Modifier.height(spacing.md))
        }
    }
}

/** Track timeline with the loop range, A/B markers and a live playhead. */
@Composable
private fun AbTimeline(loop: AbLoopState, positionFlow: StateFlow<PlaybackPosition>) {
    val colors = AuralisTheme.colors
    val position by positionFlow.collectAsStateWithLifecycle()
    val duration = position.durationMs.coerceAtLeast(1L)
    val aFrac = (loop.startMs ?: 0L).toFloat() / duration
    val bFrac = (loop.endMs ?: duration).toFloat() / duration
    val playFrac = position.positionMs.toFloat() / duration
    val hasA = loop.startMs != null
    val hasB = loop.endMs != null
    val rangeColor by animateColorAsState(
        if (loop.enabled) colors.accent else colors.accent.copy(alpha = 0.45f),
        label = "abRange"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBehind {
                val trackH = 8.dp.toPx()
                val y = size.height / 2f
                val corner = CornerRadius(trackH / 2f)
                // base
                drawRoundRect(
                    color = colors.surfaceMuted,
                    topLeft = Offset(0f, y - trackH / 2f),
                    size = Size(size.width, trackH),
                    cornerRadius = corner
                )
                // loop range
                if (hasA || hasB) {
                    val x0 = size.width * aFrac.coerceIn(0f, 1f)
                    val x1 = size.width * bFrac.coerceIn(0f, 1f)
                    if (x1 > x0) {
                        drawRoundRect(
                            color = rangeColor,
                            topLeft = Offset(x0, y - trackH / 2f),
                            size = Size(x1 - x0, trackH),
                            cornerRadius = corner
                        )
                    }
                }
                // playhead
                val px = size.width * playFrac.coerceIn(0f, 1f)
                drawLine(
                    color = colors.textPrimary,
                    start = Offset(px, y - 16.dp.toPx()),
                    end = Offset(px, y + 16.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                // markers
                if (hasA) {
                    drawCircle(color = colors.accent, radius = 8.dp.toPx(), center = Offset(size.width * aFrac, y))
                    drawCircle(color = colors.onAccent, radius = 3.dp.toPx(), center = Offset(size.width * aFrac, y))
                }
                if (hasB) {
                    drawCircle(color = colors.accent, radius = 8.dp.toPx(), center = Offset(size.width * bFrac, y))
                    drawCircle(color = colors.onAccent, radius = 3.dp.toPx(), center = Offset(size.width * bFrac, y))
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(Formatters.duration(position.positionMs), style = AuralisType.numeric, color = colors.textSecondary)
            Text(Formatters.duration(position.durationMs), style = AuralisType.numeric, color = colors.textSecondary)
        }
    }
}
