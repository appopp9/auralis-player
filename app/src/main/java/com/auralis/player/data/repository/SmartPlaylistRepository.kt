package com.auralis.player.data.repository

import com.auralis.player.core.TextNormalizer
import com.auralis.player.data.db.SmartPlaylistDao
import com.auralis.player.data.db.SmartPlaylistEntity
import com.auralis.player.domain.model.SmartField
import com.auralis.player.domain.model.SmartOperator
import com.auralis.player.domain.model.SmartPlaylist
import com.auralis.player.domain.model.SmartRule
import com.auralis.player.domain.model.SmartRuleCodec
import com.auralis.player.domain.model.SmartSort
import com.auralis.player.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Storage and evaluation for smart playlists.
 *
 * Matching runs in memory over the library snapshot the app already holds, so a
 * smart playlist stays correct the moment a song is played, favourited or
 * rescanned, without any cache to invalidate.
 */
@Singleton
class SmartPlaylistRepository @Inject constructor(
    private val dao: SmartPlaylistDao
) {

    val playlists: Flow<List<SmartPlaylist>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun byId(id: Long): SmartPlaylist? = dao.byId(id)?.toDomain()

    suspend fun save(playlist: SmartPlaylist): Long {
        val now = System.currentTimeMillis()
        return dao.upsert(
            SmartPlaylistEntity(
                id = playlist.id,
                name = playlist.name.trim().ifBlank { "Smart playlist" },
                icon = playlist.icon,
                rulesJson = SmartRuleCodec.encode(playlist.rules),
                matchAll = playlist.matchAll,
                limitCount = playlist.limit.coerceAtLeast(0),
                sortKey = playlist.sort.key,
                sortDescending = playlist.sortDescending,
                createdAt = if (playlist.createdAt == 0L) now else playlist.createdAt,
                updatedAt = now
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun SmartPlaylistEntity.toDomain() = SmartPlaylist(
        id = id,
        name = name,
        icon = icon,
        rules = SmartRuleCodec.decode(rulesJson),
        matchAll = matchAll,
        limit = limitCount,
        sort = SmartSort.from(sortKey),
        sortDescending = sortDescending,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {

        private const val DAY_MS = 86_400_000L

        /**
         * Resolves a smart playlist against [library].
         *
         * A playlist with no rules deliberately matches everything, so a brand
         * new one shows the whole library instead of looking broken while the
         * user is still adding conditions.
         */
        fun evaluate(
            playlist: SmartPlaylist,
            library: List<Song>,
            now: Long = System.currentTimeMillis()
        ): List<Song> {
            val rules = playlist.rules.filter { it.isUsable() }
            val matched = if (rules.isEmpty()) {
                library
            } else {
                library.filter { song ->
                    if (playlist.matchAll) {
                        rules.all { matches(song, it, now) }
                    } else {
                        rules.any { matches(song, it, now) }
                    }
                }
            }
            val sorted = sort(matched, playlist.sort, playlist.sortDescending)
            return if (playlist.limit > 0) sorted.take(playlist.limit) else sorted
        }

        /** Blank text or a non-numeric number would silently match nothing. */
        private fun SmartRule.isUsable(): Boolean = when {
            operator == SmartOperator.IS_TRUE || operator == SmartOperator.IS_FALSE -> true
            value.isBlank() -> false
            !field.isText && value.trim().toLongOrNull() == null -> false
            else -> true
        }

        private fun sort(songs: List<Song>, sort: SmartSort, descending: Boolean): List<Song> {
            if (sort == SmartSort.RANDOM) return songs.shuffled()
            val comparator: Comparator<Song> = when (sort) {
                SmartSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
                SmartSort.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
                SmartSort.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
                SmartSort.YEAR -> compareBy { it.year }
                SmartSort.DURATION -> compareBy { it.durationMs }
                SmartSort.PLAY_COUNT -> compareBy { it.playCount }
                SmartSort.LAST_PLAYED -> compareBy { it.lastPlayedAt }
                SmartSort.DATE_ADDED -> compareBy { it.dateAddedSec }
                SmartSort.RANDOM -> compareBy { it.id }
            }
            // Stable tie-break on id keeps the order from jittering between reads.
            val full = comparator.thenBy { it.id }
            return if (descending) songs.sortedWith(full.reversed()) else songs.sortedWith(full)
        }

        private fun matches(song: Song, rule: SmartRule, now: Long): Boolean = when {
            rule.field.isBoolean -> matchBoolean(song, rule)
            rule.field.isDate -> matchDate(song, rule, now)
            rule.field.isText -> matchText(song, rule)
            else -> matchNumber(song, rule)
        }

        private fun matchBoolean(song: Song, rule: SmartRule): Boolean {
            val actual = when (rule.field) {
                SmartField.FAVORITE -> song.isFavorite
                SmartField.HAS_LYRICS -> !song.lyrics.isNullOrBlank()
                else -> false
            }
            return if (rule.operator == SmartOperator.IS_FALSE) !actual else actual
        }

        private fun matchText(song: Song, rule: SmartRule): Boolean {
            val raw = when (rule.field) {
                SmartField.TITLE -> song.title
                SmartField.ARTIST -> song.artist
                SmartField.ALBUM -> song.album
                SmartField.ALBUM_ARTIST -> song.albumArtist
                SmartField.GENRE -> song.genre
                SmartField.COMPOSER -> song.composer
                SmartField.FOLDER -> song.folderName
                SmartField.MOOD -> song.mood
                else -> ""
            }
            // Normalised on both sides so Persian spelling variants of the same
            // artist name do not split a rule in half.
            val actual = TextNormalizer.normalize(raw)
            val expected = TextNormalizer.normalize(rule.value)
            return when (rule.operator) {
                SmartOperator.CONTAINS -> actual.contains(expected)
                SmartOperator.NOT_CONTAINS -> !actual.contains(expected)
                SmartOperator.EQUALS -> actual == expected
                SmartOperator.NOT_EQUALS -> actual != expected
                SmartOperator.STARTS_WITH -> actual.startsWith(expected)
                SmartOperator.ENDS_WITH -> actual.endsWith(expected)
                else -> false
            }
        }

        private fun matchNumber(song: Song, rule: SmartRule): Boolean {
            val actual = when (rule.field) {
                SmartField.YEAR -> song.year.toLong()
                // Compared in whole seconds so the user types "180", not "180000".
                SmartField.DURATION -> song.durationMs / 1000L
                SmartField.PLAY_COUNT -> song.playCount.toLong()
                else -> 0L
            }
            val a = rule.value.trim().toLongOrNull() ?: return false
            return when (rule.operator) {
                SmartOperator.EQUALS -> actual == a
                SmartOperator.NOT_EQUALS -> actual != a
                SmartOperator.GREATER -> actual > a
                SmartOperator.LESS -> actual < a
                SmartOperator.BETWEEN -> {
                    val b = rule.valueTo.trim().toLongOrNull() ?: return false
                    actual >= minOf(a, b) && actual <= maxOf(a, b)
                }
                else -> false
            }
        }

        private fun matchDate(song: Song, rule: SmartRule, now: Long): Boolean {
            val timestampMs = when (rule.field) {
                SmartField.LAST_PLAYED -> song.lastPlayedAt
                SmartField.DATE_ADDED -> normalizedAddedMs(song)
                else -> 0L
            }
            val days = rule.value.trim().toLongOrNull() ?: return false
            // Never played / unknown date can never be "in the last N days", but it
            // is legitimately "not in the last N days".
            if (timestampMs <= 0L) return rule.operator == SmartOperator.NOT_IN_LAST
            val within = now - timestampMs <= days * DAY_MS
            return if (rule.operator == SmartOperator.NOT_IN_LAST) !within else within
        }

        /**
         * MediaStore reports DATE_ADDED in seconds, but some vendors report
         * milliseconds. Same normalisation as the library sort.
         */
        private fun normalizedAddedMs(song: Song): Long {
            val raw = if (song.dateAddedSec > 0L) song.dateAddedSec else song.dateModifiedSec
            if (raw <= 0L) return 0L
            return if (raw > 100_000_000_000L) raw else raw * 1000L
        }
    }
}
