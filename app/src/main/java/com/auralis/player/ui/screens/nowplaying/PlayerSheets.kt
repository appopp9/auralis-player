package com.auralis.player.ui.screens.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.AbLoop
import com.auralis.player.domain.model.EqualizerPreset
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.AbLoopState
import com.auralis.player.playback.EqualizerState
import com.auralis.player.playback.SleepTimerState
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.SheetDivider
import com.auralis.player.ui.components.SheetHandle
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.screens.playlists.TextPromptDialog
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
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
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.screen),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Queue • ${queue.size}",
                    style = AuralisType.headline,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                AccentIconButton(Icons.Rounded.DeleteSweep, "Clear queue", onClick = onClear)
            }
            SheetDivider()

            if (queue.isEmpty()) {
                Text(
                    text = "The queue is empty. Play something to fill it up.",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(spacing.screen)
                )
                return@Column
            }

            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                itemsIndexed(queue, key = { index, song -> "${index}-${song.id}" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        com.auralis.player.ui.components.PressableSurface(
                            onClick = { onSelect(index) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                            ) {
                                SongArtwork(songId = song.id, modifier = Modifier.size(42.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = AuralisType.body,
                                        color = if (index == currentIndex) colors.accent else colors.textPrimary,
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
                        }
                        AccentIconButton(Icons.Rounded.ArrowUpward, "Move up", size = 34.dp) {
                            if (index > 0) onMove(index, index - 1)
                        }
                        AccentIconButton(Icons.Rounded.ArrowDownward, "Move down", size = 34.dp) {
                            if (index < queue.lastIndex) onMove(index, index + 1)
                        }
                        AccentIconButton(Icons.Rounded.Close, "Remove from queue", size = 34.dp) { onRemove(index) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var custom by remember { mutableStateOf(20f) }

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
            Text("Sleep timer", style = AuralisType.headline, color = colors.textPrimary)
            Text(
                text = when {
                    state.stopAtTrackEnd -> "Playback stops at the end of this track"
                    state.active -> "Stopping in ${Formatters.duration(state.remainingMs)}"
                    else -> "Auralis will pause playback when the timer ends"
                },
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                listOf(5, 10, 15, 30, 45, 60).forEach { minutes ->
                    AuralisChip(label = "$minutes min", selected = false) {
                        onStart(minutes)
                        onDismiss()
                    }
                }
                AuralisChip(label = "End of track", selected = state.stopAtTrackEnd) {
                    onEndOfTrack()
                    onDismiss()
                }
            }

            Text("Custom: ${custom.toInt()} min", style = AuralisType.label, color = colors.textPrimary)
            Slider(value = custom, onValueChange = { custom = it }, valueRange = 1f..180f)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                AuralisChip(label = "Start custom", selected = true) {
                    onStart(custom.toInt())
                    onDismiss()
                }
                if (state.active) {
                    AuralisChip(label = "Cancel timer", selected = false) {
                        onCancel()
                        onDismiss()
                    }
                }
            }
            Box(modifier = Modifier.height(spacing.md))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    builtInPresets: List<EqualizerPreset>,
    customPresets: List<EqualizerPreset>,
    loop: AbLoopState,
    savedLoops: List<AbLoop>,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onBassBoost: (Int) -> Unit,
    onTreble: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onLoudness: (Int) -> Unit,
    onApplyLoop: (AbLoop) -> Unit,
    onDeleteLoop: (Long) -> Unit,
    onSaveLoop: (String) -> Unit,
    onClearLoop: () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var savePresetDialog by remember { mutableStateOf(false) }
    var saveLoopDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Equalizer", style = AuralisType.headline, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Switch(checked = state.enabled, onCheckedChange = onToggleEnabled, enabled = state.available)
            }

            if (!state.available) {
                Text(
                    "This device did not expose an audio effects session. Start playback and reopen this panel.",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Text(
                    "${state.bandCount} bands • ${state.presetName}",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
                state.levelsMb.forEachIndexed { index, level ->
                    val freq = state.centerFrequencies.getOrNull(index) ?: 0
                    Column {
                        Text(
                            text = if (freq >= 1000) "${freq / 1000} kHz  ${level / 100} dB" else "$freq Hz  ${level / 100} dB",
                            style = AuralisType.bodySmall,
                            color = colors.textTertiary
                        )
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { onBandChange(index, it.toInt()) },
                            valueRange = state.minLevelMb.toFloat()..state.maxLevelMb.toFloat()
                        )
                    }
                }

                Text("Presets", style = AuralisType.title, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    builtInPresets.forEach { preset ->
                        AuralisChip(
                            label = preset.name,
                            selected = preset.name == state.presetName,
                            onClick = { onApplyPreset(preset) }
                        )
                    }
                    customPresets.forEach { preset ->
                        AuralisChip(
                            label = preset.name,
                            selected = preset.name == state.presetName,
                            onClick = { onApplyPreset(preset) }
                        )
                    }
                    AuralisChip(label = "Save current", selected = false) { savePresetDialog = true }
                }
                if (customPresets.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        customPresets.forEach { preset ->
                            AuralisChip(label = "Delete ${preset.name}", selected = false) {
                                onDeletePreset(preset.id)
                            }
                        }
                    }
                }

                SheetDivider()
                Text("Effects", style = AuralisType.title, color = colors.textPrimary)
                EffectSlider("Bass boost", state.bassBoost, 1000, state.bassBoostSupported, onBassBoost)
                EffectSlider("Treble", state.trebleBoost, 1000, true, onTreble)
                EffectSlider("Virtualizer / spatial", state.virtualizer, 1000, state.virtualizerSupported, onVirtualizer)
                EffectSlider("Loudness", state.loudness, 2000, state.loudnessSupported, onLoudness)
            }

            SheetDivider()
            Text("A-B loop", style = AuralisType.title, color = colors.textPrimary)
            Text(
                text = if (loop.ready) {
                    "${Formatters.duration(loop.startMs ?: 0)} → ${Formatters.duration(loop.endMs ?: 0)}"
                } else {
                    "Mark A and B on the player to create a loop region."
                },
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                if (loop.ready) {
                    AuralisChip(label = "Save loop", selected = false) { saveLoopDialog = true }
                    AuralisChip(label = "Clear loop", selected = false, onClick = onClearLoop)
                }
                savedLoops.forEach { saved ->
                    AuralisChip(label = saved.label, selected = false) { onApplyLoop(saved) }
                }
            }
            if (savedLoops.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    savedLoops.forEach { saved ->
                        AuralisChip(label = "Delete ${saved.label}", selected = false) { onDeleteLoop(saved.id) }
                    }
                }
            }
            Box(modifier = Modifier.height(spacing.lg))
        }
    }

    if (savePresetDialog) {
        TextPromptDialog(
            title = "Save preset",
            initial = "My preset",
            confirmLabel = "Save",
            onConfirm = {
                savePresetDialog = false
                if (it.isNotBlank()) onSavePreset(it)
            },
            onDismiss = { savePresetDialog = false }
        )
    }

    if (saveLoopDialog) {
        TextPromptDialog(
            title = "Save loop",
            initial = "Loop",
            confirmLabel = "Save",
            onConfirm = {
                saveLoopDialog = false
                if (it.isNotBlank()) onSaveLoop(it)
            },
            onDismiss = { saveLoopDialog = false }
        )
    }
}

@Composable
private fun EffectSlider(
    label: String,
    value: Int,
    max: Int,
    supported: Boolean,
    onChange: (Int) -> Unit
) {
    val colors = AuralisTheme.colors
    Column {
        Text(
            text = if (supported) "$label • ${value * 100 / max}%" else "$label • not supported",
            style = AuralisType.bodySmall,
            color = if (supported) colors.textSecondary else colors.textTertiary
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..max.toFloat(),
            enabled = supported
        )
    }
}
