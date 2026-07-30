package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.data.scanner.MediaScanner
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiMessage(val text: String, val actionLabel: String? = null)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val mediaScanner: MediaScanner,
    private val tagEditor: TagEditor,
    private val artworkColorExtractor: ArtworkColorExtractor,
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

    init {
        player.connect()
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
            }
        }
    }
}
