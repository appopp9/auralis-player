package com.auralis.player.data.scanner

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import com.auralis.player.data.db.ExcludedFolderDao
import com.auralis.player.data.db.SongDao
import com.auralis.player.data.db.SongEntity
import com.auralis.player.domain.model.ScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans device audio through MediaStore. Runs completely off the main thread,
 * reports progress, removes stale rows and skips duplicate files.
 */
@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val excludedFolderDao: ExcludedFolderDao
) {
    private val _progress = MutableStateFlow(ScanProgress())
    val progress: StateFlow<ScanProgress> = _progress.asStateFlow()

    private val mutex = Mutex()
    private var job: Job? = null

    fun scanAsync(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) { scan() }
    }

    suspend fun scan(): ScanProgress = mutex.withLock {
        withContext(Dispatchers.IO) {
            _progress.value = ScanProgress(running = true)
            val excluded = runCatching { excludedFolderDao.paths() }.getOrDefault(emptyList())
            val existingIds = runCatching { songDao.getAllIds() }.getOrDefault(emptyList()).toHashSet()

            val found = ArrayList<SongEntity>(512)
            val seenSignatures = HashSet<String>()
            val seenIds = HashSet<Long>()

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ARTIST_ID)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.COMPOSER)
                add(MediaStore.Audio.Media.YEAR)
                add(MediaStore.Audio.Media.TRACK)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.DATA)
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.DATE_MODIFIED)
                add(MediaStore.Audio.Media.DISPLAY_NAME)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.ALBUM_ARTIST)
                    add(MediaStore.Audio.Media.DISC_NUMBER)
                }
            }.toTypedArray()

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
            val cursor: Cursor? = runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
                )
            }.getOrNull()

            if (cursor == null) {
                _progress.value = ScanProgress(running = false, finishedAt = System.currentTimeMillis())
                return@withContext _progress.value
            }

            val genreFallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) emptyMap() else legacyGenreMap()

            cursor.use { c ->
                val total = c.count
                val idIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val composerIdx = c.getColumnIndex(MediaStore.Audio.Media.COMPOSER)
                val yearIdx = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val trackIdx = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val durationIdx = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataIdx = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val mimeIdx = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeIdx = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val addedIdx = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val modifiedIdx = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val displayIdx = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val genreIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                val albumArtistIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST) else -1
                val discIdx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    c.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER) else -1

                var processed = 0
                while (c.moveToNext()) {
                    processed++
                    val id = c.getLong(idIdx)
                    val path = if (dataIdx >= 0) c.getString(dataIdx).orEmpty() else ""
                    val displayName = if (displayIdx >= 0) c.getString(displayIdx).orEmpty() else ""
                    val folderPath = when {
                        path.isNotEmpty() -> File(path).parent.orEmpty()
                        else -> ""
                    }
                    if (excluded.any { folderPath.startsWith(it, ignoreCase = true) }) continue
                    if (!supportedFormat(path, if (mimeIdx >= 0) c.getString(mimeIdx) else null)) continue

                    val duration = c.getLong(durationIdx)
                    val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else 0L
                    val title = c.getString(titleIdx)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.').ifBlank { "Unknown" }
                    val artist = normalize(c.getString(artistIdx))
                    val album = normalize(c.getString(albumIdx))

                    val signature = "${title.lowercase(Locale.ROOT)}|${artist.lowercase(Locale.ROOT)}|$duration|$size"
                    if (!seenSignatures.add(signature)) continue
                    if (!seenIds.add(id)) continue

                    val rawTrack = if (trackIdx >= 0) c.getInt(trackIdx) else 0
                    val disc = when {
                        discIdx >= 0 -> c.getString(discIdx)?.substringBefore('/')?.trim()?.toIntOrNull() ?: (rawTrack / 1000)
                        rawTrack >= 1000 -> rawTrack / 1000
                        else -> 1
                    }.coerceAtLeast(1)
                    val track = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack
                    val genre = when {
                        genreIdx >= 0 -> c.getString(genreIdx).orEmpty()
                        else -> genreFallback[id].orEmpty()
                    }.ifBlank { "Unknown genre" }
                    val albumArtist = when {
                        albumArtistIdx >= 0 -> c.getString(albumArtistIdx).orEmpty()
                        else -> ""
                    }.ifBlank { artist }

                    found += SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        artistId = c.getLong(artistIdIdx),
                        album = album,
                        albumId = c.getLong(albumIdIdx),
                        albumArtist = albumArtist,
                        genre = genre,
                        composer = if (composerIdx >= 0) c.getString(composerIdx).orEmpty() else "",
                        year = if (yearIdx >= 0) c.getInt(yearIdx) else 0,
                        trackNumber = track,
                        discNumber = disc,
                        durationMs = duration,
                        path = path.ifEmpty {
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                            ).toString()
                        },
                        folderPath = folderPath,
                        folderName = folderPath.substringAfterLast('/').ifBlank { "Storage" },
                        mimeType = if (mimeIdx >= 0) c.getString(mimeIdx).orEmpty() else "audio/*",
                        sizeBytes = size,
                        dateAddedSec = if (addedIdx >= 0) c.getLong(addedIdx) else 0L,
                        dateModifiedSec = if (modifiedIdx >= 0) c.getLong(modifiedIdx) else 0L,
                        mood = MoodClassifier.classify(genre, duration, title),
                        lyrics = null
                    )

                    if (processed % 40 == 0 || processed == total) {
                        _progress.value = ScanProgress(
                            running = true,
                            processed = processed,
                            total = total,
                            currentPath = folderPath
                        )
                    }
                }
            }

            // Preserve user data (favorites, counters, custom tags) across rescans.
            val previous = songDao.getAll().associateBy { it.id }
            val merged = found.map { fresh ->
                val old = previous[fresh.id] ?: return@map fresh
                fresh.copy(
                    isFavorite = old.isFavorite,
                    favoritedAt = old.favoritedAt,
                    playCount = old.playCount,
                    lastPlayedAt = old.lastPlayedAt,
                    lyrics = old.lyrics ?: fresh.lyrics
                )
            }
            merged.chunked(400).forEach { songDao.upsertAll(it) }

            val removed = existingIds.filter { it !in seenIds }
            if (removed.isNotEmpty()) removed.chunked(400).forEach { songDao.deleteByIds(it) }

            val result = ScanProgress(
                running = false,
                processed = merged.size,
                total = merged.size,
                finishedAt = System.currentTimeMillis(),
                addedCount = merged.count { it.id !in existingIds },
                removedCount = removed.size
            )
            _progress.value = result
            result
        }
    }

    private fun normalize(value: String?): String =
        value?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown"

    private fun supportedFormat(path: String, mime: String?): Boolean {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext in SUPPORTED_EXTENSIONS) return true
        return mime?.startsWith("audio/") == true
    }

    @Suppress("DEPRECATION")
    private fun legacyGenreMap(): Map<Long, String> {
        val map = HashMap<Long, String>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null, null, null
            )?.use { genres ->
                while (genres.moveToNext()) {
                    val genreId = genres.getLong(0)
                    val name = genres.getString(1).orEmpty()
                    context.contentResolver.query(
                        MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null, null, null
                    )?.use { members ->
                        while (members.moveToNext()) map[members.getLong(0)] = name
                    }
                }
            }
        }
        return map
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "ogg", "oga", "m4a", "m4b", "opus", "aiff", "aif", "wma", "mka", "mp4"
        )
    }
}

object MoodClassifier {
    private val energetic = listOf("rock", "metal", "punk", "electronic", "dance", "edm", "techno", "house", "trance", "drum")
    private val chill = listOf("ambient", "chill", "lofi", "lo-fi", "downtempo", "acoustic", "folk")
    private val focus = listOf("classical", "instrumental", "soundtrack", "piano", "score", "orchestra")
    private val romantic = listOf("soul", "r&b", "rnb", "love", "ballad", "jazz", "blues")
    private val party = listOf("pop", "hip hop", "hip-hop", "rap", "latin", "reggaeton", "disco", "funk")

    fun classify(genre: String, durationMs: Long, title: String): String {
        val haystack = (genre + " " + title).lowercase(Locale.ROOT)
        return when {
            energetic.any { haystack.contains(it) } -> "Energetic"
            party.any { haystack.contains(it) } -> "Upbeat"
            romantic.any { haystack.contains(it) } -> "Romantic"
            focus.any { haystack.contains(it) } -> "Focus"
            chill.any { haystack.contains(it) } -> "Chill"
            durationMs > 6 * 60_000 -> "Deep listen"
            durationMs < 2 * 60_000 -> "Quick hits"
            else -> "Everyday"
        }
    }

    val ALL = listOf("Energetic", "Upbeat", "Romantic", "Focus", "Chill", "Deep listen", "Quick hits", "Everyday")
}
