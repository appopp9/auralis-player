package com.auralis.player.data.lyrics

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

/** Editable lyrics payload shown in the in-app lyrics editor. */
data class RawLyrics(val text: String, val translation: String)

/** Outcome of an explicit online lyrics download. */
sealed class LyricsFetchResult {
    /** Lyrics were downloaded and saved (or were already saved). */
    data class Success(val lyrics: Lyrics, val alreadySaved: Boolean) : LyricsFetchResult()
    /** The provider returned no lyrics for this track. */
    data object NotFound : LyricsFetchResult()
    /** No internet connection — cached lyrics (if any) remain usable. */
    data object Offline : LyricsFetchResult()
    /** A network / parsing error occurred. */
    data class Error(val message: String) : LyricsFetchResult()
}

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao
) {

    /**
     * Resolves the best available lyrics for a song, including any translation.
     * Order: cached in DB → sidecar files → embedded tag → (optional) online.
     * Anything found is cached back into the DB so it also works offline later.
     */
    suspend fun lyricsFor(song: Song, allowOnline: Boolean): Lyrics? = withContext(Dispatchers.IO) {
        val translation = translationFor(song)

        val mainText: String? = run {
            song.lyrics?.takeIf { it.isNotBlank() }?.let { return@run it }

            sidecarFile(song)?.let { file ->
                val text = runCatching { file.readText() }.getOrNull()
                if (!text.isNullOrBlank()) {
                    songDao.updateLyrics(song.id, text)
                    return@run text
                }
            }

            embedded(song.id)?.let { text ->
                songDao.updateLyrics(song.id, text)
                return@run text
            }

            if (allowOnline && isOnline()) {
                fetchOnline(song)?.let { text ->
                    songDao.updateLyrics(song.id, text)
                    return@run text
                }
            }
            null
        }
        val resolvedText = mainText ?: return@withContext null

        val parsed = LyricsParser.parse(resolvedText)
        parsed.copy(
            translationLines = translation?.let { LyricsParser.parse(it).lines } ?: emptyList(),
            translationPlainText = translation?.takeIf { LyricsParser.parse(it).lines.isEmpty() }
        )
    }

    /** True when a lyrics payload is already cached locally for offline use. */
    fun hasCached(song: Song): Boolean =
        !song.lyrics.isNullOrBlank() || sidecarFile(song) != null

    /**
     * Explicit user-triggered download. Distinguishes offline, not-found and
     * already-saved so the UI can give precise feedback instead of failing
     * silently. Requires connectivity; cached lyrics are returned when present.
     */
    suspend fun downloadOnline(song: Song): LyricsFetchResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            // Cached lyrics remain perfectly usable offline.
            song.lyrics?.takeIf { it.isNotBlank() }?.let {
                val parsed = LyricsParser.parse(it)
                return@withContext LyricsFetchResult.Success(parsed, alreadySaved = true)
            }
            return@withContext LyricsFetchResult.Offline
        }

        val text = try {
            fetchOnline(song)
        } catch (t: Throwable) {
            return@withContext LyricsFetchResult.Error(t.message ?: "Download failed")
        }

        if (text.isNullOrBlank()) return@withContext LyricsFetchResult.NotFound

        songDao.updateLyrics(song.id, text)
        val translation = translationFor(song)
        val parsed = LyricsParser.parse(text).copy(
            translationLines = translation?.let { LyricsParser.parse(it).lines } ?: emptyList(),
            translationPlainText = translation?.takeIf { LyricsParser.parse(it).lines.isEmpty() }
        )
        LyricsFetchResult.Success(parsed, alreadySaved = false)
    }

    /** Saves (or clears with null) the main lyrics for a song. */
    suspend fun save(songId: Long, text: String?) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, text?.takeIf { it.isNotBlank() })
    }

    /** Saves (or clears with null) the translation for a song. */
    suspend fun saveTranslation(songId: Long, text: String?) = withContext(Dispatchers.IO) {
        songDao.updateLyricsTranslation(songId, text?.takeIf { it.isNotBlank() })
    }

    /** Raw stored/derived lyrics + translation text, for the in-app editor. */
    suspend fun rawFor(song: Song): RawLyrics = withContext(Dispatchers.IO) {
        val main = song.lyrics?.takeIf { it.isNotBlank() }
            ?: sidecarFile(song)?.let { runCatching { it.readText() }.getOrNull() }
            ?: embedded(song.id)
        RawLyrics(main.orEmpty(), translationFor(song).orEmpty())
    }

    /** True if the device currently has a usable internet connection. */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun sidecarFile(song: Song): File? {
        if (!song.path.startsWith("/")) return null
        val base = song.path.substringBeforeLast('.', song.path)
        return listOf("$base.lrc", "$base.txt")
            .map(::File)
            .firstOrNull { it.exists() && it.canRead() }
    }

    /**
     * Looks for a translation sidecar next to the audio file. Supported names
     * for `/x/song.mp3`: `song.fa.lrc`, `song.translation.lrc`, `song.per.lrc`,
     * `song.ar.lrc`, `song.fa.txt`, `song.translation.txt`.
     */
    private fun translationFor(song: Song): String? {
        song.lyricsTranslation?.takeIf { it.isNotBlank() }?.let { return it }
        if (!song.path.startsWith("/")) return null
        val base = song.path.substringBeforeLast('.', song.path)
        val candidates = listOf(
            "$base.fa.lrc", "$base.translation.lrc", "$base.per.lrc", "$base.ar.lrc",
            "$base.fa.txt", "$base.translation.txt"
        )
        val file = candidates.map(::File).firstOrNull { it.exists() && it.canRead() } ?: return null
        return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
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

    /**
     * Finds the translation text for a given playback position by matching the
     * nearest synced translation timestamp (within [toleranceMs]).
     */
    fun translationAt(translationLines: List<LyricLine>, timeMs: Long, toleranceMs: Long = 1200): String? {
        if (translationLines.isEmpty()) return null
        var best: LyricLine? = null
        var bestDelta = Long.MAX_VALUE
        for (line in translationLines) {
            val delta = kotlin.math.abs(line.timeMs - timeMs)
            if (delta < bestDelta) {
                bestDelta = delta
                best = line
            }
        }
        return if (bestDelta <= toleranceMs) best?.text else null
    }
}
