package com.auralis.player.domain.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val albumArtist: String,
    val genre: String,
    val composer: String,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val durationMs: Long,
    val path: String,
    val folderPath: String,
    val folderName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val dateModifiedSec: Long,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val mood: String = "",
    val lyrics: String? = null
) {
    val artworkUri: String get() = "auralis://artwork/$id"
    val displayArtist: String get() = if (artist.isBlank() || artist == "<unknown>") "Unknown artist" else artist
    val displayAlbum: String get() = if (album.isBlank()) "Unknown album" else album
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtist: String,
    val songCount: Int,
    val year: Int,
    val totalDurationMs: Long,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val totalDurationMs: Long,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class Genre(
    val name: String,
    val songCount: Int,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class MoodBucket(
    val name: String,
    val songCount: Int,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class FolderSummary(
    val path: String,
    val name: String,
    val songCount: Int,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val artworkSongId: Long
) {
    val artworkUri: String get() = "auralis://artwork/$artworkSongId"
}

data class AbLoop(
    val id: Long,
    val songId: Long,
    val label: String,
    val startMs: Long,
    val endMs: Long
)

data class EqualizerPreset(
    val id: Long,
    val name: String,
    val bandLevels: List<Int>,
    val isBuiltIn: Boolean
)

data class ListeningStats(
    val totalSongs: Int,
    val totalListeningMs: Long,
    val topSongs: List<Pair<Song, Int>>,
    val topArtists: List<Pair<String, Int>>,
    val topAlbums: List<Pair<String, Int>>,
    val topGenres: List<Pair<String, Int>>,
    val history: List<HistoryEntry>,
    val dailyMinutes: List<Pair<String, Long>>
)

data class HistoryEntry(
    val songId: Long,
    val title: String,
    val artist: String,
    val playedAt: Long,
    val playedMs: Long
)

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String,
    val synchronized: Boolean
)

enum class SortOrder {
    TITLE_ASC, TITLE_DESC, RECENTLY_ADDED, RECENTLY_PLAYED, MOST_PLAYED,
    DURATION, YEAR, ARTIST, ALBUM
}

enum class LibraryTab { SONGS, ARTISTS, ALBUMS, ALBUM_ARTISTS, GENRES, FOLDERS, PLAYLISTS, MOODS, FAVORITES }

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class AccentPalette { BLUE, PURPLE, VIOLET, CYAN, PINK, PEACH, GREEN, ORANGE, CUSTOM }

enum class VisualizerMode { BARS, WAVE, CIRCULAR, SPECTRUM, PARTICLE, MINIMAL, AURORA, OFF }

enum class GridStyle { LIST, GRID }

data class ScanProgress(
    val running: Boolean = false,
    val processed: Int = 0,
    val total: Int = 0,
    val currentPath: String = "",
    val finishedAt: Long = 0L,
    val addedCount: Int = 0,
    val removedCount: Int = 0
)
