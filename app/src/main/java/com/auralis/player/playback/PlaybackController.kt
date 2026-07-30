package com.auralis.player.playback

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.SortOrder
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the single [ExoPlayer] instance for the app and bridges it to the
 * [MusicRepository]. UI observes [Player] directly via MediaController; this
 * class is the only writer.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository
) : MediaSession.Callback {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(30_000L)
        .build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private var queueJob: Job? = null

    /** Replace the current queue with [songs], starting playback from [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queueJob?.cancel()
        val items = songs.map { it.toMediaItem() }
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        player.prepare()
        player.playWhenReady = true
        registerPlayIfNeeded()
    }

    fun playNow(song: Song) {
        val all = musicRepository.songs.value
        val ordered = if (all.isEmpty()) listOf(song) else MusicRepository.sort(all, SortOrder.TITLE_ASC)
        val idx = ordered.indexOfFirst { it.id == song.id }.let { if (it < 0) 0 else it }
        playQueue(ordered, idx)
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() = player.seekToNext()
    fun previous() = player.seekToPrevious()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    private fun registerPlayIfNeeded() {
        scope.launch {
            val current = player.currentMediaItem ?: return@launch
            val songId = current.mediaId.toLongOrNull() ?: return@launch
            musicRepository.registerPlay(songId, player.duration.coerceAtLeast(0L))
        }
    }

    private fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(path.ifBlank { "content://media/external/audio/media/$id" })
        .setMimeType(mimeType.ifBlank { "audio/*" })
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(displayArtist)
                .setAlbumTitle(displayAlbum)
                .setAlbumArtist(albumArtist)
                .setGenre(genre)
                .setTrackNumber(trackNumber)
                .setDiscNumber(discNumber)
                .setReleaseYear(if (year > 0) year else null)
                .setArtworkUri(android.net.Uri.parse(artworkUri))
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()

    /** Initial bootstrap on app start: restore the last played song id (paused, not playing). */
    fun bootstrap() {
        scope.launch {
            runCatching {
                val lastId = settingsRepository.settings.first().lastQueueSongId
                if (lastId > 0L) {
                    val all = musicRepository.songs.value
                    val idx = all.indexOfFirst { it.id == lastId }
                    if (idx >= 0) {
                        player.setMediaItems(all.map { it.toMediaItem() }, idx, 0L)
                        player.prepare()
                    }
                }
            }
        }
    }

    /** MediaSession.Callback: allow browsers (notification, widgets, Auto) to add items. */
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val future = SettableFuture.create<List<MediaItem>>()
        scope.launch {
            val resolved = mediaItems.map { item ->
                if (item.localConfiguration != null) return@map item
                val songId = item.mediaId.toLongOrNull()
                val song = songId?.let { id -> musicRepository.songByIdSuspend(id) }
                song?.toMediaItem() ?: item
            }
            future.set(resolved)
        }
        return future
    }
}
