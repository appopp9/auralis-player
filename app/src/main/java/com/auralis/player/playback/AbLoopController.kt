package com.auralis.player.playback

import androidx.media3.common.Player
import com.auralis.player.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AbLoopState(
    val songId: Long = -1L,
    val startMs: Long? = null,
    val endMs: Long? = null,
    val enabled: Boolean = false
) {
    val ready: Boolean get() = startMs != null && endMs != null && endMs > startMs
}

/** Keeps playback inside the selected A-B region. */
@Singleton
class AbLoopController @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(AbLoopState())
    val state: StateFlow<AbLoopState> = _state.asStateFlow()

    private var player: Player? = null
    private var job: Job? = null

    fun attach(player: Player) {
        this.player = player
        job?.cancel()
        job = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val current = _state.value
                val target = this@AbLoopController.player
                val armed = target != null && current.enabled && current.ready && target.isPlaying
                if (!armed) {
                    // No loop to police: idle slowly instead of waking the main
                    // thread five times a second for the whole app lifetime.
                    delay(IDLE_INTERVAL_MS)
                    continue
                }
                delay(ACTIVE_INTERVAL_MS)
                val start = current.startMs ?: continue
                val end = current.endMs ?: continue
                val position = target!!.currentPosition
                if (position >= end || position < start - 500) {
                    target.seekTo(start)
                }
            }
        }
    }

    fun detach() {
        job?.cancel()
        job = null
        player = null
    }

    fun onTrackChanged(songId: Long) {
        if (_state.value.songId != songId) _state.value = AbLoopState(songId = songId)
    }

    fun markStart(songId: Long, positionMs: Long) {
        _state.value = _state.value.copy(songId = songId, startMs = positionMs)
    }

    fun markEnd(songId: Long, positionMs: Long) {
        val start = _state.value.startMs
        _state.value = _state.value.copy(
            songId = songId,
            endMs = positionMs,
            enabled = start != null && positionMs > start
        )
    }

    fun setRegion(songId: Long, startMs: Long, endMs: Long) {
        _state.value = AbLoopState(songId, startMs, endMs, enabled = endMs > startMs)
    }

    fun setEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(enabled = enabled && _state.value.ready)
    }

    fun clear() {
        _state.value = AbLoopState(songId = _state.value.songId)
    }

    private companion object {
        const val ACTIVE_INTERVAL_MS = 150L
        const val IDLE_INTERVAL_MS = 1_000L
    }
}
