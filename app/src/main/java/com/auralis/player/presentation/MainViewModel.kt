package com.auralis.player.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.auralis.player.core.AppShortcuts
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.data.backup.BackupManager
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.data.scanner.MediaScanner
import com.auralis.player.data.ringtone.RingtoneCutter
import com.auralis.player.data.tags.TagEditor
import com.auralis.player.data.tags.TagUpdate
import com.auralis.player.data.tags.TagWriteResult
import com.auralis.player.domain.model.ScanProgress
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlayerConnection
import com.auralis.player.playback.PlayerUiState
import com.auralis.player.ui.theme.ArtworkColorExtractor
import com.auralis.player.ui.theme.ArtworkColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiMessage(val text: String, val actionLabel: String? = null)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaScanner: MediaScanner,
    private val tagEditor: TagEditor,
    private val ringtoneCutter: RingtoneCutter,
    private val artworkColorExtractor: ArtworkColorExtractor,
    private val backupManager: BackupManager,
    val player: PlayerConnection
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val playerState: StateFlow<PlayerUiState> = player.state

    val scanProgress: StateFlow<ScanProgress> = mediaScanner.progress

    private val _artworkColors = MutableStateFlow<ArtworkColorScheme?>(null)
    val artworkColors: StateFlow<ArtworkColorScheme?> = _artworkColors.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    private val _pendingIntentSender = MutableStateFlow<android.content.IntentSender?>(null)
    val pendingIntentSender: StateFlow<android.content.IntentSender?> = _pendingIntentSender.asStateFlow()

    private var permissionGranted = false

    private val _uiReady = MutableStateFlow(false)
    /**
     * True only after the first *persisted* settings snapshot has loaded from
     * DataStore. Gates the splash screen and first paint so the default theme
     * and Home page never flash before the user's choices are applied.
     */
    val uiReady: StateFlow<Boolean> = _uiReady.asStateFlow()

    init {
        player.connect()
        viewModelScope.launch {
            // Suspend until the first real value arrives (StateFlow starts with
            // the in-memory default, so we must await the on-disk read).
            settings.first()
            _uiReady.value = true
        }
        viewModelScope.launch {
            runCatching { backupManager.autoBackupIfDue() }
        }
        // D23 — keep the launcher shortcuts in step with listening history.
        viewModelScope.launch {
            musicRepository.recentlyPlayed.collect { recent ->
                AppShortcuts.publish(appContext, recent)
            }
        }
    }

    /**
     * D23 — handles a launcher shortcut tap. Waits for the library to finish
     * loading, so a cold start from a shortcut still plays something.
     */
    fun handleShortcut(action: String?, songId: Long) {
        if (action.isNullOrBlank()) return
        viewModelScope.launch {
            val library = musicRepository.songs.first { it.isNotEmpty() }
            when (action) {
                AppShortcuts.ACTION_SHUFFLE_ALL -> player.playShuffled(library)
                AppShortcuts.ACTION_RESUME -> player.playPause(true)
                AppShortcuts.ACTION_PLAY_SONG -> {
                    val song = library.firstOrNull { it.id == songId }
                    if (song != null) {
                        player.play(listOf(song))
                    } else {
                        notify("That track is no longer in your library")
                    }
                }
            }
        }
    }

    // ---- backup ------------------------------------------------------------

    fun backupFileName(): String = backupManager.suggestedFileName()

    fun exportBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching { backupManager.exportTo(uri) }
                .onSuccess { notify("Backup saved — encrypted, only Auralis can open it") }
                .onFailure { notify("Backup failed: ${it.message ?: "unknown error"}") }
        }
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching { backupManager.importFrom(uri) }
                .onSuccess { summary ->
                    notify(
                        "Restored ${summary.playlists} playlists, " +
                            "${summary.favorites} favorites, ${summary.pinned} pinned"
                    )
                }
                .onFailure { notify("Restore failed: ${it.message ?: "not a valid Auralis backup"}") }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        permissionGranted = granted
        if (granted) {
            viewModelScope.launch {
                mediaScanner.scan()
                settingsRepository.setFirstScanDone(true)
            }
        } else {
            notify("Auralis needs audio access to build your library")
        }
    }

    fun rescan() {
        if (!permissionGranted) {
            notify("Grant audio access first")
            return
        }
        viewModelScope.launch {
            val result = mediaScanner.scan()
            notify("Library updated — ${result.total} tracks")
        }
    }

    fun refreshArtworkColors(songId: Long, isDark: Boolean) {
        viewModelScope.launch {
            _artworkColors.value = artworkColorExtractor.schemeFor(songId, isDark)
        }
    }

    fun dynamicAccent(): Color? =
        if (settings.value.dynamicArtworkColor) _artworkColors.value?.primary else null

    // ---- playback shortcuts ------------------------------------------------

    fun playAll(songs: List<Song>, index: Int = 0) = player.play(songs, index)

    fun shuffleAll(songs: List<Song>) = player.playShuffled(songs)

    fun playNext(song: Song) {
        player.playNext(listOf(song))
        notify("Playing next: ${song.title}")
    }

    fun addToQueue(songs: List<Song>) {
        player.addToQueue(songs)
        notify(if (songs.size == 1) "Added to queue" else "${songs.size} tracks added to queue")
    }

    /** First *persisted* start-screen value (suspends past the default). */
    suspend fun settingsRepositoryFirstStartScreen(): String =
        settingsRepository.settings.first().startScreen.let { route ->
            when (route) {
                "library", "playlists", "favorites" -> route
                else -> "home"
            }
        }

    fun togglePinned(song: Song) {
        viewModelScope.launch {
            val pinned = settings.value.pinnedSongs.contains(song.id)
            settingsRepository.togglePinnedSong(song.id)
            notify(if (pinned) "Unpinned \"${song.title}\"" else "Pinned \"${song.title}\" to top")
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(song.id)
            notify(if (song.isFavorite) "Removed from favorites" else "Added to favorites")
        }
    }

    fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.addSongs(playlistId, songIds)
            notify("Added to ${playlistRepository.playlistName(playlistId)}")
        }
    }

    fun createPlaylistWith(name: String, songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.create(name, songIds)
            notify("Playlist \"$name\" created")
        }
    }

    // ---- song file actions -------------------------------------------------

    fun saveTags(song: Song, update: TagUpdate, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = tagEditor.apply(song, update)) {
                is TagWriteResult.Success -> {
                    notify("Tags saved")
                    onDone(true)
                }
                is TagWriteResult.NeedsPermission -> {
                    _pendingIntentSender.value = result.intentSender
                    onDone(false)
                }
                is TagWriteResult.LocalOnly -> {
                    notify(result.reason)
                    onDone(true)
                }
            }
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            when (val result = tagEditor.delete(song)) {
                is TagWriteResult.Success -> notify("\"${song.title}\" deleted")
                is TagWriteResult.NeedsPermission -> _pendingIntentSender.value = result.intentSender
                is TagWriteResult.LocalOnly -> notify(result.reason)
            }
        }
    }

    fun consumeIntentSender() {
        _pendingIntentSender.value = null
    }

    fun shareIntent(song: Song) = tagEditor.shareIntent(song)

    fun canSetRingtone(): Boolean = tagEditor.canWriteSystemSettings()

    fun writeSettingsIntent() = tagEditor.writeSettingsIntent()

    fun setAsRingtone(song: Song) {
        val done = tagEditor.setAsRingtone(song)
        notify(if (done) "Set as ringtone" else "Allow ‘Modify system settings’ to set a ringtone")
    }

    /** Trim a segment out of [song] and install it as the device ringtone. */
    fun trimRingtone(song: Song, startMs: Long, endMs: Long) {
        viewModelScope.launch {
            when (val result = ringtoneCutter.trimAndSet(song, startMs, endMs)) {
                RingtoneCutter.TrimResult.Success -> notify("Ringtone set")
                RingtoneCutter.TrimResult.NoPermission ->
                    notify("Allow ‘Modify system settings’ to set a ringtone")
                RingtoneCutter.TrimResult.UnsupportedFormat ->
                    notify("This audio format can't be trimmed — use ‘Set as ringtone’ for the full song")
                is RingtoneCutter.TrimResult.Failed ->
                    notify("Couldn't set ringtone: ${result.reason}")
            }
        }
    }

    fun notify(text: String, actionLabel: String? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel))
    }

    // ---- settings ----------------------------------------------------------

    fun updateSettings(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    /**
     * Persists only the fields that actually changed between [old] and [new].
     * Lets the settings UI stay a pure function of [AppSettings].
     */
    fun applySettings(old: AppSettings, new: AppSettings) {
        if (old == new) return
        viewModelScope.launch {
            with(settingsRepository) {
                if (old.themeMode != new.themeMode) setThemeMode(new.themeMode)
                if (old.appTheme != new.appTheme) setAppTheme(new.appTheme)
                if (old.startScreen != new.startScreen) setStartScreen(new.startScreen)
                if (old.autoBackupEnabled != new.autoBackupEnabled) setAutoBackup(new.autoBackupEnabled)
                if (old.accent != new.accent) setAccent(new.accent)
                if (old.customAccent != new.customAccent) setCustomAccent(new.customAccent)
                if (old.dynamicArtworkColor != new.dynamicArtworkColor) setDynamicArtwork(new.dynamicArtworkColor)
                if (old.animationsEnabled != new.animationsEnabled) setAnimations(new.animationsEnabled)
                if (old.hapticsEnabled != new.hapticsEnabled) setHaptics(new.hapticsEnabled)
                if (old.gridStyle != new.gridStyle) setGridStyle(new.gridStyle)
                if (old.visualizerMode != new.visualizerMode) setVisualizerMode(new.visualizerMode)
                if (old.visualizerSensitivity != new.visualizerSensitivity) setVisualizerSensitivity(new.visualizerSensitivity)
                if (old.visualizerSmoothing != new.visualizerSmoothing) setVisualizerSmoothing(new.visualizerSmoothing)
                if (old.visualizerBars != new.visualizerBars) setVisualizerBars(new.visualizerBars)
                if (old.visualizerSpeed != new.visualizerSpeed) setVisualizerSpeed(new.visualizerSpeed)
                if (old.visualizerIntensity != new.visualizerIntensity) setVisualizerIntensity(new.visualizerIntensity)
                if (old.gaplessEnabled != new.gaplessEnabled) setGapless(new.gaplessEnabled)
                if (old.crossfadeSeconds != new.crossfadeSeconds) setCrossfade(new.crossfadeSeconds)
                if (old.replayGainEnabled != new.replayGainEnabled) setReplayGain(new.replayGainEnabled)
                if (old.shuffle != new.shuffle) {
                    setShuffle(new.shuffle)
                    player.setShuffle(new.shuffle)
                }
                if (old.repeatMode != new.repeatMode) setRepeat(new.repeatMode)
                if (old.resumeOnStart != new.resumeOnStart) setResume(new.resumeOnStart)
                if (old.respectAudioFocus != new.respectAudioFocus) setAudioFocus(new.respectAudioFocus)
                if (old.playbackSpeed != new.playbackSpeed) {
                    setSpeed(new.playbackSpeed)
                    player.setSpeed(new.playbackSpeed)
                }
                if (old.eqEnabled != new.eqEnabled) setEqEnabled(new.eqEnabled)
                if (old.eqBandCount != new.eqBandCount) setEqBandCount(new.eqBandCount)
                if (old.bassBoost != new.bassBoost) setBassBoost(new.bassBoost)
                if (old.trebleBoost != new.trebleBoost) setTrebleBoost(new.trebleBoost)
                if (old.volumeBoost != new.volumeBoost) setVolumeBoost(new.volumeBoost)
                if (old.balance != new.balance) setBalance(new.balance)
                if (old.defaultTab != new.defaultTab) setDefaultTab(new.defaultTab)
                if (old.songSort != new.songSort) setSongSort(new.songSort)
                if (old.minTrackSeconds != new.minTrackSeconds) setMinTrackSeconds(new.minTrackSeconds)
                if (old.embeddedLyrics != new.embeddedLyrics) setEmbeddedLyrics(new.embeddedLyrics)
                if (old.onlineLyrics != new.onlineLyrics) setOnlineLyrics(new.onlineLyrics)
                if (old.autoSearchLyrics != new.autoSearchLyrics) setAutoSearchLyrics(new.autoSearchLyrics)
                if (old.showNotification != new.showNotification) setShowNotification(new.showNotification)
                if (old.notificationArtwork != new.notificationArtwork) setNotificationArtwork(new.notificationArtwork)
                if (old.notificationControls != new.notificationControls) setNotificationControls(new.notificationControls)
                if (old.seekIncrementMs != new.seekIncrementMs) setSeekIncrement(new.seekIncrementMs)
                if (old.lyricsFontSize != new.lyricsFontSize) setLyricsFontSize(new.lyricsFontSize)
                if (old.lyricsBoldActive != new.lyricsBoldActive) setLyricsBoldActive(new.lyricsBoldActive)
                if (old.lyricsLineSpacing != new.lyricsLineSpacing) setLyricsLineSpacing(new.lyricsLineSpacing)
                if (old.lyricsAlign != new.lyricsAlign) setLyricsAlign(new.lyricsAlign)
                if (old.lyricsShowTranslation != new.lyricsShowTranslation) setLyricsShowTranslation(new.lyricsShowTranslation)
                if (old.lyricsTranslationGap != new.lyricsTranslationGap) setLyricsTranslationGap(new.lyricsTranslationGap)
                if (old.lyricsTranslationScale != new.lyricsTranslationScale) setLyricsTranslationScale(new.lyricsTranslationScale)
                if (old.lyricsInactiveAlpha != new.lyricsInactiveAlpha) setLyricsInactiveAlpha(new.lyricsInactiveAlpha)
                if (old.lyricsPersianFont != new.lyricsPersianFont) setLyricsPersianFont(new.lyricsPersianFont)
                if (old.floatingLyricsEnabled != new.floatingLyricsEnabled) setFloatingLyricsEnabled(new.floatingLyricsEnabled)

                // Audio processing (A3–A8). These used to be missing from the
                // diff, so the switches in Settings reverted on the next redraw.
                if (old.virtualizer != new.virtualizer) setVirtualizer(new.virtualizer)
                if (old.loudnessGain != new.loudnessGain) setLoudness(new.loudnessGain)
                if (old.skipSilence != new.skipSilence) setSkipSilence(new.skipSilence)
                if (old.pitchCorrection != new.pitchCorrection) setPitchCorrection(new.pitchCorrection)
                if (old.targetLoudnessDb != new.targetLoudnessDb) setTargetLoudness(new.targetLoudnessDb)
                if (old.reverbPreset != new.reverbPreset) setReverbPreset(new.reverbPreset)
                if (old.monoAudio != new.monoAudio) setMonoAudio(new.monoAudio)
                if (old.perDeviceProfiles != new.perDeviceProfiles) setPerDeviceProfiles(new.perDeviceProfiles)

                // Headset / Bluetooth behaviour (D20) and Drive Mode (D21).
                if (old.autoPlayOnConnect != new.autoPlayOnConnect) setAutoPlayOnConnect(new.autoPlayOnConnect)
                if (old.pauseOnDisconnect != new.pauseOnDisconnect) setPauseOnDisconnect(new.pauseOnDisconnect)
                if (old.resumeOnReconnect != new.resumeOnReconnect) setResumeOnReconnect(new.resumeOnReconnect)
                if (old.autoDriveMode != new.autoDriveMode) setAutoDriveMode(new.autoDriveMode)
                if (old.autoDriveModeNotify != new.autoDriveModeNotify) setAutoDriveModeNotify(new.autoDriveModeNotify)
                if (old.driveModeLyrics != new.driveModeLyrics) setDriveModeLyrics(new.driveModeLyrics)
                if (old.driveModeKeepScreenOn != new.driveModeKeepScreenOn) setDriveModeKeepScreenOn(new.driveModeKeepScreenOn)
                if (old.driveModeSwipeGestures != new.driveModeSwipeGestures) setDriveModeSwipeGestures(new.driveModeSwipeGestures)
                if (old.driveModeSeekSeconds != new.driveModeSeekSeconds) setDriveModeSeekSeconds(new.driveModeSeekSeconds)
            }
        }
    }
}
