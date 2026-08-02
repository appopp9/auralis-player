package com.auralis.player.data.repository

import com.auralis.player.data.db.PlaylistDao
import com.auralis.player.data.db.PlaylistEntity
import com.auralis.player.data.db.PlaylistSongEntity
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.di.ApplicationScope
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope
) {

    private val members: Flow<List<PlaylistSongEntity>> = playlistDao.observeAllMembers()

    val playlists: StateFlow<List<Playlist>> =
        combine(
            playlistDao.observePlaylists(),
            members,
            musicRepository.songs,
            settingsRepository.settings
        ) { lists, links, songs, settings ->
            val songById = songs.associateBy { it.id }
            val pinned = settings.pinnedPlaylists
            lists.map { entity ->
                val ids = links.filter { it.playlistId == entity.id }.sortedBy { it.position }
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    songCount = ids.count { songById.containsKey(it.songId) },
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    artworkSongId = ids.firstOrNull { songById.containsKey(it.songId) }?.songId ?: 0L,
                    pinned = entity.id in pinned
                )
            }
                // Pinned playlists always float to the top, then most recent.
                .sortedWith(compareByDescending<Playlist> { it.pinned }.thenByDescending { it.updatedAt })
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Toggle the pinned state of a playlist (persisted across restarts). */
    suspend fun togglePinned(playlistId: Long) = settingsRepository.togglePinnedPlaylist(playlistId)

    fun songsOf(playlistId: Long): Flow<List<Song>> =
        combine(members, musicRepository.songs) { links, songs ->
            val songById = songs.associateBy { it.id }
            links.filter { it.playlistId == playlistId }
                .sortedBy { it.position }
                .mapNotNull { songById[it.songId] }
        }

    suspend fun create(name: String, songIds: List<Long> = emptyList()): Long {
        val now = System.currentTimeMillis()
        val id = playlistDao.insertPlaylist(PlaylistEntity(name = name.trim(), createdAt = now, updatedAt = now))
        if (songIds.isNotEmpty()) addSongs(id, songIds)
        return id
    }

    suspend fun rename(id: Long, name: String) =
        playlistDao.rename(id, name.trim(), System.currentTimeMillis())

    suspend fun delete(id: Long) {
        playlistDao.clearMembers(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val existing = playlistDao.members(playlistId).map { it.songId }.toHashSet()
        var position = playlistDao.maxPosition(playlistId)
        val toAdd = songIds.filter { existing.add(it) }.map { songId ->
            position += 1
            PlaylistSongEntity(playlistId, songId, position, now)
        }
        if (toAdd.isNotEmpty()) {
            playlistDao.insertMembers(toAdd)
            playlistDao.touch(playlistId, now)
        }
    }

    suspend fun removeSong(playlistId: Long, songId: Long) {
        playlistDao.removeMember(playlistId, songId)
        playlistDao.touch(playlistId, System.currentTimeMillis())
    }

    suspend fun reorder(playlistId: Long, orderedSongIds: List<Long>) =
        playlistDao.reorder(playlistId, orderedSongIds, System.currentTimeMillis())

    suspend fun playlistName(id: Long): String = playlistDao.getPlaylist(id)?.name.orEmpty()
}
