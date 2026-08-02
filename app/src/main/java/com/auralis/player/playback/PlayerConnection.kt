package com.auralis.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.di.ApplicationScope
import com.auralis.player.domain.model.Song
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structural playback state. Deliberately excludes the play position: position
 * ticks 4×/second and anything collecting it recomposes at that rate. Screens
 * that need the position collect [PlayerConnection.position] separately, so
 * the rest of the app stays perfectly still while music plays.
 */
data class PlayerUiState(
    val connected: Boolean = false,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = 0,
    val speed: Float = 1f
) {
    val hasQueue: Boolean get() = queue.isNotEmpty()
}

/** High-frequency position stream, isolated from [PlayerUiState]. */
data class PlaybackPosition(
    val positionMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val durationMs: Long = 0L
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val bufferedProgress: Float
        get() = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

/**
 * Single point of contact between the UI layer and the playback service.
 * ViewModels talk to this class; composables never touch the player directly.
 */
@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionInfo: PlaybackSessionInfo,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(PlaybackPosition())
    val position: StateFlow<PlaybackPosition> = _position.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                controller = future.get().also { it.addListener(listener) }
                _state.value = _state.value.copy(connected = true)
                syncState()
            }
        }, MoreExecutors.directExecutor())

        // Keep the exposed song objects in sync with the library database, so
        // things like the favourite flag update the player UI immediately
        // instead of waiting for the next playback event.
        scope.launch(Dispatchers.Main) {
            musicRepository.songs.collect { syncState() }
        }

        scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(250)
                val player = controller ?: continue
                if (player.isPlaying || _position.value.positionMs != player.currentPosition) {
                    _position.value = PlaybackPosition(
                        positionMs = player.currentPosition.coerceAtLeast(0),
                        bufferedMs = player.bufferedPosition.coerceAtLeast(0),
                        durationMs = player.duration.takeIf { it > 0 } ?: _position.value.durationMs
                    )
                }
            }
        }
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        _state.value = PlayerUiState()
        _position.value = PlaybackPosition()
    }

    private fun syncState() {
        val player = controller ?: return
        val songsById = musicRepository.songs.value.associateBy { it.id }
        val queue = (0 until player.mediaItemCount).mapNotNull { index ->
            songsById[MediaItems.songId(player.getMediaItemAt(index))]
        }
        val currentId = MediaItems.songId(player.currentMediaItem)
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        val next = _state.value.copy(
            connected = true,
            currentSong = songsById[currentId] ?: _state.value.currentSong?.takeIf { it.id == currentId },
            isPlaying = player.isPlaying,
            durationMs = duration,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = queue,
            queueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
            speed = player.playbackParameters.speed
        )
        // StateFlow versus data class equality keeps no-op syncs from emitting.
        _state.value = next
        _position.value = _position.value.copy(
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = duration
        )
    }

    // ---- transport ---------------------------------------------------------

    fun play(songs: List<Song>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { MediaItems.from(it) }
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), startPositionMs)
        player.prepare()
        player.play()
        syncState()
    }

    fun playShuffled(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        play(shuffled, 0)
        setShuffle(true)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause()
        else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun playPause(play: Boolean) {
        val player = controller ?: return
        if (play) {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        } else player.pause()
    }

    fun next() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
        else player.seekTo(0, 0)
    }

    fun previous() {
        val player = controller ?: return
        if (player.currentPosition > 4000) player.seekTo(0)
        else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
        else player.seekTo(0)
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0))
        _position.value = _position.value.copy(positionMs = positionMs.coerceAtLeast(0))
    }

    fun seekToFraction(fraction: Float) {
        val duration = _position.value.durationMs.takeIf { it > 0 } ?: _state.value.durationMs
        if (duration > 0) seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    fun currentPositionMs(): Long = _position.value.positionMs

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
        scope.launch { settingsRepository.setShuffle(enabled) }
    }

    fun toggleShuffle() = setShuffle(!(controller?.shuffleModeEnabled ?: false))

    fun cycleRepeat() {
        val player = controller ?: return
        val next = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = next
        scope.launch {
            settingsRepository.setRepeat(
                when (next) {
                    Player.REPEAT_MODE_ALL -> 1
                    Player.REPEAT_MODE_ONE -> 2
                    else -> 0
                }
            )
        }
    }

    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4f)
        controller?.setPlaybackSpeed(clamped)
        scope.launch { settingsRepository.setSpeed(clamped) }
    }

    /** Relative seek used by long-press on next/previous. */
    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        val duration = player.duration
        val upper = if (duration > 0) duration else Long.MAX_VALUE
        val target = (player.currentPosition + deltaMs).coerceIn(0L, upper)
        player.seekTo(target)
        _position.value = _position.value.copy(positionMs = target)
    }

    // ---- queue -------------------------------------------------------------

    fun playNext(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        val items = songs.map { MediaItems.from(it) }
        if (player.mediaItemCount == 0) {
            play(songs)
        } else {
            player.addMediaItems((player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount), items)
            syncState()
        }
    }

    fun addToQueue(songs: List<Song>) {
        val player = controller ?: return
        if (songs.isEmpty()) return
        if (player.mediaItemCount == 0) play(songs)
        else {
            player.addMediaItems(songs.map { MediaItems.from(it) })
            syncState()
        }
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.removeMediaItem(index)
            syncState()
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val player = controller ?: return
        if (from in 0 until player.mediaItemCount && to in 0 until player.mediaItemCount) {
            player.moveMediaItem(from, to)
            syncState()
        }
    }

    fun skipToQueueIndex(index: Int) {
        val player = controller ?: return
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0)
            player.play()
        }
    }

    fun clearQueue() {
        controller?.clearMediaItems()
        syncState()
    }

    fun currentMediaItem(): MediaItem? = controller?.currentMediaItem

    /**
     * The live ExoPlayer audio session id, published by the playback service.
     * `MediaController` does not expose it, so it is read from the shared
     * [PlaybackSessionInfo] instead of being hard-coded.
     */
    fun audioSessionId(): Int = sessionInfo.audioSessionId.value

    /** Same value as a stream, for screens that attach effects reactively. */
    val audioSessionIdFlow: StateFlow<Int> = sessionInfo.audioSessionId
}
