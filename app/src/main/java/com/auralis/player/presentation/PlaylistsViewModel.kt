package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class QuickCollection(val title: String) {
    RECENTLY_PLAYED("Recently played"),
    MOST_PLAYED("Most played"),
    RECENTLY_ADDED("Recently added"),
    FAVORITES("Favorites"),
    NEVER_PLAYED("Never played")
}

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    val smartCounts: StateFlow<Map<QuickCollection, Int>> = musicRepository.songs
        .let { flow ->
            kotlinx.coroutines.flow.combine(
                musicRepository.recentlyPlayed,
                musicRepository.mostPlayed,
                musicRepository.recentlyAdded,
                musicRepository.favorites,
                musicRepository.neverPlayed
            ) { recentlyPlayed, mostPlayed, recentlyAdded, favorites, neverPlayed ->
                mapOf(
                    QuickCollection.RECENTLY_PLAYED to recentlyPlayed.size,
                    QuickCollection.MOST_PLAYED to mostPlayed.size,
                    QuickCollection.RECENTLY_ADDED to recentlyAdded.size,
                    QuickCollection.FAVORITES to favorites.size,
                    QuickCollection.NEVER_PLAYED to neverPlayed.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun songsOf(playlistId: Long): Flow<List<Song>> = playlistRepository.songsOf(playlistId)

    fun smartSongs(playlist: QuickCollection): List<Song> = when (playlist) {
        QuickCollection.RECENTLY_PLAYED -> musicRepository.recentlyPlayed.value
        QuickCollection.MOST_PLAYED -> musicRepository.mostPlayed.value
        QuickCollection.RECENTLY_ADDED -> musicRepository.recentlyAdded.value
        QuickCollection.FAVORITES -> musicRepository.favorites.value
        QuickCollection.NEVER_PLAYED -> musicRepository.neverPlayed.value
    }

    fun create(name: String, songIds: List<Long> = emptyList()) {
        viewModelScope.launch { playlistRepository.create(name, songIds) }
    }

    fun createFromArtist(name: String) {
        viewModelScope.launch {
            playlistRepository.create(name, musicRepository.songsOfArtist(name).map { it.id })
        }
    }

    fun createFromAlbum(albumId: Long, name: String) {
        viewModelScope.launch {
            playlistRepository.create(name, musicRepository.songsOfAlbum(albumId).map { it.id })
        }
    }

    fun createFromGenre(name: String) {
        viewModelScope.launch {
            playlistRepository.create(name, musicRepository.songsOfGenre(name).map { it.id })
        }
    }

    fun createFromMood(name: String) {
        viewModelScope.launch {
            playlistRepository.create(name, musicRepository.songsOfMood(name).map { it.id })
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { playlistRepository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    fun togglePinned(id: Long) {
        viewModelScope.launch { playlistRepository.togglePinned(id) }
    }

    fun addSongs(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch { playlistRepository.addSongs(playlistId, songIds) }
    }

    fun removeSong(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSong(playlistId, songId) }
    }

    fun reorder(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch { playlistRepository.reorder(playlistId, orderedSongIds) }
    }
}
