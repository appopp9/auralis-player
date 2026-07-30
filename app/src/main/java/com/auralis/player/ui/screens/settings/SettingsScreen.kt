package com.auralis.player.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.GridStyle
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.ScanProgress
import com.auralis.player.domain.model.SortOrder
import com.auralis.player.domain.model.ThemeMode
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.screens.library.sortLabel
import com.auralis.player.ui.screens.library.tabLabel
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.AuralisColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    scan: ScanProgress,
    excludedFolders: List<String>,
    totalSongs: Int,
    contentPadding: PaddingValues,
    onUpdate: (AppSettings) -> Unit,
    onRescan: () -> Unit,
    onIncludeFolder: (String) -> Unit,
    onOpenFolderBrowser: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = spacing.screen, vertical = spacing.sm)
            ) {
                Text("Settings", style = AuralisType.display, color = colors.textPrimary)
                Text(
                    "$totalSongs tracks • offline-first • no tracking",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // ---- Appearance ----
        item { SectionHeader(title = "Appearance") }
        item {
            SettingsCard {
                Text("Theme", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ThemeMode.entries.forEach { mode ->
                        AuralisChip(
                            label = themeLabel(mode),
                            selected = settings.themeMode == mode,
                            onClick = { onUpdate(settings.copy(themeMode = mode)) }
                        )
                    }
                }
                Text("Accent color", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AccentPalette.entries.filter { it != AccentPalette.CUSTOM }.forEach { palette ->
                        val color = AuralisColors.swatch(palette, settings.customAccent)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (settings.accent == palette) 3.dp else 0.dp,
                                    color = colors.textPrimary.copy(alpha = 0.7f),
                                    shape = CircleShape
                                )
                                .clickable { onUpdate(settings.copy(accent = palette)) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    listOf(
                        0xFF7C5CFFL to "Violet",
                        0xFFFF4D6DL to "Crimson",
                        0xFF00C2A8L to "Teal",
                        0xFFF2C14EL to "Amber",
                        0xFF4DA3FFL to "Sky"
                    ).forEach { (argb, name) ->
                        AuralisChip(
                            label = "Custom: $name",
                            selected = settings.accent == AccentPalette.CUSTOM && settings.customAccent == argb,
                            onClick = {
                                onUpdate(settings.copy(accent = AccentPalette.CUSTOM, customAccent = argb))
                            }
                        )
                    }
                }
                SwitchRow("Dynamic color from artwork", settings.dynamicArtworkColor) {
                    onUpdate(settings.copy(dynamicArtworkColor = it))
                }
                SwitchRow("Animations", settings.animationsEnabled) {
                    onUpdate(settings.copy(animationsEnabled = it))
                }
                SwitchRow("Haptic feedback", settings.hapticsEnabled) {
                    onUpdate(settings.copy(hapticsEnabled = it))
                }
                Text("Layout style", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    GridStyle.entries.forEach { style ->
                        AuralisChip(
                            label = if (style == GridStyle.LIST) "List" else "Grid",
                            selected = settings.gridStyle == style,
                            onClick = { onUpdate(settings.copy(gridStyle = style)) }
                        )
                    }
                }
            }
        }

        // ---- Visualizer ----
        item { SectionHeader(title = "Visualizer") }
        item {
            SettingsCard {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    VisualizerMode.entries.forEach { mode ->
                        AuralisChip(
                            label = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = settings.visualizerMode == mode,
                            onClick = { onUpdate(settings.copy(visualizerMode = mode)) }
                        )
                    }
                }
                SliderRow("Sensitivity", settings.visualizerSensitivity, 0.2f, 3f) {
                    onUpdate(settings.copy(visualizerSensitivity = it))
                }
                SliderRow("Smoothing", settings.visualizerSmoothing, 0f, 0.95f) {
                    onUpdate(settings.copy(visualizerSmoothing = it))
                }
                SliderRow("Animation speed", settings.visualizerSpeed, 0.3f, 2.5f) {
                    onUpdate(settings.copy(visualizerSpeed = it))
                }
                SliderRow("Intensity", settings.visualizerIntensity, 0.3f, 2f) {
                    onUpdate(settings.copy(visualizerIntensity = it))
                }
                SliderRow("Bars", settings.visualizerBars.toFloat(), 16f, 128f) {
                    onUpdate(settings.copy(visualizerBars = it.toInt()))
                }
            }
        }

        // ---- Playback ----
        item { SectionHeader(title = "Playback") }
        item {
            SettingsCard {
                SwitchRow("Gapless playback", settings.gaplessEnabled) { onUpdate(settings.copy(gaplessEnabled = it)) }
                SliderRow("Crossfade (${settings.crossfadeSeconds}s)", settings.crossfadeSeconds.toFloat(), 0f, 12f) {
                    onUpdate(settings.copy(crossfadeSeconds = it.toInt()))
                }
                SwitchRow("ReplayGain normalization", settings.replayGainEnabled) { onUpdate(settings.copy(replayGainEnabled = it)) }
                SwitchRow("Shuffle", settings.shuffle) { onUpdate(settings.copy(shuffle = it)) }
                Text("Repeat", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    listOf(0 to "Off", 1 to "One", 2 to "All").forEach { (mode, label) ->
                        AuralisChip(
                            label = label,
                            selected = settings.repeatMode == mode,
                            onClick = { onUpdate(settings.copy(repeatMode = mode)) }
                        )
                    }
                }
                SwitchRow("Resume playback on start", settings.resumeOnStart) { onUpdate(settings.copy(resumeOnStart = it)) }
                SwitchRow("Respect audio focus", settings.respectAudioFocus) { onUpdate(settings.copy(respectAudioFocus = it)) }
                SliderRow("Playback speed (${String.format("%.2f", settings.playbackSpeed)}x)", settings.playbackSpeed, 0.5f, 2f) {
                    onUpdate(settings.copy(playbackSpeed = it))
                }
            }
        }

        // ---- Audio ----
        item { SectionHeader(title = "Audio") }
        item {
            SettingsCard {
                SwitchRow("Equalizer enabled", settings.eqEnabled) { onUpdate(settings.copy(eqEnabled = it)) }
                Text("Band count", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    listOf(5, 10).forEach { count ->
                        AuralisChip(
                            label = "$count band",
                            selected = settings.eqBandCount == count,
                            onClick = { onUpdate(settings.copy(eqBandCount = count)) }
                        )
                    }
                }
                SliderRow("Bass boost", settings.bassBoost.toFloat(), 0f, 1000f) {
                    onUpdate(settings.copy(bassBoost = it.toInt()))
                }
                SliderRow("Treble", settings.trebleBoost.toFloat(), 0f, 1000f) {
                    onUpdate(settings.copy(trebleBoost = it.toInt()))
                }
                SliderRow("Volume boost", settings.volumeBoost, 0f, 1000f) {
                    onUpdate(settings.copy(volumeBoost = it))
                }
                SliderRow("Balance", settings.balance, -1f, 1f) { onUpdate(settings.copy(balance = it)) }
            }
        }

        // ---- Library ----
        item { SectionHeader(title = "Library") }
        item {
            SettingsCard {
                if (scan.running) {
                    Text(
                        "Scanning • ${scan.processed}/${scan.total}",
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary
                    )
                    LinearProgressIndicator(
                        progress = { if (scan.total > 0) scan.processed.toFloat() / scan.total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.accent
                    )
                } else {
                    AuralisChip(label = "Rescan library", selected = true, onClick = onRescan)
                }
                AuralisChip(label = "Open folder browser", selected = false, onClick = onOpenFolderBrowser)
                AuralisChip(label = "Listening statistics", selected = false, onClick = onOpenStats)

                SliderRow("Ignore tracks shorter than ${settings.minTrackSeconds}s", settings.minTrackSeconds.toFloat(), 0f, 120f) {
                    onUpdate(settings.copy(minTrackSeconds = it.toInt()))
                }

                Text("Default tab", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    LibraryTab.entries.forEach { tab ->
                        AuralisChip(
                            label = tabLabel(tab),
                            selected = settings.defaultTab == tab,
                            onClick = { onUpdate(settings.copy(defaultTab = tab)) }
                        )
                    }
                }

                Text("Default sort", style = AuralisType.label, color = colors.textPrimary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    SortOrder.entries.forEach { order ->
                        AuralisChip(
                            label = sortLabel(order),
                            selected = settings.songSort == order,
                            onClick = { onUpdate(settings.copy(songSort = order)) }
                        )
                    }
                }

                Text("Excluded folders", style = AuralisType.label, color = colors.textPrimary)
                if (excludedFolders.isEmpty()) {
                    Text("None", style = AuralisType.bodySmall, color = colors.textTertiary)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        excludedFolders.forEach { path ->
                            AuralisChip(label = "✕ ${path.substringAfterLast('/')}", selected = false, onClick = {
                                onIncludeFolder(path)
                            })
                        }
                    }
                }
            }
        }

        // ---- Lyrics ----
        item { SectionHeader(title = "Lyrics") }
        item {
            SettingsCard {
                SwitchRow("Embedded lyrics", settings.embeddedLyrics) { onUpdate(settings.copy(embeddedLyrics = it)) }
                SwitchRow("Online lyrics lookup", settings.onlineLyrics) { onUpdate(settings.copy(onlineLyrics = it)) }
                SwitchRow("Auto search when missing", settings.autoSearchLyrics) { onUpdate(settings.copy(autoSearchLyrics = it)) }
                Text(
                    "Online lookup contacts lrclib.net only when you enable it.",
                    style = AuralisType.bodySmall,
                    color = colors.textTertiary
                )
            }
        }

        // ---- Notifications ----
        item { SectionHeader(title = "Notifications") }
        item {
            SettingsCard {
                SwitchRow("Show notification", settings.showNotification) { onUpdate(settings.copy(showNotification = it)) }
                SwitchRow("Media controls", settings.notificationControls) { onUpdate(settings.copy(notificationControls = it)) }
                SwitchRow("Artwork in notification", settings.notificationArtwork) { onUpdate(settings.copy(notificationArtwork = it)) }
            }
        }

        // ---- Privacy ----
        item { SectionHeader(title = "Privacy") }
        item {
            SettingsCard {
                Text(
                    "Auralis is offline-first. There is no account, no analytics and no background upload. " +
                        "Your library, statistics and settings never leave this device.",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        item { Box(modifier = Modifier.padding(spacing.xl)) }
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val spacing = AuralisTheme.spacing
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            content = content
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = AuralisType.body, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    val colors = AuralisTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = AuralisType.bodySmall, color = colors.textSecondary)
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max)
    }
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
    ThemeMode.AMOLED -> "AMOLED"
}
