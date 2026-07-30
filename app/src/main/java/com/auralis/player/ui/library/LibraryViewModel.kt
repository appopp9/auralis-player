package com.auralis.player.ui.library

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val songs: StateFlow<List<Song>> = musicRepository.songs

    val artists: StateFlow<List<Artist>> = musicRepository.artists

    val albums: StateFlow<List<Album>> = musicRepository.albums

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    fun play(song: Song) = playbackController.playNow(song)

    fun shuffleAll() {
        val all = songs.value.shuffled()
        if (all.isNotEmpty()) playbackController.playQueue(all, 0)
    }
}
