package com.auralis.player.ui.search

import androidx.lifecycle.ViewModel
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.SearchResults
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.Song
import com.auralis.player.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class SearchTab { SONGS, ALBUMS, ARTISTS }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackController: PlaybackController
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results

    private val _selectedTab = MutableStateFlow(SearchTab.SONGS)
    val selectedTab: StateFlow<SearchTab> = _selectedTab

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank()) {
            _results.value = SearchResults()
        } else {
            _results.value = musicRepository.search(newQuery)
        }
    }

    fun onSearch(query: String) {
        _query.value = query
        if (query.isBlank()) return
        _results.value = musicRepository.search(query)
        addRecentSearch(query)
    }

    fun selectTab(tab: SearchTab) { _selectedTab.value = tab }

    fun clearSearch() {
        _query.value = ""
        _results.value = SearchResults()
    }

    fun removeRecentSearch(query: String) {
        _recentSearches.value = _recentSearches.value.filter { it != query }
    }

    private fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _recentSearches.value = listOf(trimmed) +
            _recentSearches.value.filter { !it.equals(trimmed, ignoreCase = true) }
    }

    fun playSong(song: Song) = playbackController.playNow(song)

    fun playAlbum(album: Album) {
        val songs = musicRepository.songsOfAlbum(album.id)
        if (songs.isNotEmpty()) playbackController.playQueue(songs, 0)
    }

    fun playArtist(artist: Artist) {
        val songs = musicRepository.songsOfArtist(artist.name)
        if (songs.isNotEmpty()) playbackController.playQueue(songs, 0)
    }
}
