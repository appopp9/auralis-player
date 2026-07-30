package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.data.repository.SearchResults
import com.auralis.player.core.Fuzzy
import com.auralis.player.domain.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults(),
    val playlists: List<Playlist> = emptyList()
) {
    val isEmpty: Boolean get() = results.isEmpty && playlists.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Results are debounced, but the text the user typed is NEVER debounced.
     * The text field is driven by [query] directly, so keystrokes can no longer
     * arrive out of order (which used to make "yas" render as "asy").
     */
    val state: StateFlow<SearchUiState> = combine(
        _query.debounce { if (it.isBlank()) 0L else 140L },
        musicRepository.songs,
        playlistRepository.playlists
    ) { query, _, playlists ->
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            SearchUiState(query = query)
        } else {
            SearchUiState(
                query = query,
                results = musicRepository.search(trimmed),
                playlists = playlists.filter { Fuzzy.matches(it.name, trimmed) }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    val suggestions: StateFlow<List<String>> = musicRepository.mostPlayed
        .map { songs -> songs.take(6).map { it.displayArtist }.distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        _query.value = ""
    }
}
