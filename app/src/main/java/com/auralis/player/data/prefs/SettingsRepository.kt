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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.AppTheme
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
    val appTheme: AppTheme = AppTheme.AURORA,
    val accent: AccentPalette = AccentPalette.BLUE,
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
    /** Trim silent passages inside and between tracks (A4). */
    val skipSilence: Boolean = false,
    /** Time-stretch instead of tape-deck pitch shifting when speed != 1 (A5). */
    val pitchCorrection: Boolean = true,
    /** Target loudness in dBFS, -24..-6. -14 is the streaming default = no change (A3). */
    val targetLoudnessDb: Int = -14,
    /** 0 = off, 1..6 = small room / medium room / large room / medium hall / large hall / plate (A6). */
    val reverbPreset: Int = 0,
    /** Fold stereo down to mono, for single-sided hearing loss (A8). */
    val monoAudio: Boolean = false,
    /** Remember effect settings per output device (A7). */
    val perDeviceProfiles: Boolean = false,
    /** Bluetooth / headset behaviour (D20). */
    val autoPlayOnConnect: Boolean = false,
    val pauseOnDisconnect: Boolean = true,
    val resumeOnReconnect: Boolean = false,
    /** Auto Drive Mode on car Bluetooth — off by default (D21). */
    val autoDriveMode: Boolean = false,
    val autoDriveModeNotify: Boolean = true,
    val embeddedLyrics: Boolean = true,
    val onlineLyrics: Boolean = false,
    val autoSearchLyrics: Boolean = false,
    val showNotification: Boolean = true,
    val notificationArtwork: Boolean = true,
    val notificationControls: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val firstScanDone: Boolean = false,
    val minTrackSeconds: Int = 20,
    val lastQueueSongId: Long = 0L,
    /** Route shown on launch: home / library / playlists / favorites. */
    val startScreen: String = "home",
    val autoBackupEnabled: Boolean = false,
    val lastAutoBackupAt: Long = 0L,
    /** Songs pinned to the top of the Songs list. */
    val pinnedSongs: Set<Long> = emptySet(),
    /** Playlists pinned to the top of the Playlists list. */
    val pinnedPlaylists: Set<Long> = emptySet(),
    /** Long-press seek increment for next/previous, milliseconds. */
    val seekIncrementMs: Int = 10_000,
    /** Lyrics appearance settings (live-previewed in the lyrics panel). */
    val lyricsFontSize: Float = 22f,
    val lyricsBoldActive: Boolean = true,
    val lyricsLineSpacing: Float = 1.35f,
    /** 0 = start, 1 = center, 2 = end. */
    val lyricsAlign: Int = 1,
    val lyricsShowTranslation: Boolean = true,
    val lyricsTranslationGap: Float = 6f,
    val lyricsTranslationScale: Float = 0.78f,
    val lyricsInactiveAlpha: Float = 0.38f,
    val lyricsPersianFont: Boolean = true,
    /** SoundCloud-style floating synced lyric line over the player. */
    val floatingLyricsEnabled: Boolean = false,
    /** Drive Mode: show the current synced lyric line in huge type. */
    val driveModeLyrics: Boolean = true,
    /** Drive Mode: keep the screen awake while the mode is open. */
    val driveModeKeepScreenOn: Boolean = true,
    /** Drive Mode: swipe left/right anywhere to change track. */
    val driveModeSwipeGestures: Boolean = true,
    /** Drive Mode: skip amount in seconds for the big seek buttons. */
    val driveModeSeekSeconds: Int = 15,
    /** Per-song manual lyrics sync offset in milliseconds (songId → offsetMs). */
    val lyricsOffsets: Map<Long, Int> = emptyMap()
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val appTheme = stringPreferencesKey("app_theme")
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
        val skipSilence = booleanPreferencesKey("skip_silence")
        val pitchCorrection = booleanPreferencesKey("pitch_correction")
        val targetLoudness = intPreferencesKey("target_loudness_db")
        val reverbPreset = intPreferencesKey("reverb_preset")
        val monoAudio = booleanPreferencesKey("mono_audio")
        val perDeviceProfiles = booleanPreferencesKey("per_device_profiles")
        val autoPlayOnConnect = booleanPreferencesKey("auto_play_on_connect")
        val pauseOnDisconnect = booleanPreferencesKey("pause_on_disconnect")
        val resumeOnReconnect = booleanPreferencesKey("resume_on_reconnect")
        val autoDriveMode = booleanPreferencesKey("auto_drive_mode")
        val autoDriveModeNotify = booleanPreferencesKey("auto_drive_mode_notify")
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
        val pinnedSongs = stringSetPreferencesKey("pinned_songs")
        val startScreen = stringPreferencesKey("start_screen")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val lastAutoBackup = longPreferencesKey("last_auto_backup")
        val pinnedPlaylists = stringSetPreferencesKey("pinned_playlists")
        val seekIncrement = intPreferencesKey("seek_increment_ms")
        val lyricsFontSize = floatPreferencesKey("lyrics_font_size")
        val lyricsBoldActive = booleanPreferencesKey("lyrics_bold_active")
        val lyricsLineSpacing = floatPreferencesKey("lyrics_line_spacing")
        val lyricsAlign = intPreferencesKey("lyrics_align")
        val lyricsShowTranslation = booleanPreferencesKey("lyrics_show_translation")
        val lyricsTranslationGap = floatPreferencesKey("lyrics_translation_gap")
        val lyricsTranslationScale = floatPreferencesKey("lyrics_translation_scale")
        val lyricsInactiveAlpha = floatPreferencesKey("lyrics_inactive_alpha")
        val lyricsPersianFont = booleanPreferencesKey("lyrics_persian_font")
        val floatingLyrics = booleanPreferencesKey("floating_lyrics")
        val driveModeLyrics = booleanPreferencesKey("drive_mode_lyrics")
        val driveModeKeepScreenOn = booleanPreferencesKey("drive_mode_keep_screen_on")
        val driveModeSwipe = booleanPreferencesKey("drive_mode_swipe")
        val driveModeSeekSeconds = intPreferencesKey("drive_mode_seek_seconds")
        val lyricsOffsets = stringSetPreferencesKey("lyrics_offsets")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            appTheme = p[Keys.appTheme]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.AURORA,
            accent = p[Keys.accent]?.let { runCatching { AccentPalette.valueOf(it) }.getOrNull() } ?: AccentPalette.BLUE,
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
            skipSilence = p[Keys.skipSilence] ?: false,
            pitchCorrection = p[Keys.pitchCorrection] ?: true,
            targetLoudnessDb = p[Keys.targetLoudness] ?: -14,
            reverbPreset = p[Keys.reverbPreset] ?: 0,
            monoAudio = p[Keys.monoAudio] ?: false,
            perDeviceProfiles = p[Keys.perDeviceProfiles] ?: false,
            autoPlayOnConnect = p[Keys.autoPlayOnConnect] ?: false,
            pauseOnDisconnect = p[Keys.pauseOnDisconnect] ?: true,
            resumeOnReconnect = p[Keys.resumeOnReconnect] ?: false,
            autoDriveMode = p[Keys.autoDriveMode] ?: false,
            autoDriveModeNotify = p[Keys.autoDriveModeNotify] ?: true,
            embeddedLyrics = p[Keys.embeddedLyrics] ?: true,
            onlineLyrics = p[Keys.onlineLyrics] ?: false,
            autoSearchLyrics = p[Keys.autoSearchLyrics] ?: false,
            showNotification = p[Keys.showNotification] ?: true,
            notificationArtwork = p[Keys.notificationArtwork] ?: true,
            notificationControls = p[Keys.notificationControls] ?: true,
            hapticsEnabled = p[Keys.haptics] ?: true,
            firstScanDone = p[Keys.firstScan] ?: false,
            minTrackSeconds = p[Keys.minTrackSeconds] ?: 20,
            lastQueueSongId = p[Keys.lastQueueSong] ?: 0L,
            pinnedSongs = p[Keys.pinnedSongs]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet(),
            startScreen = p[Keys.startScreen] ?: "home",
            autoBackupEnabled = p[Keys.autoBackup] ?: false,
            lastAutoBackupAt = p[Keys.lastAutoBackup] ?: 0L,
            pinnedPlaylists = p[Keys.pinnedPlaylists]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet(),
            seekIncrementMs = p[Keys.seekIncrement] ?: 10_000,
            lyricsFontSize = p[Keys.lyricsFontSize] ?: 22f,
            lyricsBoldActive = p[Keys.lyricsBoldActive] ?: true,
            lyricsLineSpacing = p[Keys.lyricsLineSpacing] ?: 1.35f,
            lyricsAlign = p[Keys.lyricsAlign] ?: 1,
            lyricsShowTranslation = p[Keys.lyricsShowTranslation] ?: true,
            lyricsTranslationGap = p[Keys.lyricsTranslationGap] ?: 6f,
            lyricsTranslationScale = p[Keys.lyricsTranslationScale] ?: 0.78f,
            lyricsInactiveAlpha = p[Keys.lyricsInactiveAlpha] ?: 0.38f,
            lyricsPersianFont = p[Keys.lyricsPersianFont] ?: true,
            floatingLyricsEnabled = p[Keys.floatingLyrics] ?: false,
            driveModeLyrics = p[Keys.driveModeLyrics] ?: true,
            driveModeKeepScreenOn = p[Keys.driveModeKeepScreenOn] ?: true,
            driveModeSwipeGestures = p[Keys.driveModeSwipe] ?: true,
            driveModeSeekSeconds = p[Keys.driveModeSeekSeconds] ?: 15,
            lyricsOffsets = p[Keys.lyricsOffsets]?.mapNotNull { entry ->
                val id = entry.substringBefore(':').toLongOrNull()
                val off = entry.substringAfter(':', "").toIntOrNull()
                if (id != null && off != null) id to off else null
            }?.toMap() ?: emptyMap()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }
    suspend fun setAppTheme(theme: AppTheme) = edit { it[Keys.appTheme] = theme.name }

    suspend fun setStartScreen(route: String) = edit { it[Keys.startScreen] = route }
    suspend fun setAutoBackup(enabled: Boolean) = edit { it[Keys.autoBackup] = enabled }
    suspend fun setLastAutoBackup(at: Long) = edit { it[Keys.lastAutoBackup] = at }

    suspend fun setPinnedSongs(ids: Set<Long>) = edit { prefs ->
        prefs[Keys.pinnedSongs] = ids.map { it.toString() }.toSet()
    }

    suspend fun togglePinnedSong(songId: Long) = edit { prefs ->
        val current = prefs[Keys.pinnedSongs] ?: emptySet()
        val id = songId.toString()
        prefs[Keys.pinnedSongs] = if (id in current) current - id else current + id
    }
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
    suspend fun setBalance(value: Float) = edit { it[Keys.balance] = value.coerceIn(-1f, 1f) }
    suspend fun setSkipSilence(enabled: Boolean) = edit { it[Keys.skipSilence] = enabled }
    suspend fun setPitchCorrection(enabled: Boolean) = edit { it[Keys.pitchCorrection] = enabled }
    suspend fun setTargetLoudness(db: Int) = edit { it[Keys.targetLoudness] = db.coerceIn(-24, -6) }
    suspend fun setReverbPreset(preset: Int) = edit { it[Keys.reverbPreset] = preset.coerceIn(0, 6) }
    suspend fun setMonoAudio(enabled: Boolean) = edit { it[Keys.monoAudio] = enabled }
    suspend fun setPerDeviceProfiles(enabled: Boolean) = edit { it[Keys.perDeviceProfiles] = enabled }
    suspend fun setAutoPlayOnConnect(enabled: Boolean) = edit { it[Keys.autoPlayOnConnect] = enabled }
    suspend fun setPauseOnDisconnect(enabled: Boolean) = edit { it[Keys.pauseOnDisconnect] = enabled }
    suspend fun setResumeOnReconnect(enabled: Boolean) = edit { it[Keys.resumeOnReconnect] = enabled }
    suspend fun setAutoDriveMode(enabled: Boolean) = edit { it[Keys.autoDriveMode] = enabled }
    suspend fun setAutoDriveModeNotify(enabled: Boolean) = edit { it[Keys.autoDriveModeNotify] = enabled }
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

    suspend fun setPinnedPlaylists(ids: Set<Long>) = edit { prefs ->
        prefs[Keys.pinnedPlaylists] = ids.map { it.toString() }.toSet()
    }
    suspend fun togglePinnedPlaylist(id: Long) = edit { prefs ->
        val current = prefs[Keys.pinnedPlaylists] ?: emptySet()
        val key = id.toString()
        prefs[Keys.pinnedPlaylists] = if (key in current) current - key else current + key
    }
    suspend fun setSeekIncrement(ms: Int) = edit { it[Keys.seekIncrement] = ms }
    suspend fun setLyricsFontSize(v: Float) = edit { it[Keys.lyricsFontSize] = v }
    suspend fun setLyricsBoldActive(v: Boolean) = edit { it[Keys.lyricsBoldActive] = v }
    suspend fun setLyricsLineSpacing(v: Float) = edit { it[Keys.lyricsLineSpacing] = v }
    suspend fun setLyricsAlign(v: Int) = edit { it[Keys.lyricsAlign] = v }
    suspend fun setLyricsShowTranslation(v: Boolean) = edit { it[Keys.lyricsShowTranslation] = v }
    suspend fun setLyricsTranslationGap(v: Float) = edit { it[Keys.lyricsTranslationGap] = v }
    suspend fun setLyricsTranslationScale(v: Float) = edit { it[Keys.lyricsTranslationScale] = v }
    suspend fun setLyricsInactiveAlpha(v: Float) = edit { it[Keys.lyricsInactiveAlpha] = v }
    suspend fun setLyricsPersianFont(v: Boolean) = edit { it[Keys.lyricsPersianFont] = v }
    suspend fun setFloatingLyricsEnabled(v: Boolean) = edit { it[Keys.floatingLyrics] = v }
    suspend fun setDriveModeLyrics(v: Boolean) = edit { it[Keys.driveModeLyrics] = v }
    suspend fun setDriveModeKeepScreenOn(v: Boolean) = edit { it[Keys.driveModeKeepScreenOn] = v }
    suspend fun setDriveModeSwipeGestures(v: Boolean) = edit { it[Keys.driveModeSwipe] = v }
    suspend fun setDriveModeSeekSeconds(v: Int) = edit { it[Keys.driveModeSeekSeconds] = v }

    /** Set (or clear with null) the manual lyrics sync offset for one song. */
    suspend fun setLyricsOffset(songId: Long, offsetMs: Int?) = edit { prefs ->
        val current = prefs[Keys.lyricsOffsets] ?: emptySet()
        val prefix = "$songId:"
        val without = current.filterNot { it.startsWith(prefix) }.toSet()
        prefs[Keys.lyricsOffsets] = if (offsetMs == null || offsetMs == 0) {
            without
        } else {
            without + "$songId:$offsetMs"
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }
}
