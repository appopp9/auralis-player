package com.auralis.player.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs")
    fun observeAll(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAll(): List<SongEntity>

    @Query("SELECT id FROM songs")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE songs SET isFavorite = :favorite, favoritedAt = :time WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, time: Long)

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :time WHERE id = :id")
    suspend fun incrementPlayCount(id: Long, time: Long)

    @Query("UPDATE songs SET lyrics = :lyrics WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String?)

    @Query(
        """UPDATE songs SET title = :title, artist = :artist, album = :album,
        albumArtist = :albumArtist, genre = :genre, composer = :composer,
        year = :year, trackNumber = :track, discNumber = :disc, lyrics = :lyrics
        WHERE id = :id"""
    )
    suspend fun updateTags(
        id: Long,
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
    )
}

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_songs ORDER BY position ASC")
    fun observeAllMembers(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name, updatedAt = :time WHERE id = :id")
    suspend fun rename(id: Long, name: String, time: Long)

    @Query("UPDATE playlists SET updatedAt = :time WHERE id = :id")
    suspend fun touch(id: Long, time: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :id")
    suspend fun clearMembers(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :id ORDER BY position ASC")
    suspend fun members(id: Long): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<PlaylistSongEntity>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeMember(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int

    @Transaction
    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>, time: Long) {
        clearMembers(playlistId)
        insertMembers(
            orderedSongIds.mapIndexed { index, songId ->
                PlaylistSongEntity(playlistId, songId, index, time)
            }
        )
        touch(playlistId, time)
    }
}

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(entry: PlayHistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PlayHistoryEntity>>

    @Query("SELECT COALESCE(SUM(playedMs), 0) FROM play_history")
    fun observeTotalListened(): Flow<Long>

    @Query("SELECT * FROM play_history WHERE playedAt >= :since ORDER BY playedAt DESC")
    fun observeSince(since: Long): Flow<List<PlayHistoryEntity>>

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Dao
interface AbLoopDao {
    @Query("SELECT * FROM ab_loops ORDER BY id DESC")
    fun observeAll(): Flow<List<AbLoopEntity>>

    @Query("SELECT * FROM ab_loops WHERE songId = :songId ORDER BY id DESC")
    fun observeForSong(songId: Long): Flow<List<AbLoopEntity>>

    @Insert
    suspend fun insert(loop: AbLoopEntity): Long

    @Query("DELETE FROM ab_loops WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM eq_presets ORDER BY name ASC")
    fun observeAll(): Flow<List<EqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: EqPresetEntity): Long

    @Query("DELETE FROM eq_presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ExcludedFolderDao {
    @Query("SELECT * FROM excluded_folders ORDER BY path ASC")
    fun observeAll(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT path FROM excluded_folders")
    suspend fun paths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE path = :path")
    suspend fun delete(path: String)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue_items ORDER BY position ASC")
    suspend fun items(): List<QueueItemEntity>

    @Query("DELETE FROM queue_items")
    suspend fun clearItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QueueItemEntity>)

    @Query("SELECT * FROM queue_state WHERE id = 0")
    suspend fun state(): QueueStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: QueueStateEntity)

    @Transaction
    suspend fun persist(songIds: List<Long>, state: QueueStateEntity) {
        clearItems()
        insertItems(songIds.mapIndexed { index, id -> QueueItemEntity(0, id, index) })
        saveState(state)
    }
}
