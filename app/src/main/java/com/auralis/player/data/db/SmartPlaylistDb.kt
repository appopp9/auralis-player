package com.auralis.player.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Rules live in a single JSON column, so adding an operator or a field later
 * never costs a schema migration.
 */
@Entity(tableName = "smart_playlists")
data class SmartPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val rulesJson: String,
    val matchAll: Boolean,
    val limitCount: Int,
    val sortKey: String,
    val sortDescending: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface SmartPlaylistDao {

    @Query("SELECT * FROM smart_playlists ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SmartPlaylistEntity>>

    @Query("SELECT * FROM smart_playlists")
    suspend fun getAll(): List<SmartPlaylistEntity>

    @Query("SELECT * FROM smart_playlists WHERE id = :id")
    suspend fun byId(id: Long): SmartPlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(playlist: SmartPlaylistEntity): Long

    @Query("DELETE FROM smart_playlists WHERE id = :id")
    suspend fun delete(id: Long)
}
