package com.auralis.player.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        AbLoopEntity::class,
        EqPresetEntity::class,
        ExcludedFolderEntity::class,
        QueueItemEntity::class,
        QueueStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuralisDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun abLoopDao(): AbLoopDao
    abstract fun eqPresetDao(): EqPresetDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun queueDao(): QueueDao
}
