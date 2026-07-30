package com.auralis.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.auralis.player.MainActivity
import com.auralis.player.R
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class WidgetSize { COMPACT, MEDIUM, LARGE }

/**
 * Central place that turns the current playback state into [RemoteViews] for all
 * three widget sizes. Listens to the player state via a foreground coroutine and
 * pushes updates to every installed instance.
 *
 * Implemented as a singleton-style object but driven by Hilt-provided deps
 * passed into [start] (called from [com.auralis.player.AuralisApp]).
 */
object WidgetRenderer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private const val ACTION_PLAY_PAUSE = "com.auralis.player.action.PLAY_PAUSE"
    private const val ACTION_NEXT = "com.auralis.player.action.NEXT"
    private const val ACTION_PREV = "com.auralis.player.action.PREV"

    fun start(
        context: Context,
        musicRepository: MusicRepository,
        settingsRepository: SettingsRepository,
        artworkLoader: ArtworkLoader
    ) {
        if (job?.isActive == true) return
        job = scope.launch {
            musicRepository.songs.collectLatest { songs ->
                val song = pickCurrent(context, songs)
                val artwork = song?.let { artworkLoader.cached(it.id, 256) }
                pushToAll(context, song, artwork)
            }
        }
    }

    private fun pickCurrent(context: Context, songs: List<Song>): Song? {
        // Heuristic until a proper Player.StateFlow is wired in: prefer the
        // most-recently-played song in the library.
        return songs.filter { it.lastPlayedAt > 0 }.maxByOrNull { it.lastPlayedAt }
    }

    private fun pushToAll(context: Context, song: Song?, artwork: Bitmap?) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            CompactWidgetProvider::class.java to WidgetSize.COMPACT,
            MediumWidgetProvider::class.java to WidgetSize.MEDIUM,
            LargeWidgetProvider::class.java to WidgetSize.LARGE
        ).forEach { (cls, size) ->
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            ids.forEach { id -> render(context, manager, id, size, song, artwork) }
        }
    }

    /** Public entry point used by the providers' [AppWidgetProvider.onUpdate]. */
    fun render(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        size: WidgetSize
    ) {
        render(context, manager, id, size, song = null, artwork = null)
    }

    private fun render(
        context: Context,
        manager: AppWidgetManager,
        id: Int,
        size: WidgetSize,
        song: Song?,
        artwork: Bitmap?
    ) {
        val layout = when (size) {
            WidgetSize.COMPACT -> R.layout.widget_compact
            WidgetSize.MEDIUM -> R.layout.widget_medium
            WidgetSize.LARGE -> R.layout.widget_large
        }
        val views = RemoteViews(context.packageName, layout)
        views.setTextViewText(R.id.widget_title, song?.title ?: context.getString(R.string.nothing_playing))
        if (size != WidgetSize.COMPACT) {
            views.setTextViewText(R.id.widget_artist, song?.displayArtist ?: "")
        }
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_note)
        }
        views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
        views.setOnClickPendingIntent(R.id.widget_play_pause, broadcastPendingIntent(context, ACTION_PLAY_PAUSE))
        if (size != WidgetSize.COMPACT) {
            views.setOnClickPendingIntent(R.id.widget_prev, broadcastPendingIntent(context, ACTION_PREV))
            views.setOnClickPendingIntent(R.id.widget_next, broadcastPendingIntent(context, ACTION_NEXT))
        }
        manager.updateAppWidget(id, views)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun broadcastPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, PlaybackService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
