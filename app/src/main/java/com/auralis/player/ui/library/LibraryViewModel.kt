package com.auralis.player.ui.library

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val songs: StateFlow<List<Song>> = musicRepository.songs

    fun play(song: Song) = playbackController.playNow(song)

    fun shuffleAll() {
        val all = songs.value.shuffled()
        if (all.isNotEmpty()) playbackController.playQueue(all, 0)
    }
}
