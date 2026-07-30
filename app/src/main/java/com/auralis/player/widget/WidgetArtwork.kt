package com.auralis.player.widget

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size

/**
 * Small synchronous artwork cache used by RemoteViews. Widgets cannot wait on
 * coroutines, so a compact bitmap cache keeps updates instant.
 */
object WidgetArtwork {

    private const val TARGET = 256
    private val cache = object : LruCache<Long, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }
    private val misses = HashSet<Long>()

    fun get(context: Context, songId: Long): Bitmap? {
        cache.get(songId)?.let { return it }
        synchronized(misses) { if (songId in misses) return null }
        val bitmap = load(context, songId)
        if (bitmap != null) cache.put(songId, bitmap)
        else synchronized(misses) { misses.add(songId) }
        return bitmap
    }

    private fun load(context: Context, songId: Long): Bitmap? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                return context.contentResolver.loadThumbnail(uri, Size(TARGET, TARGET), null)
            }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
