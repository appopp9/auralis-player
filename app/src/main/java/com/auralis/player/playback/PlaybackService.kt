package com.auralis.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.auralis.player.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground media playback service backed by ExoPlayer and exposed to the system
 * (notification, Bluetooth, Android Auto, Assistants) through a [MediaSession].
 *
 * Hilt provides the [PlaybackController] which owns the [ExoPlayer] instance so
 * UI and service share the same player.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var playbackController: PlaybackController

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = playbackController.player
        val sessionActivityIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(playbackController)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // Release only the MediaSession; the underlying ExoPlayer is owned by
        // PlaybackController (Hilt singleton) and lives for the process lifetime
        // so that UI can keep observing state across service recreation.
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession
}
