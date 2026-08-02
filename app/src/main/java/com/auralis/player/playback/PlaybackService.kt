package com.auralis.player.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
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
import kotlin.math.sqrt

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
    @Inject lateinit var sessionInfo: PlaybackSessionInfo

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentSongId = -1L
    private var accumulatedPlayMs = 0L
    private var flushedMs = 0L
    private var countedForCurrent = false
    private var minTrackSeconds = 20
    private var crossfadeSeconds = 0

    /** When gapless is off, tracks are cross-faded by this much instead. */
    private var gaplessEnabled = true

    /** Notification preferences, read by the notification provider below. */
    private var showNotification = true
    private var notificationControls = true

    /** Null until the first settings snapshot, so focus is set exactly once. */
    private var respectAudioFocus: Boolean? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    /** Kept as a field so the artwork preference can be toggled at runtime. */
    private val bitmapLoader: AuralisBitmapLoader by lazy { AuralisBitmapLoader(artworkLoader) }

    /**
     * The user's configured level. `player.volume` is also written by the
     * crossfade ramp every 500 ms, so the two must never fight: the settings
     * observer writes directly only when no fade is in progress, and the ramp
     * always multiplies against this value.
     */
    private var baseVolume = 1f
    private var crossfadeActive = false

    /** A8 — balance and mono folding, which `player.volume` cannot do. */
    private val channelMixing = ChannelMixingAudioProcessor()

    // D20 — headset / Bluetooth behaviour, each independently toggleable.
    private var autoPlayOnConnect = false
    private var pauseOnDisconnect = true
    private var resumeOnReconnect = false
    private var pausedByRouteLoss = false

    private val audioManager: AudioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            if (addedDevices?.any { it.isHeadsetLike() } != true) return
            if (player.mediaItemCount == 0 || player.isPlaying) return
            if (autoPlayOnConnect || (resumeOnReconnect && pausedByRouteLoss)) {
                pausedByRouteLoss = false
                player.play()
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            if (removedDevices?.any { it.isHeadsetLike() } != true) return
            if (pauseOnDisconnect && player.isPlaying) {
                pausedByRouteLoss = true
                player.pause()
            }
        }
    }

    private fun AudioDeviceInfo.isHeadsetLike(): Boolean = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        type == AudioDeviceInfo.TYPE_USB_HEADSET

    override fun onCreate() {
        super.onCreate()

        // Media notification with a guaranteed previous / play-pause / next
        // row (compact and expanded), regardless of queue position.
        setMediaNotificationProvider(object : DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showPauseButton: Boolean
            ): ImmutableList<CommandButton> {
                val previous = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .setIconResId(androidx.media3.ui.R.drawable.exo_notification_previous)
                    .setDisplayName("Previous")
                    .setExtras(android.os.Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 0) })
                    .setEnabled(playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS))
                    .build()
                val playPause = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setIconResId(
                        if (showPauseButton) {
                            androidx.media3.ui.R.drawable.exo_notification_pause
                        } else {
                            androidx.media3.ui.R.drawable.exo_notification_play
                        }
                    )
                    .setDisplayName(if (showPauseButton) "Pause" else "Play")
                    .setExtras(android.os.Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1) })
                    .build()
                val next = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                    .setIconResId(androidx.media3.ui.R.drawable.exo_notification_next)
                    .setDisplayName("Next")
                    .setExtras(android.os.Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 2) })
                    .setEnabled(playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT))
                    .build()
                // Seek back / forward use the configurable increment and show in
                // the expanded notification alongside previous / play / next.
                val seekBack = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                    .setIconResId(com.auralis.player.R.drawable.ic_seek_back)
                    .setDisplayName("Seek back")
                    .setEnabled(playerCommands.contains(Player.COMMAND_SEEK_BACK))
                    .build()
                val seekForward = CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                    .setIconResId(com.auralis.player.R.drawable.ic_seek_forward)
                    .setDisplayName("Seek forward")
                    .setEnabled(playerCommands.contains(Player.COMMAND_SEEK_FORWARD))
                    .build()
                // "Notification controls" off keeps only play/pause, which is
                // the minimum a media notification must expose.
                return if (notificationControls) {
                    ImmutableList.of(previous, seekBack, playPause, seekForward, next)
                } else {
                    ImmutableList.of(playPause)
                }
            }
        })

        applyChannelMixing(balance = 0f, mono = false)

        // DefaultAudioProcessorChain keeps ExoPlayer's own silence-skipping and
        // Sonic (speed/pitch) processors in the chain and puts ours in front.
        // setAudioProcessors(...) would replace the chain and silently break
        // skipSilenceEnabled (A4) and playbackParameters (A5).
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(channelMixing)
                )
                .build()
        }

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(PlayerListener())

        // Publish the session id so the equaliser / visualiser on the UI side
        // can attach to the real audio session instead of guessing.
        sessionInfo.update(player.audioSessionId)

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
            .setBitmapLoader(bitmapLoader)
            .build()

        sleepTimer.setCallback {
            serviceScope.launch { fadeOutAndPause() }
        }
        loopController.attach(player)

        // D20 — device names need BLUETOOTH_CONNECT on Android 12+, but the
        // route type alone is enough for connect / disconnect behaviour, so
        // this works with no extra permission at all.
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, null) }

        observeSettings()
        startPositionLoop()
        restoreQueue()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * "Show notification" governs the idle / paused notification only: while a
     * foreground media service is actually playing, Android requires the
     * notification to stay up, so it is never hidden mid-playback.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (!showNotification && !player.isPlaying) return
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        flushListeningTime()
        persistQueue()
        runCatching { audioManager.unregisterAudioDeviceCallback(deviceCallback) }
        sessionInfo.clear()
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
                gaplessEnabled = settings.gaplessEnabled
                showNotification = settings.showNotification
                notificationControls = settings.notificationControls
                bitmapLoader.artworkEnabled = settings.notificationArtwork
                autoPlayOnConnect = settings.autoPlayOnConnect
                pauseOnDisconnect = settings.pauseOnDisconnect
                resumeOnReconnect = settings.resumeOnReconnect
                player.repeatMode = when (settings.repeatMode) {
                    1 -> Player.REPEAT_MODE_ALL
                    2 -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                player.shuffleModeEnabled = settings.shuffle

                // "Respect other apps" — re-applying the attributes swaps the
                // internal audio focus handler, so it is only done on change.
                if (respectAudioFocus != settings.respectAudioFocus) {
                    respectAudioFocus = settings.respectAudioFocus
                    runCatching {
                        player.setAudioAttributes(audioAttributes, settings.respectAudioFocus)
                    }
                }

                // A5 — pitch 1f time-stretches and keeps voices natural;
                // pitch == speed is the tape-deck effect.
                val speed = settings.playbackSpeed.coerceIn(0.25f, 4f)
                player.playbackParameters = PlaybackParameters(
                    speed,
                    if (settings.pitchCorrection) 1f else speed
                )

                // Seek increments are fixed at build time on ExoPlayer; the UI
                // applies the user's increment directly via seekTo instead.
                //
                // A3 — normalization below the neutral target cannot be done by
                // LoudnessEnhancer (it only adds gain), so the quiet direction is
                // applied here as a volume factor. It only runs when the user has
                // switched normalization ("ReplayGain") on.
                val normalizationFactor = if (settings.replayGainEnabled) {
                    val deltaDb = settings.targetLoudnessDb - AudioEffectsController.NEUTRAL_LOUDNESS_DB
                    if (deltaDb < 0) {
                        Math.pow(10.0, deltaDb / 20.0).toFloat().coerceIn(0.1f, 1f)
                    } else {
                        1f
                    }
                } else {
                    1f
                }
                baseVolume = (settings.volumeBoost.coerceIn(0.2f, 1f) * normalizationFactor)
                    .coerceIn(0.05f, 1f)
                if (!crossfadeActive) player.volume = baseVolume

                player.skipSilenceEnabled = settings.skipSilence

                // A8 — balance / mono fold, applied in the audio sink.
                applyChannelMixing(settings.balance, settings.monoAudio)

                visualizerController.barCount = settings.visualizerBars
                visualizerController.sensitivity = settings.visualizerSensitivity
                visualizerController.smoothing = settings.visualizerSmoothing

                if (settings.eqEnabled) {
                    audioEffects.attach(player.audioSessionId)
                    sessionInfo.update(player.audioSessionId)
                    audioEffects.setEnabled(true)
                    val levels = settings.eqLevels.split(",").mapNotNull { it.trim().toIntOrNull() }
                    if (levels.isNotEmpty()) audioEffects.applyLevels(levels, settings.eqPresetName)
                    audioEffects.setBassBoost(settings.bassBoost)
                    audioEffects.setTrebleBoost(settings.trebleBoost)
                    audioEffects.setVirtualizer(settings.virtualizer)
                } else {
                    audioEffects.setEnabled(false)
                }

                // A3 / A6 — normalization and reverb are independent of the
                // equaliser, so they attach on their own account.
                val normalizationTarget = if (settings.replayGainEnabled) {
                    settings.targetLoudnessDb
                } else {
                    AudioEffectsController.NEUTRAL_LOUDNESS_DB
                }
                if (normalizationTarget != AudioEffectsController.NEUTRAL_LOUDNESS_DB ||
                    settings.reverbPreset > 0 ||
                    settings.loudnessGain > 0
                ) {
                    audioEffects.attach(player.audioSessionId)
                    sessionInfo.update(player.audioSessionId)
                }
                audioEffects.setLoudness(settings.loudnessGain)
                audioEffects.setTargetLoudness(normalizationTarget)
                audioEffects.setReverbPreset(settings.reverbPreset)
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

    /**
     * A1 — equal-power crossfade. A linear ramp audibly dips in the middle,
     * so the linear position is passed through sqrt() and then multiplied by
     * the user's configured level rather than replacing it.
     */
    private fun applyCrossfadeVolume() {
        // With gapless off the tracks still need a short fade, otherwise the
        // switch in Settings would do nothing at all.
        val fadeSeconds = when {
            crossfadeSeconds > 0 -> crossfadeSeconds
            !gaplessEnabled -> NON_GAPLESS_FADE_SECONDS
            else -> 0
        }
        if (fadeSeconds <= 0) {
            if (crossfadeActive) {
                crossfadeActive = false
                player.volume = baseVolume
            }
            return
        }
        val duration = player.duration
        if (duration <= 0) return
        val position = player.currentPosition
        val remaining = duration - position
        val fadeMs = fadeSeconds * 1000L
        val linear = when {
            remaining in 0..fadeMs -> remaining.toFloat() / fadeMs
            position in 0..fadeMs -> position.toFloat() / fadeMs
            else -> 1f
        }.coerceIn(0f, 1f)

        crossfadeActive = linear < 1f
        val target = (sqrt(linear) * baseVolume).coerceIn(0f, 1f)
        if (abs(player.volume - target) > 0.02f) player.volume = target
    }

    /**
     * Builds the channel mixing matrices. The far channel is attenuated rather
     * than the near one boosted, so panning can never clip. A 1→2 matrix is
     * registered as well, or balance would do nothing on mono source files.
     */
    private fun applyChannelMixing(balance: Float, mono: Boolean) {
        val pan = balance.coerceIn(-1f, 1f)
        val left = if (pan > 0f) 1f - pan else 1f
        val right = if (pan < 0f) 1f + pan else 1f

        // Row-major, indexed inputChannel * outputCount + outputChannel.
        val stereo = if (mono) {
            floatArrayOf(0.5f * left, 0.5f * right, 0.5f * left, 0.5f * right)
        } else {
            floatArrayOf(left, 0f, 0f, right)
        }
        runCatching {
            channelMixing.putChannelMixingMatrix(ChannelMixingMatrix(2, 2, stereo))
            channelMixing.putChannelMixingMatrix(
                ChannelMixingMatrix(1, 2, floatArrayOf(left, right))
            )
        }
    }

    private fun maybeCountPlay() {
        if (countedForCurrent || currentSongId <= 0) return
        val duration = player.duration.takeIf { it > 0 } ?: return
        val threshold = minOf(minTrackSeconds * 1000L, duration / 2)
        if (accumulatedPlayMs >= threshold) {
            countedForCurrent = true
            val songId = currentSongId
            serviceScope.launch {
                withContext(Dispatchers.IO) { musicRepository.incrementPlayCount(songId) }
            }
        }
    }

    /**
     * Writes the not-yet-recorded stretch of real playing time for the current
     * track into the listening history. Delta-based and guarded against
     * non-positive values, so pause / seek / track changes never double count.
     */
    private fun flushListeningTime() {
        val songId = currentSongId
        if (songId <= 0) return
        val delta = accumulatedPlayMs - flushedMs
        if (delta <= 0L) return
        flushedMs = accumulatedPlayMs
        serviceScope.launch {
            withContext(Dispatchers.IO) { musicRepository.addListeningTime(songId, delta) }
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
        // An empty list is persisted too: clearing the queue must survive a
        // restart instead of resurrecting the previous one.
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
            // Record the outgoing track's listening time before resetting.
            flushListeningTime()
            currentSongId = MediaItems.songId(mediaItem)
            accumulatedPlayMs = 0
            flushedMs = 0
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
                sessionInfo.update(player.audioSessionId)
                audioEffects.attach(player.audioSessionId)
                visualizerController.start(player.audioSessionId)
            } else {
                // Pausing or backgrounding banks the time played so far.
                flushListeningTime()
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
            if (playbackState == Player.STATE_ENDED) {
                flushListeningTime()
                if (sleepTimer.consumeTrackEnd()) {
                    player.pause()
                }
            }
        }
    }

    companion object {
        /** Fade applied at track boundaries when gapless playback is off. */
        private const val NON_GAPLESS_FADE_SECONDS = 1
    }
}
