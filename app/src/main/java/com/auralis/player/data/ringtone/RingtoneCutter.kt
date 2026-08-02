package com.auralis.player.data.ringtone

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import com.auralis.player.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trims a segment out of a local audio file and installs it as the device
 * ringtone. Cutting is a container remux (MediaExtractor → MediaMuxer) into an
 * .m4a file, which works for AAC/M4A/MP4 sources; formats MediaMuxer cannot
 * write (e.g. MP3) fail cleanly and are reported to the user.
 */
@Singleton
class RingtoneCutter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canWrite(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)

    sealed class TrimResult {
        data object Success : TrimResult()
        data object NoPermission : TrimResult()
        data object UnsupportedFormat : TrimResult()
        data class Failed(val reason: String) : TrimResult()
    }

    /** Cut [startMs, endMs) out of [song] and set it as the default ringtone. */
    suspend fun trimAndSet(song: Song, startMs: Long, endMs: Long): TrimResult =
        withContext(Dispatchers.IO) {
            if (!canWrite()) return@withContext TrimResult.NoPermission
            if (endMs <= startMs) return@withContext TrimResult.Failed("Invalid range")
            val src = song.path
            if (!src.startsWith("/")) return@withContext TrimResult.Failed("No local file path")

            val tmp = File(context.cacheDir, "ringtone_trim_${song.id}.m4a")
            val cut = runCatching { cut(src, startMs, endMs, tmp) }.getOrDefault(false)
            if (!cut || !tmp.exists() || tmp.length() == 0L) {
                tmp.delete()
                return@withContext TrimResult.UnsupportedFormat
            }

            runCatching {
                val uri = insertRingtone(song, tmp)
                RingtoneManager.setActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE,
                    uri
                )
                tmp.delete()
                TrimResult.Success
            }.getOrElse {
                tmp.delete()
                TrimResult.Failed(it.message ?: "Couldn't set ringtone")
            }
        }

    /** Remux the [startMs, endMs) window of [sourcePath] into [outFile] (.m4a). */
    private fun cut(sourcePath: String, startMs: Long, endMs: Long, outFile: File): Boolean {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(sourcePath)
            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            if (trackIndex < 0 || format == null) return false
            extractor.selectTrack(trackIndex)

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(format)
            muxer.start()

            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()
            val endUs = endMs * 1000L
            var wroteAny = false
            while (true) {
                info.offset = 0
                info.size = extractor.readSampleData(buffer, 0)
                if (info.size < 0) break
                val t = extractor.sampleTime
                if (t > endUs) break
                info.presentationTimeUs = t
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(dstTrack, buffer, info)
                wroteAny = true
                extractor.advance()
            }
            wroteAny
        } catch (e: Exception) {
            false
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }
    }

    /** Insert the trimmed file into MediaStore as a ringtone and return its Uri. */
    private fun insertRingtone(song: Song, tmp: File): Uri {
        val resolver = context.contentResolver
        val name = "${song.title.ifBlank { "Auralis" }} (ringtone)"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.m4a")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.TITLE, name)
            put(MediaStore.Audio.Media.ARTIST, song.displayArtist)
            put(MediaStore.Audio.Media.IS_RINGTONE, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES)
                put(MediaStore.Audio.Media.IS_PENDING, true)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("MediaStore insert failed")
        resolver.openOutputStream(uri)?.use { out ->
            tmp.inputStream().use { it.copyTo(out) }
        } ?: throw IllegalStateException("Couldn't write ringtone data")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, false) }
            resolver.update(uri, done, null, null)
        }
        return uri
    }
}
