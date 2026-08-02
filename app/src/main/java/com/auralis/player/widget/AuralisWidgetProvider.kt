package com.auralis.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.player.MainActivity
import com.auralis.player.R
import com.auralis.player.playback.PlaybackService
import com.google.common.util.concurrent.MoreExecutors

/**
 * Base implementation shared by the three widget sizes. Every control is wired
 * to the real media session, so widgets stay in sync with the player.
 */
abstract class AuralisWidgetProvider : AppWidgetProvider() {

    abstract val layoutRes: Int
    open val showsArtist: Boolean = false
    open val showsSkip: Boolean = false
    open val showsProgress: Boolean = false

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context, manager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREVIOUS -> command(context, intent.action!!)
        }
    }

    private fun command(context: Context, action: String) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                when (action) {
                    ACTION_PLAY_PAUSE -> if (controller.isPlaying) controller.pause() else {
                        controller.prepare()
                        controller.play()
                    }
                    ACTION_NEXT -> controller.seekToNextMediaItem()
                    ACTION_PREVIOUS -> controller.seekToPreviousMediaItem()
                }
                MediaController.releaseFuture(future)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            val views = RemoteViews(context.packageName, layoutRes)
            val metadata = controller?.mediaMetadata
            val title = metadata?.title?.toString()
            val artist = metadata?.artist?.toString()
            val isPlaying = controller?.isPlaying == true

            views.setTextViewText(R.id.widget_title, title ?: context.getString(R.string.nothing_playing))
            if (showsArtist) {
                views.setTextViewText(R.id.widget_artist, artist.orEmpty())
                views.setViewVisibility(
                    R.id.widget_artist,
                    if (artist.isNullOrBlank()) View.GONE else View.VISIBLE
                )
            }
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )

            // RemoteViews cannot await a coroutine, so the widget uses its own
            // small synchronous bitmap cache instead of the Coil-backed loader.
            val songId = controller?.currentMediaItem?.mediaId?.toLongOrNull() ?: -1L
            val bitmap = if (songId > 0) WidgetArtwork.get(context, songId) else null
            if (bitmap != null) views.setImageViewBitmap(R.id.widget_artwork, bitmap)
            else views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_note)

            if (showsProgress) {
                val duration = controller?.duration ?: 0L
                val position = controller?.currentPosition ?: 0L
                val progress = if (duration > 0) ((position * 1000) / duration).toInt() else 0
                views.setProgressBar(R.id.widget_progress, 1000, progress.coerceIn(0, 1000), false)
            }

            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            views.setOnClickPendingIntent(R.id.widget_play_pause, actionIntent(context, ACTION_PLAY_PAUSE))
            if (showsSkip) {
                views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT))
                views.setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, ACTION_PREVIOUS))
            }

            ids.forEach { manager.updateAppWidget(it, views) }
            if (controller != null) MediaController.releaseFuture(future)
        }, MoreExecutors.directExecutor())
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode() + layoutRes,
        Intent(context, this::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        const val ACTION_PLAY_PAUSE = "com.auralis.player.widget.PLAY_PAUSE"
        const val ACTION_NEXT = "com.auralis.player.widget.NEXT"
        const val ACTION_PREVIOUS = "com.auralis.player.widget.PREVIOUS"
    }
}

class CompactWidgetProvider : AuralisWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_compact
}

class MediumWidgetProvider : AuralisWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_medium
    override val showsArtist: Boolean = true
    override val showsSkip: Boolean = true
}

class LargeWidgetProvider : AuralisWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_large
    override val showsArtist: Boolean = true
    override val showsSkip: Boolean = true
    override val showsProgress: Boolean = true
}
