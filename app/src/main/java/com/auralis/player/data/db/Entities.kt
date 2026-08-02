package com.auralis.player.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index("album"), Index("artist"), Index("folderPath"), Index("genre"),
        Index("isFavorite"), Index("dateAddedSec"), Index("playCount")
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
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
    val mood: String,
    val lyrics: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val favoritedAt: Long = 0L,
    /** User-provided or imported translation (LRC or plain text). */
    val lyricsTranslation: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long
)

@Entity(tableName = "play_history", indices = [Index("songId"), Index("playedAt")])
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
    val playedMs: Long
)

@Entity(tableName = "ab_loops", indices = [Index("songId")])
data class AbLoopEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val label: String,
    val startMs: Long,
    val endMs: Long
)

@Entity(tableName = "eq_presets")
data class EqPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val levels: String,
    val bandCount: Int
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val path: String
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val position: Int
)

@Entity(tableName = "queue_state")
data class QueueStateEntity(
    @PrimaryKey val id: Int = 0,
    val currentIndex: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int
)
