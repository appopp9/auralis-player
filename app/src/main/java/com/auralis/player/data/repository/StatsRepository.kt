package com.auralis.player.data.repository

import com.auralis.player.data.db.HistoryDao
import com.auralis.player.di.ApplicationScope
import com.auralis.player.domain.model.HistoryEntry
import com.auralis.player.domain.model.ListeningStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val musicRepository: MusicRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    val stats: StateFlow<ListeningStats> = combine(
        musicRepository.songs,
        historyDao.observeRecent(500),
        historyDao.observeTotalListened()
    ) { songs, history, totalListened ->
        val songById = songs.associateBy { it.id }
        val topSongs = songs.filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(10)
            .map { it to it.playCount }

        val topArtists = songs.filter { it.playCount > 0 }
            .groupBy { it.displayArtist }
            .map { (name, items) -> name to items.sumOf { it.playCount } }
            .sortedByDescending { it.second }
            .take(8)

        val topAlbums = songs.filter { it.playCount > 0 }
            .groupBy { it.displayAlbum }
            .map { (name, items) -> name to items.sumOf { it.playCount } }
            .sortedByDescending { it.second }
            .take(8)

        val topGenres = songs.filter { it.playCount > 0 }
            .groupBy { it.genre }
            .map { (name, items) -> name to items.sumOf { it.playCount } }
            .sortedByDescending { it.second }
            .take(8)

        val entries = history.mapNotNull { entry ->
            val song = songById[entry.songId] ?: return@mapNotNull null
            HistoryEntry(entry.songId, song.title, song.displayArtist, entry.playedAt, entry.playedMs)
        }

        val daily = history
            .groupBy { dayFormat.format(Date(it.playedAt)) }
            .map { (day, items) -> day to items.sumOf { it.playedMs } }
            .takeLast(14)

        ListeningStats(
            totalSongs = songs.size,
            totalListeningMs = totalListened,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            history = entries.take(100),
            dailyMinutes = daily
        )
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        ListeningStats(0, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )

    suspend fun clearHistory() = historyDao.clear()
}
