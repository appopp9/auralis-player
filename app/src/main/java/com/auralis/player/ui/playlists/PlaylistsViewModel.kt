package com.auralis.player.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    val favorites: StateFlow<List<Song>> = musicRepository.favorites

    val recentlyPlayed: StateFlow<List<Song>> = musicRepository.recentlyPlayed

    val mostPlayed: StateFlow<List<Song>> = musicRepository.mostPlayed

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    fun showCreateDialog() { _showCreateDialog.value = true }
    fun dismissCreateDialog() { _showCreateDialog.value = false }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.create(name)
            _showCreateDialog.value = false
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.songsOf(playlist.id).collect { songs ->
                if (songs.isNotEmpty()) {
                    playbackController.playQueue(songs, 0)
                }
            }
        }
    }

    fun playSong(song: Song) = playbackController.playNow(song)

    fun playSongList(songs: List<Song>) {
        if (songs.isNotEmpty()) playbackController.playQueue(songs, 0)
    }

    fun songsOfPlaylist(playlistId: Long): StateFlow<List<Song>> =
        playlistRepository.songsOf(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
