package com.auralis.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralis.player.MainActivity
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.data.db.QueueDao
import com.auralis.player.data.db.QueueStateEntity
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

/**
 * Foreground media playback service. Owns the ExoPlayer instance, the media
 * session (lock screen / Bluetooth / notification controls), play statistics,
 * queue persistence, sleep timer and A-B looping.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var queueDao: QueueDao
    @Inject lateinit var artworkLoader: ArtworkLoader
    @Inject lateinit var audioEffects: AudioEffectsController
    @Inject lateinit var visualizerController: VisualizerController
    @Inject lateinit var sleepTimer: SleepTimer
    @Inject lateinit var loopController: AbLoopController
    @Inject lateinit var widgetUpdater: WidgetUpdater

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentSongId = -1L
    private var accumulatedPlayMs = 0L
    private var countedForCurrent = false
    private var minTrackSeconds = 20
    private var crossfadeSeconds = 0

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(PlayerListener())

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setBitmapLoader(AuralisBitmapLoader(artworkLoader))
            .build()

        sleepTimer.setCallback {
            serviceScope.launch { fadeOutAndPause() }
        }
        loopController.attach(player)

        observeSettings()
        startPositionLoop()
        restoreQueue()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        persistQueue()
        loopController.detach()
        audioEffects.release()
        visualizerController.stop()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.settings.collect { settings ->
                minTrackSeconds = settings.minTrackSeconds
                crossfadeSeconds = settings.crossfadeSeconds
                player.repeatMode = when (settings.repeatMode) {
                    1 -> Player.REPEAT_MODE_ALL
                    2 -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                player.shuffleModeEnabled = settings.shuffle
                player.setPlaybackSpeed(settings.playbackSpeed.coerceIn(0.5f, 2f))
                player.volume = settings.volumeBoost.coerceIn(0.2f, 1f)
                player.skipSilenceEnabled = false

                visualizerController.barCount = settings.visualizerBars
                visualizerController.sensitivity = settings.visualizerSensitivity
                visualizerController.smoothing = settings.visualizerSmoothing

                if (settings.eqEnabled) {
                    audioEffects.attach(player.audioSessionId)
                    audioEffects.setEnabled(true)
                    val levels = settings.eqLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (levels.isNotEmpty()) audioEffects.applyLevels(levels, settings.eqPresetName)
                    audioEffects.setBassBoost(settings.bassBoost)
                    audioEffects.setTrebleBoost(settings.trebleBoost)
                    audioEffects.setVirtualizer(settings.virtualizer)
                    audioEffects.setLoudness(settings.loudnessGain)
                } else {
                    audioEffects.setEnabled(false)
                }
            }
        }
    }

    private fun startPositionLoop() {
        serviceScope.launch {
            var lastPersist = 0L
            while (isActive) {
                delay(500)
                if (player.isPlaying) {
                    accumulatedPlayMs += 500
                    maybeCountPlay()
                    applyCrossfadeVolume()
                }
                val now = System.currentTimeMillis()
                if (now - lastPersist > 5_000) {
                    lastPersist = now
                    persistQueue()
                    widgetUpdater.update()
                }
            }
        }
    }

    private fun applyCrossfadeVolume() {
        if (crossfadeSeconds <= 0) return
        val duration = player.duration
        if (duration <= 0) return
        val remaining = duration - player.currentPosition
        val fadeMs = crossfadeSeconds * 1000L
        val target = when {
            remaining in 0..fadeMs -> remaining.toFloat() / fadeMs
            player.currentPosition in 0..fadeMs -> player.currentPosition.toFloat() / fadeMs
            else -> 1f
        }.coerceIn(0.05f, 1f)
        if (abs(player.volume - target) > 0.02f) player.volume = target
    }

    private fun maybeCountPlay() {
        if (countedForCurrent || currentSongId <= 0) return
        val duration = player.duration.takeIf { it > 0 } ?: return
        val threshold = minOf(minTrackSeconds * 1000L, duration / 2)
        if (accumulatedPlayMs >= threshold) {
            countedForCurrent = true
            val songId = currentSongId
            val played = accumulatedPlayMs
            serviceScope.launch {
                withContext(Dispatchers.IO) { musicRepository.registerPlay(songId, played) }
            }
        }
    }

    private suspend fun fadeOutAndPause() {
        val startVolume = player.volume
        var step = 0
        while (step < 20 && player.isPlaying) {
            step++
            player.volume = (startVolume * (1f - step / 20f)).coerceAtLeast(0f)
            delay(75)
        }
        player.pause()
        player.volume = startVolume
    }

    private fun persistQueue() {
        val ids = (0 until player.mediaItemCount).map {
            MediaItems.songId(player.getMediaItemAt(it))
        }.filter { it > 0 }
        if (ids.isEmpty()) return
        val state = QueueStateEntity(
            id = 0,
            currentIndex = player.currentMediaItemIndex,
            positionMs = player.currentPosition,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode
        )
        serviceScope.launch(Dispatchers.IO) {
            runCatching { queueDao.persist(ids, state) }
        }
    }

    private fun restoreQueue() {
        serviceScope.launch {
            val settings = settingsRepository.settings.first()
            if (!settings.resumeOnStart) return@launch
            val restored = withContext(Dispatchers.IO) {
                val items = queueDao.items()
                val state = queueDao.state()
                if (items.isEmpty() || state == null) return@withContext null
                val songsById = musicRepository.songs.value.associateBy { it.id }
                val mediaItems = items.sortedBy { it.position }
                    .mapNotNull { songsById[it.songId] }
                    .map { MediaItems.from(it) }
                if (mediaItems.isEmpty()) null else mediaItems to state
            } ?: return@launch

            val (items, state) = restored
            if (player.mediaItemCount == 0) {
                player.setMediaItems(items, state.currentIndex.coerceIn(0, items.lastIndex), state.positionMs)
                player.shuffleModeEnabled = state.shuffle
                player.repeatMode = state.repeatMode
                player.prepare()
                player.playWhenReady = false
            }
        }
    }

    private inner class PlayerListener : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            currentSongId = MediaItems.songId(mediaItem)
            accumulatedPlayMs = 0
            countedForCurrent = false
            loopController.onTrackChanged(currentSongId)
            widgetUpdater.update()
            if (sleepTimer.consumeTrackEnd() && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                player.pause()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            widgetUpdater.update()
            if (isPlaying) {
                audioEffects.attach(player.audioSessionId)
                visualizerController.start(player.audioSessionId)
            } else {
                persistQueue()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // Skip unplayable/missing files instead of surfacing a raw exception.
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
                player.prepare()
            } else {
                player.pause()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && sleepTimer.consumeTrackEnd()) {
                player.pause()
            }
        }
    }
}
