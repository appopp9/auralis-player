package com.auralis.player.ui.albums

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val albums: StateFlow<List<Album>> = musicRepository.albums

    fun songsOfAlbum(albumId: Long): List<Song> = musicRepository.songsOfAlbum(albumId)

    fun playSong(song: Song) = playbackController.playNow(song)

    fun playAlbum(album: Album) {
        val songs = musicRepository.songsOfAlbum(album.id)
        if (songs.isNotEmpty()) {
            playbackController.playQueue(songs, 0)
        }
    }

    fun shuffleAlbum(album: Album) {
        val songs = musicRepository.songsOfAlbum(album.id).shuffled()
        if (songs.isNotEmpty()) {
            playbackController.playQueue(songs, 0)
        }
    }
}
