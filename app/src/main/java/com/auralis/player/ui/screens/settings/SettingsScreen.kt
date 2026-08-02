package com.auralis.player.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.AppTheme
import com.auralis.player.domain.model.GridStyle
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.ScanProgress
import com.auralis.player.domain.model.SortOrder
import com.auralis.player.domain.model.ThemeMode
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SegmentedControl
import com.auralis.player.ui.components.SliderSetting
import com.auralis.player.ui.components.SwitchSetting
import com.auralis.player.ui.components.appear
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.screens.library.sortLabel
import com.auralis.player.ui.screens.library.tabLabel
import com.auralis.player.ui.theme.AuralisColors
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.ThemeSpec
import com.auralis.player.ui.theme.ThemeSpecs
import com.auralis.player.ui.theme.display

// ---------------------------------------------------------------------------
// Settings — hub of categories, each opening a focused detail page.
// ---------------------------------------------------------------------------

private data class SettingsCategory(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

private val categories = listOf(
    SettingsCategory("appearance", Icons.Rounded.Palette, "Appearance", "Mode, accent, layout"),
    SettingsCategory("playback", Icons.Rounded.PlayCircle, "Playback", "Gapless, crossfade, speed, repeat"),
    SettingsCategory("audio", Icons.Rounded.GraphicEq, "Audio & Effects", "Equalizer, bass, balance, visualizer"),
    SettingsCategory("library", Icons.Rounded.LibraryMusic, "Library", "Scanning, folders, defaults"),
    SettingsCategory("lyrics", Icons.Rounded.Lyrics, "Lyrics", "Embedded and online lookup"),
    SettingsCategory("notifications", Icons.Rounded.Notifications, "Notifications", "Media controls and artwork"),
    SettingsCategory("backup", Icons.Rounded.Backup, "Backup & Restore", "Encrypted export of your data"),
    SettingsCategory("about", Icons.Rounded.Lock, "Privacy & About", "Offline-first, no tracking")
)

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
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var section by rememberSaveable { mutableStateOf("") }
    val motion = AuralisTheme.motion

    BackHandler(enabled = section.isNotEmpty()) { section = "" }

    AnimatedContent(
        targetState = section,
        transitionSpec = {
            val forward = targetState.isNotEmpty()
            (fadeIn(motion.tweenMedium()) + slideInHorizontally(motion.tweenMedium()) {
                if (forward) it / 8 else -it / 8
            }) togetherWith (fadeOut(motion.tweenFast()) + slideOutHorizontally(motion.tweenFast()) {
                if (forward) -it / 10 else it / 10
            })
        },
        label = "settingsSection",
        modifier = modifier.fillMaxSize()
    ) { current ->
        when (current) {
            "" -> SettingsHub(
                settings = settings,
                totalSongs = totalSongs,
                contentPadding = contentPadding,
                onUpdate = onUpdate,
                onOpen = { section = it }
            )
            else -> SettingsDetail(
                key = current,
                settings = settings,
                scan = scan,
                excludedFolders = excludedFolders,
                contentPadding = contentPadding,
                onUpdate = onUpdate,
                onRescan = onRescan,
                onIncludeFolder = onIncludeFolder,
                onOpenFolderBrowser = onOpenFolderBrowser,
                onOpenStats = onOpenStats,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                onBack = { section = "" }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Hub
// ---------------------------------------------------------------------------

@Composable
private fun SettingsHub(
    settings: AppSettings,
    totalSongs: Int,
    contentPadding: PaddingValues,
    onUpdate: (AppSettings) -> Unit,
    onOpen: (String) -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val spacing = AuralisTheme.spacing

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = spacing.screen, vertical = spacing.md)
            ) {
                Text("Settings", style = style.display(AuralisType.display), color = colors.textPrimary)
                Text(
                    "$totalSongs tracks • offline-first • no tracking",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        // Theme Studio stays on the hub — it is the app's hero setting.
        item {
            Text(
                "THEME STUDIO",
                style = AuralisType.overline,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = spacing.screen)
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                items(ThemeSpecs.all, key = { it.theme.name }) { spec ->
                    ThemePreviewCard(
                        spec = spec,
                        isDark = colors.isDark,
                        selected = settings.appTheme == spec.theme,
                        onClick = { onUpdate(settings.copy(appTheme = spec.theme)) }
                    )
                }
            }
        }

        categories.forEachIndexed { index, category ->
            item(key = category.key) {
                CategoryRow(
                    category = category,
                    index = index,
                    onClick = { onOpen(category.key) }
                )
            }
        }

        item { Spacer(Modifier.height(spacing.huge)) }
    }
}

@Composable
private fun CategoryRow(category: SettingsCategory, index: Int, onClick: () -> Unit) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    PressableSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xxs)
            .appear(index)
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(category.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(category.title, style = AuralisType.title, color = colors.textPrimary)
                    Text(category.subtitle, style = AuralisType.bodySmall, color = colors.textSecondary)
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detail pages
// ---------------------------------------------------------------------------

@Composable
private fun SettingsDetail(
    key: String,
    settings: AppSettings,
    scan: ScanProgress,
    excludedFolders: List<String>,
    contentPadding: PaddingValues,
    onUpdate: (AppSettings) -> Unit,
    onRescan: () -> Unit,
    onIncludeFolder: (String) -> Unit,
    onOpenFolderBrowser: () -> Unit,
    onOpenStats: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onBack: () -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val spacing = AuralisTheme.spacing
    val category = categories.firstOrNull { it.key == key } ?: return

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                AccentIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back to settings") { onBack() }
                Column {
                    Text(category.title, style = style.display(AuralisType.headline), color = colors.textPrimary)
                    Text(category.subtitle, style = AuralisType.bodySmall, color = colors.textSecondary)
                }
            }
        }

        when (key) {
            "appearance" -> appearancePage(settings, onUpdate)
            "playback" -> playbackPage(settings, onUpdate)
            "audio" -> audioPage(settings, onUpdate)
            "library" -> libraryPage(settings, scan, excludedFolders, onUpdate, onRescan, onIncludeFolder, onOpenFolderBrowser, onOpenStats)
            "lyrics" -> lyricsPage(settings, onUpdate)
            "notifications" -> notificationsPage(settings, onUpdate)
            "backup" -> backupPage(settings, onUpdate, onExportBackup, onImportBackup)
            "about" -> aboutPage()
        }

        item { Spacer(Modifier.height(spacing.huge)) }
    }
}

private fun LazyListScope.settingsCard(
    index: Int = 0,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    item {
        val spacing = AuralisTheme.spacing
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.md, vertical = spacing.xs)
                .appear(index)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                content = content
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text,
        style = AuralisType.overline,
        color = AuralisTheme.colors.textTertiary
    )
}

// ---- Appearance ------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.appearancePage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    settingsCard(0) {
        GroupLabel("MODE")
        val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
        val selected = when (settings.themeMode) {
            ThemeMode.LIGHT -> 1
            ThemeMode.DARK, ThemeMode.AMOLED -> 2
            else -> 0
        }
        SegmentedControl(
            options = listOf("System", "Light", "Dark"),
            selectedIndex = selected,
            onSelect = { onUpdate(settings.copy(themeMode = modes[it])) }
        )
    }

    settingsCard(1) {
        GroupLabel("START SCREEN")
        Text(
            "Which page opens when you launch the app",
            style = AuralisType.bodySmall,
            color = AuralisTheme.colors.textSecondary
        )
        val startOptions = listOf("home", "library", "playlists", "favorites")
        SegmentedControl(
            options = listOf("Home", "Library", "Playlists", "Likes"),
            selectedIndex = startOptions.indexOf(settings.startScreen).coerceAtLeast(0),
            onSelect = { onUpdate(settings.copy(startScreen = startOptions[it])) }
        )
    }

    settingsCard(2) {
        val colors = AuralisTheme.colors
        val spec = ThemeSpecs.of(settings.appTheme)
        GroupLabel("ACCENT")
        if (spec.signatureAccent == null && settings.appTheme != AppTheme.DYNAMIC) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm)
            ) {
                AccentPalette.entries.forEach { palette ->
                    val color = AuralisColors.swatch(palette, settings.customAccent)
                    val isSelected = settings.accent == palette
                    // popSpring under/overshoots past [0,1] — clamp before it
                    // reaches Color.copy(alpha) or border width, both of which
                    // throw on out-of-range values.
                    val ringRaw by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = AuralisTheme.motion.popSpring(),
                        label = "swatch"
                    )
                    val ring = ringRaw.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = (2.5f * ring).dp,
                                color = colors.textPrimary.copy(alpha = 0.8f * ring),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .combinedClickableCompat { onUpdate(settings.copy(accent = palette)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                tint = AuralisColors.readableOn(color),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Theme Builder — fine-tune a fully custom accent colour.
            if (settings.accent == AccentPalette.CUSTOM) {
                CustomAccentEditor(
                    customAccent = settings.customAccent,
                    onPick = { argb -> onUpdate(settings.copy(accent = AccentPalette.CUSTOM, customAccent = argb)) }
                )
            }
        } else {
            Text(
                when (settings.appTheme) {
                    AppTheme.DYNAMIC -> "Accent follows your wallpaper (Android 12+) or the current artwork."
                    else -> "${spec.label} carries its own signature palette."
                },
                style = AuralisType.bodySmall,
                color = colors.textTertiary
            )
        }
    }

    settingsCard(3) {
        GroupLabel("LAYOUT")
        SegmentedControl(
            options = listOf("List", "Grid"),
            selectedIndex = if (settings.gridStyle == GridStyle.LIST) 0 else 1,
            onSelect = { onUpdate(settings.copy(gridStyle = if (it == 0) GridStyle.LIST else GridStyle.GRID)) }
        )
        SwitchSetting("Animations", settings.animationsEnabled, description = "Motion across the whole app") {
            onUpdate(settings.copy(animationsEnabled = it))
        }
        SwitchSetting("Haptic feedback", settings.hapticsEnabled, description = "Tiny taps on interactions") {
            onUpdate(settings.copy(hapticsEnabled = it))
        }
    }

}

// ---- Playback ----------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.playbackPage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    settingsCard(0) {
        GroupLabel("ENGINE")
        SwitchSetting("Gapless playback", settings.gaplessEnabled) {
            onUpdate(settings.copy(gaplessEnabled = it))
        }
        SliderSetting(
            label = "Crossfade",
            value = settings.crossfadeSeconds.toFloat(),
            valueRange = 0f..12f,
            valueText = if (settings.crossfadeSeconds == 0) "Off" else "${settings.crossfadeSeconds}s",
            onValueChange = { onUpdate(settings.copy(crossfadeSeconds = it.toInt())) }
        )
        if (settings.crossfadeSeconds > 0 && settings.gaplessEnabled) {
            Text(
                "Crossfade overlaps the end of one track with the start of the next, " +
                    "so gapless playback cannot apply while it is on.",
                style = AuralisType.bodySmall,
                color = AuralisTheme.colors.textTertiary
            )
        }
        SliderSetting(
            label = "Playback speed",
            value = settings.playbackSpeed,
            valueRange = 0.25f..4f,
            valueText = String.format("%.2f×", settings.playbackSpeed),
            onValueChange = { onUpdate(settings.copy(playbackSpeed = (it * 4).toInt() / 4f)) }
        )
        SwitchSetting(
            "Keep pitch when changing speed",
            settings.pitchCorrection,
            description = "Off sounds like an old tape deck"
        ) { onUpdate(settings.copy(pitchCorrection = it)) }
        SwitchSetting(
            "Skip silence",
            settings.skipSilence,
            description = "Trim silent passages inside and between tracks"
        ) { onUpdate(settings.copy(skipSilence = it)) }
        SwitchSetting("ReplayGain normalization", settings.replayGainEnabled) {
            onUpdate(settings.copy(replayGainEnabled = it))
        }
    }

    settingsCard(1) {
        GroupLabel("BEHAVIOR")
        Text("Repeat", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        SegmentedControl(
            options = listOf("Off", "One", "All"),
            selectedIndex = settings.repeatMode.coerceIn(0, 2),
            onSelect = { onUpdate(settings.copy(repeatMode = it)) }
        )
        SwitchSetting("Shuffle", settings.shuffle) { onUpdate(settings.copy(shuffle = it)) }
        SwitchSetting("Resume playback on start", settings.resumeOnStart) {
            onUpdate(settings.copy(resumeOnStart = it))
        }
        SwitchSetting("Respect audio focus", settings.respectAudioFocus, description = "Pause when another app plays audio") {
            onUpdate(settings.copy(respectAudioFocus = it))
        }
        Text("Long-press seek amount", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        Text(
            "How far holding next / previous skips in the player",
            style = AuralisType.bodySmall,
            color = AuralisTheme.colors.textTertiary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            listOf(5, 10, 15, 30).forEach { seconds ->
                AuralisChip(
                    label = "${seconds}s",
                    selected = settings.seekIncrementMs == seconds * 1000
                ) { onUpdate(settings.copy(seekIncrementMs = seconds * 1000)) }
            }
        }
    }

    settingsCard(3) {
        GroupLabel("HEADPHONES & BLUETOOTH")
        SwitchSetting(
            "Play on connect",
            settings.autoPlayOnConnect,
            description = "Start playing when headphones or a car stereo connect"
        ) { onUpdate(settings.copy(autoPlayOnConnect = it)) }
        SwitchSetting(
            "Pause on disconnect",
            settings.pauseOnDisconnect,
            description = "Never blast music out of the phone speaker by accident"
        ) { onUpdate(settings.copy(pauseOnDisconnect = it)) }
        SwitchSetting(
            "Resume on reconnect",
            settings.resumeOnReconnect,
            description = "Pick the track back up when the same device comes back"
        ) { onUpdate(settings.copy(resumeOnReconnect = it)) }
    }

    settingsCard(2) {
        GroupLabel("DRIVE MODE")
        Text(
            "A stripped-back, glanceable player with oversized controls for the car",
            style = AuralisType.bodySmall,
            color = AuralisTheme.colors.textSecondary
        )
        SwitchSetting(
            "Show lyric line",
            settings.driveModeLyrics,
            description = "One large synced line, when lyrics exist"
        ) { onUpdate(settings.copy(driveModeLyrics = it)) }
        SwitchSetting(
            "Keep screen on",
            settings.driveModeKeepScreenOn,
            description = "Prevent the display from sleeping in Drive Mode"
        ) { onUpdate(settings.copy(driveModeKeepScreenOn = it)) }
        SwitchSetting(
            "Swipe to change track",
            settings.driveModeSwipeGestures,
            description = "Swipe anywhere left or right instead of aiming for a button"
        ) { onUpdate(settings.copy(driveModeSwipeGestures = it)) }
        Text("Skip buttons", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            listOf(10, 15, 30, 60).forEach { seconds ->
                AuralisChip(
                    label = "${seconds}s",
                    selected = settings.driveModeSeekSeconds == seconds
                ) { onUpdate(settings.copy(driveModeSeekSeconds = seconds)) }
            }
        }
    }
}

// ---- Audio -------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.audioPage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    settingsCard(0) {
        GroupLabel("EQUALIZER")
        SwitchSetting("Equalizer enabled", settings.eqEnabled) {
            onUpdate(settings.copy(eqEnabled = it))
        }
        Text("Bands", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        SegmentedControl(
            options = listOf("5 band", "10 band"),
            selectedIndex = if (settings.eqBandCount == 10) 1 else 0,
            onSelect = { onUpdate(settings.copy(eqBandCount = if (it == 1) 10 else 5)) }
        )
    }

    settingsCard(1) {
        GroupLabel("TONE")
        SliderSetting(
            label = "Bass boost",
            value = settings.bassBoost.toFloat(),
            valueRange = 0f..1000f,
            valueText = "${settings.bassBoost / 10}%",
            onValueChange = { onUpdate(settings.copy(bassBoost = it.toInt())) }
        )
        SliderSetting(
            label = "Treble",
            value = settings.trebleBoost.toFloat(),
            valueRange = 0f..1000f,
            valueText = "${settings.trebleBoost / 10}%",
            onValueChange = { onUpdate(settings.copy(trebleBoost = it.toInt())) }
        )
        SliderSetting(
            label = "Volume boost",
            value = settings.volumeBoost,
            valueRange = 1f..3f,
            valueText = String.format("%.1f×", settings.volumeBoost),
            onValueChange = { onUpdate(settings.copy(volumeBoost = it)) }
        )
        SliderSetting(
            label = "Balance",
            value = settings.balance,
            valueRange = -1f..1f,
            valueText = when {
                settings.balance < -0.05f -> "L ${(-settings.balance * 100).toInt()}%"
                settings.balance > 0.05f -> "R ${(settings.balance * 100).toInt()}%"
                else -> "Center"
            },
            onValueChange = { onUpdate(settings.copy(balance = it)) }
        )
        SwitchSetting(
            "Mono audio",
            settings.monoAudio,
            description = "Fold both channels into one — for listening with one ear or one earbud"
        ) { onUpdate(settings.copy(monoAudio = it)) }
    }

    settingsCard(5) {
        GroupLabel("LOUDNESS & SPACE")
        SliderSetting(
            label = "Target loudness",
            value = settings.targetLoudnessDb.toFloat(),
            valueRange = -24f..-6f,
            valueText = if (settings.targetLoudnessDb == -14) {
                "-14 dBFS (no change)"
            } else {
                "${settings.targetLoudnessDb} dBFS"
            },
            onValueChange = { onUpdate(settings.copy(targetLoudnessDb = it.toInt())) }
        )
        Text(
            "Quiet tracks are lifted toward this level. -14 dBFS is the streaming " +
                "convention and leaves everything untouched.",
            style = AuralisType.bodySmall,
            color = AuralisTheme.colors.textTertiary
        )
        Text("Reverb", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            listOf(
                "Off", "Small room", "Medium room", "Large room",
                "Medium hall", "Large hall", "Plate"
            ).forEachIndexed { index, label ->
                AuralisChip(
                    label = label,
                    selected = settings.reverbPreset == index
                ) { onUpdate(settings.copy(reverbPreset = index)) }
            }
        }
    }

    settingsCard(2) {
        GroupLabel("VISUALIZER")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            VisualizerMode.entries.forEach { mode ->
                AuralisChip(
                    label = mode.name.lowercase().replaceFirstChar { c -> c.uppercase() },
                    selected = settings.visualizerMode == mode
                ) { onUpdate(settings.copy(visualizerMode = mode)) }
            }
        }
        SliderSetting("Sensitivity", settings.visualizerSensitivity, 0.2f..3f,
            onValueChange = { onUpdate(settings.copy(visualizerSensitivity = it)) })
        SliderSetting("Smoothing", settings.visualizerSmoothing, 0f..0.95f,
            onValueChange = { onUpdate(settings.copy(visualizerSmoothing = it)) })
        SliderSetting("Intensity", settings.visualizerIntensity, 0.3f..2f,
            onValueChange = { onUpdate(settings.copy(visualizerIntensity = it)) })
        SliderSetting(
            label = "Bars",
            value = settings.visualizerBars.toFloat(),
            valueRange = 16f..128f,
            valueText = settings.visualizerBars.toString(),
            onValueChange = { onUpdate(settings.copy(visualizerBars = it.toInt())) }
        )
    }
}

// ---- Library -------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
private fun LazyListScope.libraryPage(
    settings: AppSettings,
    scan: ScanProgress,
    excludedFolders: List<String>,
    onUpdate: (AppSettings) -> Unit,
    onRescan: () -> Unit,
    onIncludeFolder: (String) -> Unit,
    onOpenFolderBrowser: () -> Unit,
    onOpenStats: () -> Unit
) {
    settingsCard(0) {
        val colors = AuralisTheme.colors
        GroupLabel("SCANNING")
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
            ActionSettingRow(Icons.Rounded.Refresh, "Rescan library", "Look for new and removed tracks", onRescan)
        }
        ActionSettingRow(Icons.Rounded.FolderOpen, "Folder browser", "Browse and exclude folders", onOpenFolderBrowser)
        ActionSettingRow(Icons.Rounded.Insights, "Listening statistics", "Your most played music", onOpenStats)
        SliderSetting(
            label = "Ignore tracks shorter than",
            value = settings.minTrackSeconds.toFloat(),
            valueRange = 0f..120f,
            valueText = "${settings.minTrackSeconds}s",
            onValueChange = { onUpdate(settings.copy(minTrackSeconds = it.toInt())) }
        )
    }

    settingsCard(1) {
        GroupLabel("DEFAULTS")
        Text("Start tab", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            LibraryTab.entries.forEach { tab ->
                AuralisChip(label = tabLabel(tab), selected = settings.defaultTab == tab) {
                    onUpdate(settings.copy(defaultTab = tab))
                }
            }
        }
        Text("Sort order", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
        ) {
            SortOrder.entries.forEach { order ->
                AuralisChip(label = sortLabel(order), selected = settings.songSort == order) {
                    onUpdate(settings.copy(songSort = order))
                }
            }
        }
    }

    settingsCard(2) {
        val colors = AuralisTheme.colors
        GroupLabel("EXCLUDED FOLDERS")
        if (excludedFolders.isEmpty()) {
            Text("None — everything on the device is scanned.", style = AuralisType.bodySmall, color = colors.textTertiary)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.xs)
            ) {
                excludedFolders.forEach { path ->
                    AuralisChip(label = "✕ ${path.substringAfterLast('/')}", selected = false) {
                        onIncludeFolder(path)
                    }
                }
            }
        }
    }
}

// ---- Lyrics / Notifications / About ------------------------------------------

private fun LazyListScope.lyricsPage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    settingsCard(0) {
        SwitchSetting("Embedded lyrics", settings.embeddedLyrics, description = "Read lyrics stored inside audio files") {
            onUpdate(settings.copy(embeddedLyrics = it))
        }
        SwitchSetting("Online lyrics lookup", settings.onlineLyrics, description = "Contacts lrclib.net only when enabled") {
            onUpdate(settings.copy(onlineLyrics = it))
        }
        SwitchSetting("Auto search when missing", settings.autoSearchLyrics) {
            onUpdate(settings.copy(autoSearchLyrics = it))
        }
        SwitchSetting("Floating lyrics on player", settings.floatingLyricsEnabled, description = "SoundCloud-style synced line that comes and goes over the player") {
            onUpdate(settings.copy(floatingLyricsEnabled = it))
        }
    }

    settingsCard(1) {
        GroupLabel("APPEARANCE")
        Text(
            "These preview live on the lyrics panel in the player.",
            style = AuralisType.bodySmall,
            color = AuralisTheme.colors.textTertiary
        )
        SliderSetting(
            label = "Font size",
            value = settings.lyricsFontSize,
            valueRange = 14f..40f,
            valueText = "${settings.lyricsFontSize.toInt()}sp",
            onValueChange = { onUpdate(settings.copy(lyricsFontSize = it)) }
        )
        SliderSetting(
            label = "Line spacing",
            value = settings.lyricsLineSpacing,
            valueRange = 1f..2.2f,
            valueText = String.format("%.2f×", settings.lyricsLineSpacing),
            onValueChange = { onUpdate(settings.copy(lyricsLineSpacing = it)) }
        )
        Text("Alignment", style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary)
        SegmentedControl(
            options = listOf("Start", "Center", "End"),
            selectedIndex = settings.lyricsAlign.coerceIn(0, 2),
            onSelect = { onUpdate(settings.copy(lyricsAlign = it)) }
        )
        SwitchSetting("Bold active line", settings.lyricsBoldActive, description = "Emphasise the line being sung") {
            onUpdate(settings.copy(lyricsBoldActive = it))
        }
        SwitchSetting("Persian font (Vazir)", settings.lyricsPersianFont, description = "Use Vazir for Persian lyrics and titles") {
            onUpdate(settings.copy(lyricsPersianFont = it))
        }
        SliderSetting(
            label = "Inactive line opacity",
            value = settings.lyricsInactiveAlpha,
            valueRange = 0.1f..0.9f,
            valueText = "${(settings.lyricsInactiveAlpha * 100).toInt()}%",
            onValueChange = { onUpdate(settings.copy(lyricsInactiveAlpha = it)) }
        )
    }

    settingsCard(2) {
        GroupLabel("TRANSLATION")
        SwitchSetting("Show translation", settings.lyricsShowTranslation, description = "Display translation under the original line") {
            onUpdate(settings.copy(lyricsShowTranslation = it))
        }
        SliderSetting(
            label = "Translation size",
            value = settings.lyricsTranslationScale,
            valueRange = 0.6f..1f,
            valueText = "${(settings.lyricsTranslationScale * 100).toInt()}%",
            onValueChange = { onUpdate(settings.copy(lyricsTranslationScale = it)) }
        )
        SliderSetting(
            label = "Gap above translation",
            value = settings.lyricsTranslationGap,
            valueRange = 0f..24f,
            valueText = "${settings.lyricsTranslationGap.toInt()}dp",
            onValueChange = { onUpdate(settings.copy(lyricsTranslationGap = it)) }
        )
    }
}

private fun LazyListScope.notificationsPage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    settingsCard(0) {
        SwitchSetting("Show notification", settings.showNotification) {
            onUpdate(settings.copy(showNotification = it))
        }
        SwitchSetting("Media controls", settings.notificationControls, description = "Previous, play/pause and next buttons") {
            onUpdate(settings.copy(notificationControls = it))
        }
        SwitchSetting("Artwork in notification", settings.notificationArtwork) {
            onUpdate(settings.copy(notificationArtwork = it))
        }
    }
}

private fun LazyListScope.backupPage(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    settingsCard(0) {
        val colors = AuralisTheme.colors
        GroupLabel("MANUAL")
        ActionSettingRow(
            Icons.Rounded.CloudUpload,
            "Export backup",
            "Favorites, pinned songs, playlists and theme",
            onExportBackup
        )
        ActionSettingRow(
            Icons.Rounded.CloudDownload,
            "Restore backup",
            "Pick an .aur file exported earlier",
            onImportBackup
        )
        Text(
            "Backups are encrypted (AES-256). Only Auralis can open them — on any device.",
            style = AuralisType.bodySmall,
            color = colors.textTertiary
        )
    }

    settingsCard(1) {
        val colors = AuralisTheme.colors
        GroupLabel("AUTOMATIC")
        SwitchSetting(
            label = "Daily auto backup",
            checked = settings.autoBackupEnabled,
            description = "Runs when you open the app, keeps the last 7 files"
        ) { onUpdate(settings.copy(autoBackupEnabled = it)) }
        if (settings.lastAutoBackupAt > 0L) {
            Text(
                "Last auto backup: " + java.text.SimpleDateFormat(
                    "d MMM yyyy, HH:mm",
                    java.util.Locale.US
                ).format(java.util.Date(settings.lastAutoBackupAt)),
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )
        }
        Text(
            "Stored in the app's private folder (Android/data). Use manual export for a copy that survives uninstalling.",
            style = AuralisType.bodySmall,
            color = colors.textTertiary
        )
    }
}

private fun LazyListScope.aboutPage() {
    settingsCard(0) {
        val colors = AuralisTheme.colors
        GroupLabel("PRIVACY")
        Text(
            "Auralis is offline-first. There is no account, no analytics and no background upload. " +
                "Your library, statistics and settings never leave this device.",
            style = AuralisType.bodySmall,
            color = colors.textSecondary
        )
        GroupLabel("VERSION")
        Text("Auralis 2.0 — Theme Studio release", style = AuralisType.bodySmall, color = colors.textSecondary)
    }
}

/** Tappable action row used inside settings cards. */
@Composable
private fun ActionSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AuralisTheme.shapes.small)
            .combinedClickableCompat { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AuralisType.body, color = colors.textPrimary)
            Text(subtitle, style = AuralisType.bodySmall, color = colors.textTertiary)
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Theme preview card
// ---------------------------------------------------------------------------

@Composable
private fun ThemePreviewCard(
    spec: ThemeSpec,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val accent = spec.signatureAccent ?: AuralisColors.Blue
    val preview = if (isDark) spec.dark(accent) else spec.light(accent)
    val borderColor by animateColorAsState(
        if (selected) colors.accent else colors.outline.copy(alpha = 0.6f),
        animationSpec = motion.tweenFast(),
        label = "themeCardBorder"
    )

    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .background(preview.background)
            .combinedClickableCompat(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(preview.accent, preview.accentAlt)))
        )
        Spacer(Modifier.height(8.dp))
        repeat(2) { index ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(preview.surfaceMuted)
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .fillMaxWidth(if (index == 0) 0.9f else 0.65f)
                        .clip(CircleShape)
                        .background(preview.textPrimary.copy(alpha = if (index == 0) 0.75f else 0.35f))
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spec.label, style = AuralisType.label, color = preview.textPrimary)
                Text(spec.tagline, style = AuralisType.overline, color = preview.textSecondary, maxLines = 1)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(preview.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = preview.onAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/** Theme Builder — hue/saturation/brightness sliders to build a custom accent. */
@Composable
private fun CustomAccentEditor(
    customAccent: Long,
    onPick: (Long) -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val hsv = remember(customAccent) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(customAccent.toInt(), it) }
    }
    var hue by remember(customAccent) { mutableStateOf(hsv[0]) }
    var sat by remember(customAccent) { mutableStateOf(hsv[1].coerceIn(0.35f, 1f)) }
    var bri by remember(customAccent) { mutableStateOf(hsv[2].coerceIn(0.55f, 1f)) }

    fun commit() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri))
        onPick(argb.toLong() and 0xFFFFFFFFL)
    }

    Column(modifier = Modifier.padding(top = spacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri))))
                    .border(1.dp, colors.outline, CircleShape)
            )
            Text(
                "Custom accent",
                style = AuralisType.label,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = spacing.sm)
            )
        }
        Text("Hue", style = AuralisType.overline, color = colors.textTertiary, modifier = Modifier.padding(top = spacing.sm))
        Slider(value = hue, onValueChange = { hue = it }, onValueChangeFinished = { commit() }, valueRange = 0f..360f)
        Text("Saturation", style = AuralisType.overline, color = colors.textTertiary)
        Slider(value = sat, onValueChange = { sat = it }, onValueChangeFinished = { commit() }, valueRange = 0.35f..1f)
        Text("Brightness", style = AuralisType.overline, color = colors.textTertiary)
        Slider(value = bri, onValueChange = { bri = it }, onValueChangeFinished = { commit() }, valueRange = 0.55f..1f)
    }
}
