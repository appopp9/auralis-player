package com.auralis.player.data.repository

import com.auralis.player.core.Fuzzy
import com.auralis.player.data.db.AbLoopEntity
import com.auralis.player.data.db.AbLoopDao
import com.auralis.player.data.db.EqPresetDao
import com.auralis.player.data.db.EqPresetEntity
import com.auralis.player.data.db.ExcludedFolderDao
import com.auralis.player.data.db.ExcludedFolderEntity
import com.auralis.player.data.db.HistoryDao
import com.auralis.player.data.db.PlayHistoryEntity
import com.auralis.player.data.db.SongDao
import com.auralis.player.data.db.toDomain
import com.auralis.player.di.ApplicationScope
import com.auralis.player.domain.model.AbLoop
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.EqualizerPreset
import com.auralis.player.domain.model.FolderSummary
import com.auralis.player.domain.model.Genre
import com.auralis.player.domain.model.MoodBucket
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.SortOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val folders: List<FolderSummary> = emptyList()
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() &&
            genres.isEmpty() && folders.isEmpty()
}

@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    private val abLoopDao: AbLoopDao,
    private val eqPresetDao: EqPresetDao,
    private val excludedFolderDao: ExcludedFolderDao,
    @ApplicationScope private val scope: CoroutineScope
) {

    val songs: StateFlow<List<Song>> = songDao.observeAll()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = songs
        .map { list -> buildAlbums(list) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = songs
        .map { list -> buildArtists(list) { it.displayArtist } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albumArtists: StateFlow<List<Artist>> = songs
        .map { list -> buildArtists(list) { it.albumArtist.ifBlank { it.displayArtist } } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val genres: StateFlow<List<Genre>> = songs
        .map { list ->
            list.groupBy { it.genre.ifBlank { "Unknown genre" } }
                .map { (name, items) -> Genre(name, items.size, items.first().id) }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val moods: StateFlow<List<MoodBucket>> = songs
        .map { list ->
            list.groupBy { it.mood.ifBlank { "Everyday" } }
                .map { (name, items) -> MoodBucket(name, items.size, items.first().id) }
                .sortedByDescending { it.songCount }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<FolderSummary>> = songs
        .map { list ->
            list.filter { it.folderPath.isNotBlank() }
                .groupBy { it.folderPath }
                .map { (path, items) ->
                    FolderSummary(path, items.first().folderName, items.size, items.first().id)
                }
                .sortedBy { it.name.lowercase(Locale.ROOT) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val favorites: StateFlow<List<Song>> = songs
        .map { list -> list.filter { it.isFavorite }.sortedBy { it.title.lowercase(Locale.ROOT) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyAdded: StateFlow<List<Song>> = songs
        .map { list -> list.sortedByDescending { it.dateAddedSec }.take(60) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayed: StateFlow<List<Song>> = songs
        .map { list -> list.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(60) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostPlayed: StateFlow<List<Song>> = songs
        .map { list -> list.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(60) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val neverPlayed: StateFlow<List<Song>> = songs
        .map { list -> list.filter { it.playCount == 0 }.sortedBy { it.title.lowercase(Locale.ROOT) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val excludedFolders: Flow<List<String>> =
        excludedFolderDao.observeAll().map { list -> list.map { it.path } }

    val customPresets: Flow<List<EqualizerPreset>> =
        eqPresetDao.observeAll().map { list -> list.map { it.toDomain() } }

    val abLoops: Flow<List<AbLoop>> =
        abLoopDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun songById(id: Long): Song? = songs.value.firstOrNull { it.id == id }

    suspend fun songByIdSuspend(id: Long): Song? = songDao.getById(id)?.toDomain()

    fun songsOfAlbum(albumId: Long): List<Song> = songs.value
        .filter { it.albumId == albumId }
        .sortedWith(compareBy({ it.discNumber }, { it.trackNumber }, { it.title }))

    fun songsOfArtist(name: String): List<Song> = songs.value
        .filter { it.displayArtist.equals(name, true) || it.albumArtist.equals(name, true) }
        .sortedWith(compareBy({ it.album }, { it.discNumber }, { it.trackNumber }))

    fun songsOfGenre(name: String): List<Song> = songs.value
        .filter { it.genre.equals(name, true) }
        .sortedBy { it.title.lowercase(Locale.ROOT) }

    fun songsOfMood(name: String): List<Song> = songs.value
        .filter { it.mood.equals(name, true) }
        .sortedBy { it.title.lowercase(Locale.ROOT) }

    fun songsOfFolder(path: String, recursive: Boolean = false): List<Song> = songs.value
        .filter { if (recursive) it.folderPath.startsWith(path) else it.folderPath == path }
        .sortedBy { it.title.lowercase(Locale.ROOT) }

    suspend fun toggleFavorite(songId: Long) {
        val song = songDao.getById(songId) ?: return
        songDao.setFavorite(songId, !song.isFavorite, System.currentTimeMillis())
    }

    suspend fun setFavorite(songId: Long, favorite: Boolean) {
        songDao.setFavorite(songId, favorite, System.currentTimeMillis())
    }

    suspend fun registerPlay(songId: Long, playedMs: Long) {
        val now = System.currentTimeMillis()
        songDao.incrementPlayCount(songId, now)
        historyDao.insert(PlayHistoryEntity(songId = songId, playedAt = now, playedMs = playedMs))
    }

    suspend fun addExcludedFolder(path: String) =
        excludedFolderDao.insert(ExcludedFolderEntity(path))

    suspend fun removeExcludedFolder(path: String) = excludedFolderDao.delete(path)

    suspend fun saveAbLoop(songId: Long, label: String, startMs: Long, endMs: Long): Long =
        abLoopDao.insert(AbLoopEntity(songId = songId, label = label, startMs = startMs, endMs = endMs))

    suspend fun deleteAbLoop(id: Long) = abLoopDao.delete(id)

    fun abLoopsForSong(songId: Long): Flow<List<AbLoop>> =
        abLoopDao.observeForSong(songId).map { list -> list.map { it.toDomain() } }

    suspend fun saveCustomPreset(name: String, levels: List<Int>) {
        eqPresetDao.insert(
            EqPresetEntity(name = name, levels = levels.joinToString(","), bandCount = levels.size)
        )
    }

    suspend fun deleteCustomPreset(id: Long) = eqPresetDao.delete(id)

    suspend fun updateTags(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        composer: String,
        year: Int,
        track: Int,
        disc: Int,
        lyrics: String?
    ) = songDao.updateTags(songId, title, artist, album, albumArtist, genre, composer, year, track, disc, lyrics)

    suspend fun removeSongsFromLibrary(ids: List<Long>) = songDao.deleteByIds(ids)

    fun search(query: String, limit: Int = 40): SearchResults {
        if (query.isBlank()) return SearchResults()
        val allSongs = songs.value
        val scoredSongs = allSongs.mapNotNull { song ->
            val score = maxOf(
                Fuzzy.score(song.title, query),
                Fuzzy.score(song.displayArtist, query) * 0.9f,
                Fuzzy.score(song.displayAlbum, query) * 0.85f,
                Fuzzy.score(song.folderName, query) * 0.6f
            )
            if (score > 0f) song to score else null
        }.sortedByDescending { it.second }.take(limit).map { it.first }

        val scoredArtists = artists.value.mapNotNull {
            val score = Fuzzy.score(it.name, query)
            if (score > 0f) it to score else null
        }.sortedByDescending { it.second }.take(12).map { it.first }

        val scoredAlbums = albums.value.mapNotNull {
            val score = maxOf(Fuzzy.score(it.name, query), Fuzzy.score(it.artist, query) * 0.8f)
            if (score > 0f) it to score else null
        }.sortedByDescending { it.second }.take(12).map { it.first }

        val scoredGenres = genres.value.filter { Fuzzy.matches(it.name, query) }.take(8)
        val scoredFolders = folders.value.mapNotNull {
            val score = maxOf(Fuzzy.score(it.name, query), Fuzzy.score(it.path, query) * 0.7f)
            if (score > 0f) it to score else null
        }.sortedByDescending { it.second }.take(8).map { it.first }

        return SearchResults(scoredSongs, scoredArtists, scoredAlbums, scoredGenres, scoredFolders)
    }

    private fun buildAlbums(list: List<Song>): List<Album> =
        list.groupBy { it.albumId }
            .map { (id, items) ->
                val first = items.first()
                Album(
                    id = id,
                    name = first.displayAlbum,
                    artist = first.displayArtist,
                    albumArtist = first.albumArtist.ifBlank { first.displayArtist },
                    songCount = items.size,
                    year = items.maxOf { it.year },
                    totalDurationMs = items.sumOf { it.durationMs },
                    artworkSongId = first.id
                )
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

    private fun buildArtists(list: List<Song>, selector: (Song) -> String): List<Artist> =
        list.groupBy(selector)
            .map { (name, items) ->
                Artist(
                    id = items.first().artistId,
                    name = name,
                    songCount = items.size,
                    albumCount = items.map { it.albumId }.distinct().size,
                    totalDurationMs = items.sumOf { it.durationMs },
                    artworkSongId = items.first().id
                )
            }
            .sortedBy { it.name.lowercase(Locale.ROOT) }

    companion object {
        fun sort(songs: List<Song>, order: SortOrder): List<Song> = when (order) {
            SortOrder.TITLE_ASC -> songs.sortedBy { it.title.lowercase(Locale.ROOT) }
            SortOrder.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase(Locale.ROOT) }
            SortOrder.RECENTLY_ADDED -> songs.sortedByDescending { it.dateAddedSec }
            SortOrder.RECENTLY_PLAYED -> songs.sortedByDescending { it.lastPlayedAt }
            SortOrder.MOST_PLAYED -> songs.sortedByDescending { it.playCount }
            SortOrder.DURATION -> songs.sortedByDescending { it.durationMs }
            SortOrder.YEAR -> songs.sortedByDescending { it.year }
            SortOrder.ARTIST -> songs.sortedBy { it.displayArtist.lowercase(Locale.ROOT) }
            SortOrder.ALBUM -> songs.sortedBy { it.displayAlbum.lowercase(Locale.ROOT) }
        }
    }
}
