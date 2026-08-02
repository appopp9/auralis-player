package com.auralis.player.data.artwork

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central artwork resolution + in-memory cache. Artwork is addressed with the
 * app scheme `auralis://artwork/{songId}` so every surface (UI, notification,
 * widgets) resolves art through the same cached path.
 */
@Singleton
class ArtworkLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val misses = HashSet<Long>()

    fun cached(songId: Long, size: Int): Bitmap? = cache.get(key(songId, size))

    suspend fun load(songId: Long, size: Int = 512): Bitmap? = withContext(Dispatchers.IO) {
        if (songId <= 0L) return@withContext null
        cache.get(key(songId, size))?.let { return@withContext it }
        synchronized(misses) { if (misses.contains(songId)) return@withContext null }
        val bitmap = resolve(songId, size)
        if (bitmap != null) {
            cache.put(key(songId, size), bitmap)
        } else {
            synchronized(misses) { misses.add(songId) }
        }
        bitmap
    }

    fun invalidate(songId: Long) {
        synchronized(misses) { misses.remove(songId) }
        listOf(96, 128, 256, 512, 1024).forEach { cache.remove(key(songId, it)) }
    }

    private fun key(songId: Long, size: Int) = "$songId@$size"

    private fun resolve(songId: Long, size: Int): Bitmap? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                return context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            }
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampled(bytes, size)
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun decodeSampled(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        var width = bounds.outWidth
        var height = bounds.outHeight
        while (width / sample > target * 2 && height / sample > target * 2) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    companion object {
        const val SCHEME = "auralis"
        fun uriFor(songId: Long): Uri = Uri.parse("$SCHEME://artwork/$songId")
        fun songIdOf(uri: Uri): Long? =
            if (uri.scheme == SCHEME) uri.lastPathSegment?.toLongOrNull() else null
    }
}
