package com.auralis.player.playback

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auralis.player.R
import com.google.common.util.concurrent.MoreExecutors

/**
 * D22 — Quick Settings tile. Shows the current track and toggles playback
 * without opening the app. The tile owns a short-lived MediaController that is
 * released as soon as the shade closes, so it never keeps the service alive.
 */
@RequiresApi(Build.VERSION_CODES.N)
class AuralisTileService : TileService() {

    private var controller: MediaController? = null

    override fun onStartListening() {
        super.onStartListening()
        connect { render() }
    }

    override fun onStopListening() {
        super.onStopListening()
        releaseController()
    }

    override fun onDestroy() {
        releaseController()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        connect { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                // Nothing queued yet: opening the app is more useful than a
                // silent no-op.
                if (player.mediaItemCount == 0) {
                    startActivityAndCollapseCompat()
                    return@connect
                }
                player.play()
            }
            render()
        }
    }

    private fun connect(onReady: (MediaController) -> Unit) {
        val existing = controller
        if (existing != null) {
            onReady(existing)
            return
        }
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            runCatching {
                val player = future.get()
                controller = player
                onReady(player)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun render() {
        val tile = qsTile ?: return
        val player = controller
        val playing = player?.isPlaying == true
        val title = runCatching {
            player?.mediaMetadata?.title?.toString()
        }.getOrNull()

        tile.state = if (player != null && player.mediaItemCount > 0) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(
            this,
            if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = title ?: if (playing) "Playing" else "Paused"
        }
        runCatching { tile.updateTile() }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun startActivityAndCollapseCompat() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = android.app.PendingIntent.getActivity(
                this,
                0,
                intent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    private fun releaseController() {
        runCatching { controller?.release() }
        controller = null
    }
}
