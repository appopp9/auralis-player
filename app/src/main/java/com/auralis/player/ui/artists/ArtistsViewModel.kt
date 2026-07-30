package com.auralis.player.ui.artists

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val artists: StateFlow<List<Artist>> = musicRepository.artists

    fun songsOfArtist(artistName: String): List<Song> = musicRepository.songsOfArtist(artistName)

    fun playSong(song: Song) = playbackController.playNow(song)

    fun playArtist(artist: Artist) {
        val songs = musicRepository.songsOfArtist(artist.name)
        if (songs.isNotEmpty()) {
            playbackController.playQueue(songs, 0)
        }
    }

    fun shuffleArtist(artist: Artist) {
        val songs = musicRepository.songsOfArtist(artist.name).shuffled()
        if (songs.isNotEmpty()) {
            playbackController.playQueue(songs, 0)
        }
    }
}
