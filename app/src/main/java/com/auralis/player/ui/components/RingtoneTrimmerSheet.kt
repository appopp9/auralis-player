package com.auralis.player.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle
import kotlin.math.min

/**
 * Ringtone Trimmer — pick a start/end window of the track and install that
 * segment as the device ringtone, or fall back to the full song. Trimming is a
 * container remux, so it works for M4A/AAC sources (MP3 is reported as
 * unsupported by the cutter, in which case "Set full song" is the path).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneTrimmerSheet(
    song: Song,
    onSetTrimmed: (startMs: Long, endMs: Long) -> Unit,
    onSetFull: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val duration = song.durationMs.coerceAtLeast(1L).toFloat()

    // Default selection: the first 30 seconds (or the whole song if shorter).
    var range by remember(song.id) {
        mutableStateOf(0f..min(30_000f, duration))
    }
    val startMs = range.start.toLong()
    val endMs = range.endInclusive.toLong()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screen)
                .padding(bottom = spacing.xxl)
        ) {
            Text(
                "Trim ringtone",
                style = AuralisTheme.style.display(AuralisType.title),
                color = colors.textPrimary
            )
            Text(
                "${song.title} • ${song.displayArtist}",
                style = localizedStyle(AuralisType.bodySmall, song.title),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(Formatters.duration(startMs), style = AuralisType.numeric, color = colors.accent)
                Text(
                    "${Formatters.duration(endMs - startMs)} selected",
                    style = AuralisType.label,
                    color = colors.textTertiary
                )
                Text(Formatters.duration(endMs), style = AuralisType.numeric, color = colors.accent)
            }

            RangeSlider(
                value = range,
                onValueChange = { range = it },
                valueRange = 0f..duration,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Trimming works with M4A/AAC files. For MP3, use “Set full song”.",
                style = AuralisType.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = spacing.sm)
            )

            AuralisChip(
                label = "Set trimmed ringtone",
                selected = true,
                onClick = {
                    onSetTrimmed(startMs, endMs)
                    onDismiss()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = spacing.lg)
            )
            TextButton(
                onClick = {
                    onSetFull()
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Set full song instead", color = colors.textSecondary)
            }
        }
    }
}
