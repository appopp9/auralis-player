package com.auralis.player.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.GridStyle
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.SortOrder
import com.auralis.player.domain.model.ThemeMode
import com.auralis.player.domain.model.VisualizerMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auralis_settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.VIOLET,
    val customAccent: Long = 0xFF7C5CFFL,
    val dynamicArtworkColor: Boolean = true,
    val animationsEnabled: Boolean = true,
    val visualizerMode: VisualizerMode = VisualizerMode.BARS,
    val visualizerSensitivity: Float = 1.0f,
    val visualizerSmoothing: Float = 0.6f,
    val visualizerBars: Int = 48,
    val visualizerSpeed: Float = 1.0f,
    val visualizerIntensity: Float = 0.85f,
    val gridStyle: GridStyle = GridStyle.LIST,
    val defaultTab: LibraryTab = LibraryTab.SONGS,
    val songSort: SortOrder = SortOrder.TITLE_ASC,
    val gaplessEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val replayGainEnabled: Boolean = false,
    val resumeOnStart: Boolean = true,
    val respectAudioFocus: Boolean = true,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val eqEnabled: Boolean = false,
    val eqBandCount: Int = 5,
    val eqLevels: String = "",
    val eqPresetName: String = "Flat",
    val bassBoost: Int = 0,
    val trebleBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGain: Int = 0,
    val balance: Float = 0f,
    val volumeBoost: Float = 1.0f,
    val embeddedLyrics: Boolean = true,
    val onlineLyrics: Boolean = false,
    val autoSearchLyrics: Boolean = false,
    val showNotification: Boolean = true,
    val notificationArtwork: Boolean = true,
    val notificationControls: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val firstScanDone: Boolean = false,
    val minTrackSeconds: Int = 20,
    val lastQueueSongId: Long = 0L
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
        val customAccent = longPreferencesKey("custom_accent")
        val dynamicArtwork = booleanPreferencesKey("dynamic_artwork")
        val animations = booleanPreferencesKey("animations")
        val visualizer = stringPreferencesKey("visualizer_mode")
        val visSensitivity = floatPreferencesKey("vis_sensitivity")
        val visSmoothing = floatPreferencesKey("vis_smoothing")
        val visBars = intPreferencesKey("vis_bars")
        val visSpeed = floatPreferencesKey("vis_speed")
        val visIntensity = floatPreferencesKey("vis_intensity")
        val gridStyle = stringPreferencesKey("grid_style")
        val defaultTab = stringPreferencesKey("default_tab")
        val songSort = stringPreferencesKey("song_sort")
        val gapless = booleanPreferencesKey("gapless")
        val crossfade = intPreferencesKey("crossfade")
        val replayGain = booleanPreferencesKey("replay_gain")
        val resume = booleanPreferencesKey("resume")
        val audioFocus = booleanPreferencesKey("audio_focus")
        val shuffle = booleanPreferencesKey("shuffle")
        val repeat = intPreferencesKey("repeat")
        val speed = floatPreferencesKey("speed")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqBands = intPreferencesKey("eq_bands")
        val eqLevels = stringPreferencesKey("eq_levels")
        val eqPreset = stringPreferencesKey("eq_preset")
        val bass = intPreferencesKey("bass_boost")
        val treble = intPreferencesKey("treble_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val loudness = intPreferencesKey("loudness")
        val balance = floatPreferencesKey("balance")
        val volumeBoost = floatPreferencesKey("volume_boost")
        val embeddedLyrics = booleanPreferencesKey("embedded_lyrics")
        val onlineLyrics = booleanPreferencesKey("online_lyrics")
        val autoSearchLyrics = booleanPreferencesKey("auto_search_lyrics")
        val showNotification = booleanPreferencesKey("show_notification")
        val notificationArtwork = booleanPreferencesKey("notification_artwork")
        val notificationControls = booleanPreferencesKey("notification_controls")
        val haptics = booleanPreferencesKey("haptics")
        val firstScan = booleanPreferencesKey("first_scan_done")
        val minTrackSeconds = intPreferencesKey("min_track_seconds")
        val lastQueueSong = longPreferencesKey("last_queue_song")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            accent = p[Keys.accent]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() } ?: AccentPalette.VIOLET,
            customAccent = p[Keys.customAccent] ?: 0xFF7C5CFFL,
            dynamicArtworkColor = p[Keys.dynamicArtwork] ?: true,
            animationsEnabled = p[Keys.animations] ?: true,
            visualizerMode = p[Keys.visualizer]?.let { runCatching { VisualizerMode.valueOf(it) }.getOrNull() } ?: VisualizerMode.BARS,
            visualizerSensitivity = p[Keys.visSensitivity] ?: 1f,
            visualizerSmoothing = p[Keys.visSmoothing] ?: 0.6f,
            visualizerBars = p[Keys.visBars] ?: 48,
            visualizerSpeed = p[Keys.visSpeed] ?: 1f,
            visualizerIntensity = p[Keys.visIntensity] ?: 0.85f,
            gridStyle = p[Keys.gridStyle]?.let { runCatching { GridStyle.valueOf(it) }.getOrNull() } ?: GridStyle.LIST,
            defaultTab = p[Keys.defaultTab]?.let { runCatching { LibraryTab.valueOf(it) }.getOrNull() } ?: LibraryTab.SONGS,
            songSort = p[Keys.songSort]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.TITLE_ASC,
            gaplessEnabled = p[Keys.gapless] ?: true,
            crossfadeSeconds = p[Keys.crossfade] ?: 0,
            replayGainEnabled = p[Keys.replayGain] ?: false,
            resumeOnStart = p[Keys.resume] ?: true,
            respectAudioFocus = p[Keys.audioFocus] ?: true,
            shuffle = p[Keys.shuffle] ?: false,
            repeatMode = p[Keys.repeat] ?: 0,
            playbackSpeed = p[Keys.speed] ?: 1f,
            eqEnabled = p[Keys.eqEnabled] ?: false,
            eqBandCount = p[Keys.eqBands] ?: 5,
            eqLevels = p[Keys.eqLevels] ?: "",
            eqPresetName = p[Keys.eqPreset] ?: "Flat",
            bassBoost = p[Keys.bass] ?: 0,
            trebleBoost = p[Keys.treble] ?: 0,
            virtualizer = p[Keys.virtualizer] ?: 0,
            loudnessGain = p[Keys.loudness] ?: 0,
            balance = p[Keys.balance] ?: 0f,
            volumeBoost = p[Keys.volumeBoost] ?: 1f,
            embeddedLyrics = p[Keys.embeddedLyrics] ?: true,
            onlineLyrics = p[Keys.onlineLyrics] ?: false,
            autoSearchLyrics = p[Keys.autoSearchLyrics] ?: false,
            showNotification = p[Keys.showNotification] ?: true,
            notificationArtwork = p[Keys.notificationArtwork] ?: true,
            notificationControls = p[Keys.notificationControls] ?: true,
            hapticsEnabled = p[Keys.haptics] ?: true,
            firstScanDone = p[Keys.firstScan] ?: false,
            minTrackSeconds = p[Keys.minTrackSeconds] ?: 20,
            lastQueueSongId = p[Keys.lastQueueSong] ?: 0L
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }
    suspend fun setAccent(accent: AccentPalette) = edit { it[Keys.accent] = accent.name }
    suspend fun setCustomAccent(color: Long) = edit { it[Keys.customAccent] = color }
    suspend fun setDynamicArtwork(enabled: Boolean) = edit { it[Keys.dynamicArtwork] = enabled }
    suspend fun setAnimations(enabled: Boolean) = edit { it[Keys.animations] = enabled }
    suspend fun setVisualizerMode(mode: VisualizerMode) = edit { it[Keys.visualizer] = mode.name }
    suspend fun setVisualizerSensitivity(value: Float) = edit { it[Keys.visSensitivity] = value }
    suspend fun setVisualizerSmoothing(value: Float) = edit { it[Keys.visSmoothing] = value }
    suspend fun setVisualizerBars(value: Int) = edit { it[Keys.visBars] = value }
    suspend fun setVisualizerSpeed(value: Float) = edit { it[Keys.visSpeed] = value }
    suspend fun setVisualizerIntensity(value: Float) = edit { it[Keys.visIntensity] = value }
    suspend fun setGridStyle(style: GridStyle) = edit { it[Keys.gridStyle] = style.name }
    suspend fun setDefaultTab(tab: LibraryTab) = edit { it[Keys.defaultTab] = tab.name }
    suspend fun setSongSort(order: SortOrder) = edit { it[Keys.songSort] = order.name }
    suspend fun setGapless(enabled: Boolean) = edit { it[Keys.gapless] = enabled }
    suspend fun setCrossfade(seconds: Int) = edit { it[Keys.crossfade] = seconds }
    suspend fun setReplayGain(enabled: Boolean) = edit { it[Keys.replayGain] = enabled }
    suspend fun setResume(enabled: Boolean) = edit { it[Keys.resume] = enabled }
    suspend fun setAudioFocus(enabled: Boolean) = edit { it[Keys.audioFocus] = enabled }
    suspend fun setShuffle(enabled: Boolean) = edit { it[Keys.shuffle] = enabled }
    suspend fun setRepeat(mode: Int) = edit { it[Keys.repeat] = mode }
    suspend fun setSpeed(value: Float) = edit { it[Keys.speed] = value }
    suspend fun setEqEnabled(enabled: Boolean) = edit { it[Keys.eqEnabled] = enabled }
    suspend fun setEqBandCount(count: Int) = edit { it[Keys.eqBands] = count }
    suspend fun setEqLevels(levels: String) = edit { it[Keys.eqLevels] = levels }
    suspend fun setEqPresetName(name: String) = edit { it[Keys.eqPreset] = name }
    suspend fun setBassBoost(value: Int) = edit { it[Keys.bass] = value }
    suspend fun setTrebleBoost(value: Int) = edit { it[Keys.treble] = value }
    suspend fun setVirtualizer(value: Int) = edit { it[Keys.virtualizer] = value }
    suspend fun setLoudness(value: Int) = edit { it[Keys.loudness] = value }
    suspend fun setBalance(value: Float) = edit { it[Keys.balance] = value }
    suspend fun setVolumeBoost(value: Float) = edit { it[Keys.volumeBoost] = value }
    suspend fun setEmbeddedLyrics(enabled: Boolean) = edit { it[Keys.embeddedLyrics] = enabled }
    suspend fun setOnlineLyrics(enabled: Boolean) = edit { it[Keys.onlineLyrics] = enabled }
    suspend fun setAutoSearchLyrics(enabled: Boolean) = edit { it[Keys.autoSearchLyrics] = enabled }
    suspend fun setShowNotification(enabled: Boolean) = edit { it[Keys.showNotification] = enabled }
    suspend fun setNotificationArtwork(enabled: Boolean) = edit { it[Keys.notificationArtwork] = enabled }
    suspend fun setNotificationControls(enabled: Boolean) = edit { it[Keys.notificationControls] = enabled }
    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.haptics] = enabled }
    suspend fun setFirstScanDone(done: Boolean) = edit { it[Keys.firstScan] = done }
    suspend fun setMinTrackSeconds(value: Int) = edit { it[Keys.minTrackSeconds] = value }
    suspend fun setLastQueueSong(id: Long) = edit { it[Keys.lastQueueSong] = id }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
