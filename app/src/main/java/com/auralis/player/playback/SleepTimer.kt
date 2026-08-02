package com.auralis.player.playback

import com.auralis.player.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val stopAtTrackEnd: Boolean = false
)

@Singleton
class SleepTimer @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null
    private var onFinish: (() -> Unit)? = null

    fun setCallback(callback: () -> Unit) {
        onFinish = callback
    }

    fun startMinutes(minutes: Int) {
        start(minutes * 60_000L)
    }

    fun start(durationMs: Long) {
        cancel()
        if (durationMs <= 0) return
        _state.value = SleepTimerState(active = true, remainingMs = durationMs)
        job = scope.launch {
            var remaining = durationMs
            while (isActive && remaining > 0) {
                delay(1000)
                remaining -= 1000
                _state.value = _state.value.copy(remainingMs = remaining.coerceAtLeast(0))
            }
            if (isActive) {
                _state.value = SleepTimerState()
                onFinish?.invoke()
            }
        }
    }

    fun stopAtTrackEnd() {
        cancel()
        _state.value = SleepTimerState(active = true, stopAtTrackEnd = true)
    }

    fun consumeTrackEnd(): Boolean {
        if (!_state.value.stopAtTrackEnd) return false
        _state.value = SleepTimerState()
        return true
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState()
    }
}
