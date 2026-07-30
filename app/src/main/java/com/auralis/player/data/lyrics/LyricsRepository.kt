package com.auralis.player.data.lyrics

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.auralis.player.data.db.SongDao
import com.auralis.player.domain.model.LyricLine
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {

    suspend fun lyricsFor(song: Song, allowOnline: Boolean): Lyrics? = withContext(Dispatchers.IO) {
        song.lyrics?.takeIf { it.isNotBlank() }?.let { return@withContext LyricsParser.parse(it) }

        sidecarFile(song)?.let { file ->
            val text = runCatching { file.readText() }.getOrNull()
            if (!text.isNullOrBlank()) {
                songDao.updateLyrics(song.id, text)
                return@withContext LyricsParser.parse(text)
            }
        }

        embedded(song.id)?.let { text ->
            songDao.updateLyrics(song.id, text)
            return@withContext LyricsParser.parse(text)
        }

        if (allowOnline) {
            fetchOnline(song)?.let { text ->
                songDao.updateLyrics(song.id, text)
                return@withContext LyricsParser.parse(text)
            }
        }
        null
    }

    suspend fun save(songId: Long, text: String?) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, text)
    }

    private fun sidecarFile(song: Song): File? {
        if (!song.path.startsWith("/")) return null
        val base = song.path.substringBeforeLast('.', song.path)
        return listOf("$base.lrc", "$base.txt")
            .map(::File)
            .firstOrNull { it.exists() && it.canRead() }
    }

    private fun embedded(songId: Long): String? {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
                ?.takeIf { it.contains('\n') }
        } catch (t: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Opt-in only: nothing leaves the device unless the user enables online lyrics. */
    private fun fetchOnline(song: Song): String? {
        return runCatching {
            val artist = URLEncoder.encode(song.displayArtist, "UTF-8")
            val title = URLEncoder.encode(song.title, "UTF-8")
            val url = URL("https://lrclib.net/api/get?artist_name=$artist&track_name=$title")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "Auralis/1.0 (offline music player)")
            }
            connection.inputStream.bufferedReader().use { reader ->
                val body = reader.readText()
                extractJsonString(body, "syncedLyrics")
                    ?: extractJsonString(body, "plainLyrics")
            }
        }.getOrNull()
    }

    private fun extractJsonString(json: String, key: String): String? {
        val marker = "\"$key\":"
        val start = json.indexOf(marker).takeIf { it >= 0 } ?: return null
        var i = start + marker.length
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < json.length) {
            val ch = json[i]
            when {
                ch == '\\' && i + 1 < json.length -> {
                    when (val next = json[i + 1]) {
                        'n' -> sb.append('\n')
                        'r' -> {}
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = json.substring(i + 2, minOf(i + 6, json.length))
                            hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                            i += 4
                        }
                        else -> sb.append(next)
                    }
                    i += 2
                }
                ch == '"' -> return sb.toString().takeIf { it.isNotBlank() }
                else -> {
                    sb.append(ch)
                    i++
                }
            }
        }
        return null
    }
}

object LyricsParser {
    private val timeRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(raw: String): Lyrics {
        val lines = ArrayList<LyricLine>()
        raw.lineSequence().forEach { line ->
            val matches = timeRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fraction = match.groupValues[3]
                val millis = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                lines += LyricLine(minutes * 60_000 + seconds * 1000 + millis, text)
            }
        }
        return if (lines.isNotEmpty()) {
            Lyrics(lines.sortedBy { it.timeMs }, raw, synchronized = true)
        } else {
            val plain = raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { LyricLine(0L, it) }
                .toList()
            Lyrics(plain, raw, synchronized = false)
        }
    }
}
