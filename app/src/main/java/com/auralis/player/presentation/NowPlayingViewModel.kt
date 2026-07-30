package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.lyrics.LyricsRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
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
            return
        }
        viewModelScope.launch {
            _lyricsLoading.value = true
            val target = musicRepository.songByIdSuspend(songId) ?: playerState.value.currentSong
            _lyrics.value = if (target == null) null else {
                val settings = kotlinx.coroutines.flow.first(settingsRepository.settings)
                lyricsRepository.lyricsFor(target, settings.onlineLyrics && settings.autoSearchLyrics)
            }
            _lyricsLoading.value = false
        }
    }

    fun searchLyricsOnline() {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            _lyricsLoading.value = true
            _lyrics.value = lyricsRepository.lyricsFor(song, allowOnline = true)
            _lyricsLoading.value = false
        }
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
        val state = playerState.value
        val song = state.currentSong ?: return
        abLoopController.markStart(song.id, state.positionMs)
    }

    fun markLoopEnd() {
        val state = playerState.value
        val song = state.currentSong ?: return
        abLoopController.markEnd(song.id, state.positionMs)
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
