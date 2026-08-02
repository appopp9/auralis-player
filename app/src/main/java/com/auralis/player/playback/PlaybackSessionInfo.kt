package com.auralis.player.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared handle on the live ExoPlayer audio session id.
 *
 * `MediaController` deliberately does not expose the audio session id, so the
 * service publishes it here the moment the player is created (and again on
 * every playback start, because the id can change when the audio sink is
 * re-created). Anything on the UI side — equaliser, visualiser, a system
 * effects panel — reads it from here instead of guessing 0.
 */
@Singleton
class PlaybackSessionInfo @Inject constructor() {

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    fun update(sessionId: Int) {
        _audioSessionId.value = sessionId
    }

    fun clear() {
        _audioSessionId.value = 0
    }
}
