package com.auralis.player.ui.screens.home

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    val allSongs: StateFlow<List<Song>> = musicRepository.songs

    val recentlyPlayed: StateFlow<List<Song>> = musicRepository.recentlyPlayed

    val popularSongs: StateFlow<List<Song>> = musicRepository.mostPlayed

    val newReleases: StateFlow<List<Song>> = musicRepository.recentlyAdded

    val player get() = playbackController.player

    fun play(song: Song) = playbackController.playNow(song)

    fun togglePlayPause() = playbackController.togglePlayPause()

    fun next() = playbackController.next()

    fun previous() = playbackController.previous()
}
