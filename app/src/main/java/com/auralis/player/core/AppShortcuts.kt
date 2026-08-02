package com.auralis.player.core

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.auralis.player.MainActivity
import com.auralis.player.R
import com.auralis.player.domain.model.Song

/**
 * D23 — launcher shortcuts. "Shuffle all" and "Resume" are static-feeling
 * entries that are always published; the rest are the most recently played
 * tracks, refreshed whenever the library or history changes.
 */
object AppShortcuts {

    const val EXTRA_ACTION = "com.auralis.player.SHORTCUT_ACTION"
    const val EXTRA_SONG_ID = "com.auralis.player.SHORTCUT_SONG_ID"

    const val ACTION_SHUFFLE_ALL = "shuffle_all"
    const val ACTION_RESUME = "resume"
    const val ACTION_PLAY_SONG = "play_song"

    private const val ID_SHUFFLE = "auralis_shuffle_all"
    private const val ID_RESUME = "auralis_resume"
    private const val PREFIX_SONG = "auralis_song_"

    /** Republishes the dynamic shortcut list. Safe to call often. */
    fun publish(context: Context, recentlyPlayed: List<Song>) {
        val max = runCatching { ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) }
            .getOrDefault(4)
            .coerceAtLeast(2)

        val shortcuts = mutableListOf(
            build(
                context,
                id = ID_SHUFFLE,
                shortLabel = "Shuffle all",
                longLabel = "Shuffle the whole library",
                iconRes = R.drawable.ic_widget_next,
                action = ACTION_SHUFFLE_ALL,
                songId = 0L
            ),
            build(
                context,
                id = ID_RESUME,
                shortLabel = "Resume",
                longLabel = "Resume where you left off",
                iconRes = R.drawable.ic_widget_play,
                action = ACTION_RESUME,
                songId = 0L
            )
        )

        recentlyPlayed.take((max - shortcuts.size).coerceAtLeast(0)).forEach { song ->
            shortcuts += build(
                context,
                id = PREFIX_SONG + song.id,
                shortLabel = song.title.take(24).ifBlank { "Track" },
                longLabel = "${song.title} — ${song.displayArtist}".take(48),
                iconRes = R.drawable.ic_widget_note,
                action = ACTION_PLAY_SONG,
                songId = song.id
            )
        }

        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }

    private fun build(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        iconRes: Int,
        action: String,
        songId: Long
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(IconCompat.createWithResource(context, iconRes))
        .setIntent(
            Intent(context, MainActivity::class.java).apply {
                this.action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_ACTION, action)
                putExtra(EXTRA_SONG_ID, songId)
            }
        )
        .build()
}
