package com.auralis.player.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.lyrics.LyricsFetchResult
import com.auralis.player.data.lyrics.LyricsRepository
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.AbLoop
import com.auralis.player.domain.model.EqualizerPreset
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.playback.AbLoopController
import com.auralis.player.playback.AbLoopState
import com.auralis.player.playback.AudioEffectsController
import com.auralis.player.playback.EqualizerState
import com.auralis.player.playback.PlayerConnection
import com.auralis.player.playback.SleepTimer
import com.auralis.player.playback.SleepTimerState
import com.auralis.player.playback.VisualizerController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-facing state of the lyrics download action, for precise feedback. */
sealed class LyricsDownloadState {
    data object Idle : LyricsDownloadState()
    data object Downloading : LyricsDownloadState()
    /** Lyrics are stored locally (justNow = downloaded in this action). */
    data class Saved(val justNow: Boolean) : LyricsDownloadState()
    data object Offline : LyricsDownloadState()
    data object NotFound : LyricsDownloadState()
    data class Failed(val message: String) : LyricsDownloadState()
}

/** Editable lyrics + translation text backing the in-app lyrics editor. */
data class LyricsDraft(
    val text: String = "",
    val translation: String = "",
    val loading: Boolean = false
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val player: PlayerConnection,
    private val musicRepository: MusicRepository,
    private val lyricsRepository: LyricsRepository,
    private val settingsRepository: SettingsRepository,
    private val audioEffects: AudioEffectsController,
    private val visualizerController: VisualizerController,
    private val sleepTimer: SleepTimer,
    private val abLoopController: AbLoopController
) : ViewModel() {

    val playerState = player.state
    val position = player.position
    val equalizerState: StateFlow<EqualizerState> = audioEffects.state
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimer.state
    val loopState: StateFlow<AbLoopState> = abLoopController.state
    val magnitudes: StateFlow<FloatArray> = visualizerController.magnitudes
    val visualizerActive: StateFlow<Boolean> = visualizerController.active

    /**
     * Optimistic favourite state: `songId to isFavorite`. It makes the like
     * button flip on the very same frame as the tap, and is dropped as soon as
     * the database emits the same value.
     */
    private val _favoriteOverride = MutableStateFlow<Pair<Long, Boolean>?>(null)

    val isCurrentFavorite: StateFlow<Boolean> =
        combine(player.state, _favoriteOverride) { state, override ->
            val song = state.currentSong
            when {
                song == null -> false
                override != null && override.first == song.id -> override.second
                else -> song.isFavorite
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    private val _downloadState = MutableStateFlow<LyricsDownloadState>(LyricsDownloadState.Idle)
    val downloadState: StateFlow<LyricsDownloadState> = _downloadState.asStateFlow()

    /** Lyrics appearance settings, live-applied to the lyrics surfaces. */
    val lyricsSettings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val customPresets: StateFlow<List<EqualizerPreset>> = musicRepository.customPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val builtInPresets: List<EqualizerPreset> = AudioEffectsController.asPresetList()

    val savedLoops: StateFlow<List<AbLoop>> = musicRepository.abLoops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            player.state
                .map { it.currentSong?.id ?: -1L }
                .distinctUntilChanged()
                .collect { songId -> loadLyrics(songId) }
        }
        viewModelScope.launch {
            player.state.collect { state ->
                val override = _favoriteOverride.value ?: return@collect
                val song = state.currentSong
                if (song == null || song.id != override.first || song.isFavorite == override.second) {
                    _favoriteOverride.value = null
                }
            }
        }
    }

    private fun loadLyrics(songId: Long) {
        if (songId <= 0) {
            _lyrics.value = null
            _downloadState.value = LyricsDownloadState.Idle
            return
        }
        viewModelScope.launch {
            _lyricsLoading.value = true
            _downloadState.value = LyricsDownloadState.Idle
            val settings = settingsRepository.settings.first()
            val offset = settings.lyricsOffsets[songId] ?: 0
            val target = musicRepository.songByIdSuspend(songId) ?: playerState.value.currentSong
            val result = if (target == null) null else {
                lyricsRepository.lyricsFor(target, settings.onlineLyrics && settings.autoSearchLyrics)
            }
            _lyrics.value = result?.let { applyOffset(it, offset) }
            _downloadState.value = if (result != null) {
                LyricsDownloadState.Saved(justNow = false)
            } else {
                LyricsDownloadState.Idle
            }
            _lyricsLoading.value = false
        }
    }

    /** Explicit user download. Gives precise offline / not-found / saved feedback. */
    fun downloadLyrics() {
        val song = playerState.value.currentSong ?: return
        if (_downloadState.value == LyricsDownloadState.Downloading) return
        viewModelScope.launch {
            _downloadState.value = LyricsDownloadState.Downloading
            when (val result = lyricsRepository.downloadOnline(song)) {
                is LyricsFetchResult.Success -> {
                    _lyrics.value = result.lyrics
                    _downloadState.value = LyricsDownloadState.Saved(justNow = !result.alreadySaved)
                }
                LyricsFetchResult.NotFound -> _downloadState.value = LyricsDownloadState.NotFound
                LyricsFetchResult.Offline -> _downloadState.value = LyricsDownloadState.Offline
                is LyricsFetchResult.Error -> _downloadState.value = LyricsDownloadState.Failed(result.message)
            }
        }
    }

    /** Import lyrics from a user-picked .lrc/.txt file for the current song. */
    fun importLyrics(uri: Uri) {
        val songId = playerState.value.currentSong?.id ?: return
        viewModelScope.launch {
            _downloadState.value = LyricsDownloadState.Downloading
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                _downloadState.value = LyricsDownloadState.Failed("Couldn't read that file")
                return@launch
            }
            runCatching { lyricsRepository.save(songId, text) }
                .onSuccess {
                    _downloadState.value = LyricsDownloadState.Saved(justNow = true)
                    loadLyrics(songId)
                }
                .onFailure { _downloadState.value = LyricsDownloadState.Failed("Couldn't save lyrics") }
        }
    }

    /** Import a translation file (.lrc/.txt) for the current song. */
    fun importTranslation(uri: Uri) {
        val songId = playerState.value.currentSong?.id ?: return
        viewModelScope.launch {
            _downloadState.value = LyricsDownloadState.Downloading
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                _downloadState.value = LyricsDownloadState.Failed("Couldn't read that file")
                return@launch
            }
            runCatching { lyricsRepository.saveTranslation(songId, text) }
                .onSuccess {
                    _editorDraft.value = _editorDraft.value.copy(translation = text)
                    _downloadState.value = LyricsDownloadState.Saved(justNow = true)
                    loadLyrics(songId)
                }
                .onFailure { _downloadState.value = LyricsDownloadState.Failed("Couldn't save translation") }
        }
    }

    // ---- in-app lyrics editor ----------------------------------------------

    private val _editorDraft = MutableStateFlow(LyricsDraft())
    /** Text currently loaded into the lyrics editor sheet. */
    val editorDraft: StateFlow<LyricsDraft> = _editorDraft.asStateFlow()

    /** Loads the stored lyrics + translation into the editor. */
    fun openLyricsEditor() {
        val song = playerState.value.currentSong ?: return
        _editorDraft.value = LyricsDraft(loading = true)
        viewModelScope.launch {
            val raw = lyricsRepository.rawFor(song)
            _editorDraft.value = LyricsDraft(
                text = raw.text,
                translation = raw.translation,
                loading = false
            )
        }
    }

    /** Writes the editor contents back to the library and refreshes the pane. */
    fun saveLyricsEdit(text: String, translation: String) {
        val songId = playerState.value.currentSong?.id ?: return
        viewModelScope.launch {
            runCatching {
                lyricsRepository.save(songId, text)
                lyricsRepository.saveTranslation(songId, translation)
            }.onSuccess {
                _editorDraft.value = LyricsDraft(text = text, translation = translation)
                _downloadState.value = if (text.isBlank()) {
                    LyricsDownloadState.Idle
                } else {
                    LyricsDownloadState.Saved(justNow = true)
                }
                loadLyrics(songId)
            }.onFailure {
                _downloadState.value = LyricsDownloadState.Failed("Couldn't save lyrics")
            }
        }
    }

    /**
     * Returns an LRC timestamp for the current playback position, so the
     * editor can stamp the line the user is looking at while the song plays.
     */
    fun currentTimestampTag(): String {
        val ms = (position.value.positionMs - (lyricsOffsetMs.value)).coerceAtLeast(0L)
        val minutes = ms / 60_000
        val seconds = (ms % 60_000) / 1000
        val hundredths = (ms % 1000) / 10
        return String.format("[%02d:%02d.%02d]", minutes, seconds, hundredths)
    }

    /** Marks the transient download feedback as consumed (after the UI shows it). */
    fun consumeDownloadFeedback() {
        val current = _downloadState.value
        if (current is LyricsDownloadState.Saved && current.justNow) {
            _downloadState.value = LyricsDownloadState.Saved(justNow = false)
        } else if (current is LyricsDownloadState.Offline ||
            current is LyricsDownloadState.NotFound ||
            current is LyricsDownloadState.Failed
        ) {
            _downloadState.value = if (_lyrics.value != null) {
                LyricsDownloadState.Saved(justNow = false)
            } else {
                LyricsDownloadState.Idle
            }
        }
    }

    // ---- lyrics appearance (live preview) ----------------------------------

    fun updateLyricsSettings(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    fun activeLyricIndex(positionMs: Long): Int {
        val lines = _lyrics.value?.lines ?: return -1
        if (lines.isEmpty()) return -1
        var index = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) index = i else break
        }
        return index
    }

    /** Shifts every synced line by the manual offset (ms). Positive = show later. */
    private fun applyOffset(lyrics: Lyrics, offsetMs: Int): Lyrics {
        if (offsetMs == 0) return lyrics
        return lyrics.copy(
            lines = lyrics.lines.map { it.copy(timeMs = (it.timeMs + offsetMs).coerceAtLeast(0L)) }
        )
    }

    /** Current song's manual lyrics sync offset in milliseconds. */
    val lyricsOffsetMs: StateFlow<Int> =
        combine(player.state, settingsRepository.settings) { state, settings ->
            settings.lyricsOffsets[state.currentSong?.id ?: -1L] ?: 0
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Shift the current song's lyrics timing by [deltaMs] and re-apply immediately. */
    fun adjustLyricsOffset(deltaMs: Int) {
        val songId = playerState.value.currentSong?.id ?: return
        viewModelScope.launch {
            val current = settingsRepository.settings.first().lyricsOffsets[songId] ?: 0
            settingsRepository.setLyricsOffset(songId, current + deltaMs)
            loadLyrics(songId)
        }
    }

    // ---- sleep timer -------------------------------------------------------

    fun startSleepTimer(minutes: Int) = sleepTimer.startMinutes(minutes)
    fun sleepAfterTrack() = sleepTimer.stopAtTrackEnd()
    fun cancelSleepTimer() = sleepTimer.cancel()

    // ---- equalizer ---------------------------------------------------------

    fun setEqEnabled(enabled: Boolean) {
        audioEffects.setEnabled(enabled)
        viewModelScope.launch { settingsRepository.setEqEnabled(enabled) }
    }

    fun setBand(index: Int, levelMb: Int) {
        audioEffects.setBandLevel(index, levelMb)
        persistLevels()
    }

    fun applyPreset(preset: EqualizerPreset) {
        val state = audioEffects.state.value
        val levels = if (preset.bandLevels.size == state.bandCount) {
            preset.bandLevels
        } else {
            AudioEffectsController.presetToMillibels(
                preset.bandLevels, state.bandCount, state.minLevelMb, state.maxLevelMb
            )
        }
        audioEffects.applyLevels(levels, preset.name)
        viewModelScope.launch {
            settingsRepository.setEqPresetName(preset.name)
            settingsRepository.setEqLevels(levels.joinToString(","))
            settingsRepository.setEqEnabled(true)
        }
    }

    fun saveCurrentAsPreset(name: String) {
        viewModelScope.launch {
            musicRepository.saveCustomPreset(name, audioEffects.currentLevels())
        }
    }

    fun deletePreset(id: Long) {
        viewModelScope.launch { musicRepository.deleteCustomPreset(id) }
    }

    fun setBassBoost(value: Int) {
        audioEffects.setBassBoost(value)
        viewModelScope.launch { settingsRepository.setBassBoost(value) }
    }

    fun setTreble(value: Int) {
        audioEffects.setTrebleBoost(value)
        viewModelScope.launch { settingsRepository.setTrebleBoost(value) }
    }

    fun setVirtualizer(value: Int) {
        audioEffects.setVirtualizer(value)
        viewModelScope.launch { settingsRepository.setVirtualizer(value) }
    }

    fun setLoudness(value: Int) {
        audioEffects.setLoudness(value)
        viewModelScope.launch { settingsRepository.setLoudness(value) }
    }

    private fun persistLevels() {
        viewModelScope.launch {
            settingsRepository.setEqLevels(audioEffects.currentLevels().joinToString(","))
            settingsRepository.setEqPresetName("Custom")
        }
    }

    // ---- A-B loop ----------------------------------------------------------

    fun markLoopStart() {
        val song = playerState.value.currentSong ?: return
        abLoopController.markStart(song.id, player.currentPositionMs())
    }

    fun markLoopEnd() {
        val song = playerState.value.currentSong ?: return
        abLoopController.markEnd(song.id, player.currentPositionMs())
    }

    fun toggleLoop(enabled: Boolean) = abLoopController.setEnabled(enabled)

    fun clearLoop() = abLoopController.clear()

    fun saveLoop(label: String) {
        val loop = abLoopController.state.value
        val start = loop.startMs ?: return
        val end = loop.endMs ?: return
        viewModelScope.launch { musicRepository.saveAbLoop(loop.songId, label, start, end) }
    }

    fun applySavedLoop(loop: AbLoop) {
        abLoopController.setRegion(loop.songId, loop.startMs, loop.endMs)
        player.seekTo(loop.startMs)
    }

    fun deleteSavedLoop(id: Long) {
        viewModelScope.launch { musicRepository.deleteAbLoop(id) }
    }

    // ---- visualizer --------------------------------------------------------

    fun visualizerNeedsPermission(): Boolean = !visualizerController.hasPermission()

    fun onCapturePermission(granted: Boolean) {
        if (!granted) return
        // The service starts capture on the next playback state change.
        player.playPause(playerState.value.isPlaying)
    }

    fun toggleFavoriteCurrent() {
        val song = playerState.value.currentSong ?: return
        val current = _favoriteOverride.value
            ?.takeIf { it.first == song.id }
            ?.second
            ?: song.isFavorite
        val next = !current
        _favoriteOverride.value = song.id to next
        viewModelScope.launch { musicRepository.setFavorite(song.id, next) }
    }
}
