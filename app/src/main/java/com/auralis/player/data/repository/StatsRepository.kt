package com.auralis.player.data.repository

import com.auralis.player.data.db.HistoryDao
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.di.ApplicationScope
import com.auralis.player.domain.model.HeatmapDay
import com.auralis.player.domain.model.HistoryEntry
import com.auralis.player.domain.model.ListeningStats
import com.auralis.player.domain.model.Trend
import com.auralis.player.domain.model.TrendingSong
import com.auralis.player.domain.model.WrappedData
import java.util.Calendar
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
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val dayFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    val stats: StateFlow<ListeningStats> = combine(
        musicRepository.songs,
        historyDao.observeRecent(500),
        historyDao.observeTotalListened(),
        settingsRepository.settings
    ) { songs, history, totalListened, settings ->
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

        // A full, chronological last-14-days window. Missing days are real
        // zeros (no listening that day), and the UI hides the chart entirely
        // when the whole window is empty instead of drawing fake bars.
        val dayMs = 86_400_000L
        val todayStart = (System.currentTimeMillis() / dayMs) * dayMs
        val sums = HashMap<Int, Long>()
        history.forEach { entry ->
            val dayIndex = ((todayStart - (entry.playedAt / dayMs) * dayMs) / dayMs).toInt()
            if (dayIndex in 0..13) {
                sums[dayIndex] = (sums[dayIndex] ?: 0L) + entry.playedMs
            }
        }
        val daily = (13 downTo 0).map { index ->
            val dayStart = todayStart - index * dayMs
            dayFormat.format(Date(dayStart)) to (sums[index] ?: 0L)
        }

        // ---- Listening activity grid -------------------------------------
        // Calendar-correct: every column is a real week starting on the
        // locale's first weekday, and the last column is the current week
        // (future days are rendered as inert placeholders). The previous
        // implementation bucketed by "days ago / 7", so columns drifted and
        // rows never matched real weekdays.
        val heatWeekCount = 20
        val perDay = HashMap<Long, Long>()
        history.forEach { entry ->
            val key = localDayStart(entry.playedAt)
            perDay[key] = (perDay[key] ?: 0L) + entry.playedMs
        }

        val today = localDayStart(System.currentTimeMillis())
        val firstWeekday = Calendar.getInstance().firstDayOfWeek
        val todayCal = Calendar.getInstance().apply { timeInMillis = today }
        // Days since the start of the current week.
        val offsetInWeek = ((todayCal.get(Calendar.DAY_OF_WEEK) - firstWeekday) + 7) % 7
        val currentWeekStart = today - offsetInWeek * dayMs
        val gridStart = currentWeekStart - (heatWeekCount - 1L) * dayMs * 7L

        val heatWeeks = (0 until heatWeekCount).map { week ->
            (0 until 7).map { day ->
                val dayStart = gridStart + (week * 7L + day) * dayMs
                HeatmapDay(
                    dayStartMs = dayStart,
                    listenedMs = perDay[dayStart] ?: 0L,
                    inFuture = dayStart > today
                )
            }
        }
        val gridDays = heatWeeks.flatten().filterNot { it.inFuture }
        val heatMax = gridDays.maxOfOrNull { it.listenedMs } ?: 0L
        val activeDays = gridDays.count { it.listenedMs > 0L }
        val averageDaily = if (activeDays > 0) {
            gridDays.sumOf { it.listenedMs } / activeDays
        } else {
            0L
        }
        val bestDay = gridDays.filter { it.listenedMs > 0L }.maxByOrNull { it.listenedMs }

        // Current streak: walk back from today (a still-silent today doesn't
        // break a streak that was alive yesterday).
        var currentStreak = 0
        var cursor = if ((perDay[today] ?: 0L) > 0L) today else today - dayMs
        while ((perDay[cursor] ?: 0L) > 0L) {
            currentStreak++
            cursor -= dayMs
        }

        var longestStreak = 0
        var run = 0
        gridDays.forEach { day ->
            if (day.listenedMs > 0L) {
                run++
                if (run > longestStreak) longestStreak = run
            } else {
                run = 0
            }
        }

        ListeningStats(
            totalSongs = songs.size,
            totalListeningMs = totalListened,
            topSongs = topSongs,
            topArtists = topArtists,
            topAlbums = topAlbums,
            topGenres = topGenres,
            history = entries.take(100),
            dailyMinutes = daily,
            heatmapWeeks = heatWeeks,
            heatmapMaxMs = heatMax,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            activeDays = activeDays,
            averageDailyMs = averageDaily,
            bestDay = bestDay
        )
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5_000),
        ListeningStats(0, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    )

    /** Year-in-review data built from this year's listening history. */
    val wrapped: StateFlow<WrappedData?> = combine(
        musicRepository.songs,
        historyDao.observeSince(yearStartMillis()),
        settingsRepository.settings
    ) { songs, history, _ ->
        if (history.isEmpty()) {
            null
        } else {
            val songById = songs.associateBy { it.id }
            val played = history.mapNotNull { songById[it.songId] }
            val playsBySong = history.groupBy { it.songId }.mapValues { it.value.size }
            val topSongPair = playsBySong.maxByOrNull { it.value }
            val topSong = topSongPair?.let { songById[it.key] }
            val playsByArtist = played.groupBy { it.displayArtist }.mapValues { it.value.size }
            val topArtistPair = playsByArtist.maxByOrNull { it.value }
            val topAlbum = played.groupBy { it.displayAlbum }.maxByOrNull { it.value.size }?.key
            val topGenre = played.groupBy { it.genre }.maxByOrNull { it.value.size }?.key
            val topSongs = playsBySong.entries.sortedByDescending { it.value }.take(5)
                .mapNotNull { (id, count) -> songById[id]?.let { it to count } }
            val topArtists = playsByArtist.entries.sortedByDescending { it.value }.take(5)
                .map { it.key to it.value }
            WrappedData(
                year = Calendar.getInstance().get(Calendar.YEAR),
                totalMs = history.sumOf { it.playedMs },
                totalPlays = history.size,
                topSong = topSong,
                topSongPlays = topSongPair?.value ?: 0,
                topArtist = topArtistPair?.key,
                topArtistPlays = topArtistPair?.value ?: 0,
                topAlbum = topAlbum,
                topGenre = topGenre,
                topSongs = topSongs,
                topArtists = topArtists
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    /** Top tracks this week, each with a trend arrow versus last week. */
    val trending: StateFlow<List<TrendingSong>> = combine(
        musicRepository.songs,
        historyDao.observeRecent(1500)
    ) { songs, history ->
        val weekMs = 7L * 86_400_000L
        val now = System.currentTimeMillis()
        val thisWeek = history.filter { it.playedAt >= now - weekMs }
        val lastWeek = history.filter { it.playedAt in (now - 2 * weekMs) until (now - weekMs) }
        val thisPlays = thisWeek.groupBy { it.songId }.mapValues { it.value.size }
        val lastPlays = lastWeek.groupBy { it.songId }.mapValues { it.value.size }
        val songById = songs.associateBy { it.id }
        thisPlays.entries.sortedByDescending { it.value }.take(10).map { (id, count) ->
            val prev = lastPlays[id] ?: 0
            TrendingSong(
                song = songById[id],
                plays = count,
                previousPlays = prev,
                trend = when {
                    prev == 0 -> Trend.NEW
                    count > prev -> Trend.UP
                    count < prev -> Trend.DOWN
                    else -> Trend.SAME
                }
            )
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Midnight (device timezone) of the day containing [ms]. */
    private fun localDayStart(ms: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun yearStartMillis(): Long = Calendar.getInstance().apply {
        set(Calendar.MONTH, 0)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun clearHistory() = historyDao.clear()
}
