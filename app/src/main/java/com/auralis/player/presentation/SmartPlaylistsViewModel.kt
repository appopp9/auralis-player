package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.data.repository.SmartPlaylistRepository
import com.auralis.player.domain.model.SmartPlaylist
import com.auralis.player.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A smart playlist together with the tracks it currently resolves to. */
data class SmartPlaylistWithSongs(
    val playlist: SmartPlaylist,
    val songs: List<Song>
)

@HiltViewModel
class SmartPlaylistsViewModel @Inject constructor(
    private val smartRepository: SmartPlaylistRepository,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    /**
     * Resolved against the live library snapshot, so a smart playlist updates
     * the instant a song is played, favourited or rescanned.
     */
    val playlists: StateFlow<List<SmartPlaylistWithSongs>> =
        combine(smartRepository.playlists, musicRepository.songs) { smart, library ->
            smart.map { playlist ->
                SmartPlaylistWithSongs(
                    playlist = playlist,
                    songs = SmartPlaylistRepository.evaluate(playlist, library)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun songsOf(id: Long): List<Song> =
        playlists.value.firstOrNull { it.playlist.id == id }?.songs.orEmpty()

    fun playlistOf(id: Long): SmartPlaylist? =
        playlists.value.firstOrNull { it.playlist.id == id }?.playlist

    /** Live preview while the user is still editing, before anything is saved. */
    fun preview(draft: SmartPlaylist): List<Song> =
        SmartPlaylistRepository.evaluate(draft, musicRepository.songs.value)

    fun save(playlist: SmartPlaylist) {
        viewModelScope.launch { smartRepository.save(playlist) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { smartRepository.delete(id) }
    }

    /** Freeze the current result set into a normal, editable playlist. */
    fun convertToRegular(id: Long) {
        val entry = playlists.value.firstOrNull { it.playlist.id == id } ?: return
        viewModelScope.launch {
            playlistRepository.create(entry.playlist.name, entry.songs.map { it.id })
        }
    }
}
