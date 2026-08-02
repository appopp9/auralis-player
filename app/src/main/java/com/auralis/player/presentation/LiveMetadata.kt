package com.auralis.player.presentation

import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlayerUiState

/**
 * Overlays live library data on top of the playback snapshot.
 *
 * The media session only knows the track it was handed, so favourite state and
 * edited tags would otherwise stay stale until the next track change.
 */
fun PlayerUiState.withLiveLibraryData(songs: List<Song>): PlayerUiState {
    val current = currentSong ?: return this
    val fresh = songs.firstOrNull { it.id == current.id } ?: return this
    return if (fresh == current) this else copy(currentSong = fresh)
}
