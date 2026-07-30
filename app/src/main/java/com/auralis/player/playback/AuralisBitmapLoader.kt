package com.auralis.player.playback

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import com.auralis.player.data.artwork.ArtworkLoader
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Resolves notification / lock screen artwork through the shared artwork cache. */
class AuralisBitmapLoader(
    private val artworkLoader: ArtworkLoader
) : BitmapLoader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bitmap != null) future.set(bitmap)
            else future.setException(IllegalArgumentException("Unable to decode artwork"))
        }
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val songId = ArtworkLoader.songIdOf(uri)
            ?: return Futures.immediateFailedFuture(IllegalArgumentException("Unsupported uri"))
        val cached = artworkLoader.cached(songId, 512)
        if (cached != null) return Futures.immediateFuture(cached)
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val bitmap = artworkLoader.load(songId, 512)
            if (bitmap != null) future.set(bitmap)
            else future.setException(IllegalStateException("No artwork"))
        }
        return future
    }
}
