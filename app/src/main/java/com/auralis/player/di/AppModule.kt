package com.auralis.player.di

import android.content.Context
import androidx.room.Room
import com.auralis.player.data.db.AbLoopDao
import com.auralis.player.data.db.AuralisDatabase
import com.auralis.player.data.db.EqPresetDao
import com.auralis.player.data.db.ExcludedFolderDao
import com.auralis.player.data.db.HistoryDao
import com.auralis.player.data.db.PlaylistDao
import com.auralis.player.data.db.QueueDao
import com.auralis.player.data.db.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AuralisDatabase =
        Room.databaseBuilder(context, AuralisDatabase::class.java, "auralis.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideSongDao(db: AuralisDatabase): SongDao = db.songDao()
    @Provides fun providePlaylistDao(db: AuralisDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideHistoryDao(db: AuralisDatabase): HistoryDao = db.historyDao()
    @Provides fun provideAbLoopDao(db: AuralisDatabase): AbLoopDao = db.abLoopDao()
    @Provides fun provideEqPresetDao(db: AuralisDatabase): EqPresetDao = db.eqPresetDao()
    @Provides fun provideExcludedFolderDao(db: AuralisDatabase): ExcludedFolderDao = db.excludedFolderDao()
    @Provides fun provideQueueDao(db: AuralisDatabase): QueueDao = db.queueDao()
}
