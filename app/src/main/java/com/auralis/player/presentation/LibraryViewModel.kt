package com.auralis.player.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.FolderSummary
import com.auralis.player.domain.model.Genre
import com.auralis.player.domain.model.GridStyle
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.MoodBucket
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.SONGS,
    val sort: SortOrder = SortOrder.TITLE_ASC,
    val grid: GridStyle = GridStyle.LIST,
    val filter: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albumArtists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val moods: List<MoodBucket> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favorites: List<Song> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.SONGS)
    private val filter = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOrder.TITLE_ASC)
    private val grid = MutableStateFlow(GridStyle.LIST)

    private val _initialised = MutableStateFlow(false)
    val initialised: StateFlow<Boolean> = _initialised.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (!_initialised.value) {
                    tab.value = settings.defaultTab
                    sort.value = settings.songSort
                    grid.value = settings.gridStyle
                    _initialised.value = true
                }
            }
        }
    }

    val state: StateFlow<LibraryUiState> = combine(
        combine(tab, filter, sort, grid) { t, f, s, g -> listOf(t, f, s, g) },
        combine(
            musicRepository.songs,
            musicRepository.albums,
            musicRepository.artists,
            musicRepository.albumArtists
        ) { songs, albums, artists, albumArtists -> listOf(songs, albums, artists, albumArtists) },
        combine(
            musicRepository.genres,
            musicRepository.folders,
            musicRepository.moods,
            musicRepository.favorites
        ) { genres, folders, moods, favorites -> listOf(genres, folders, moods, favorites) },
        playlistRepository.playlists
    ) { controls, primary, secondary, playlists ->
        @Suppress("UNCHECKED_CAST")
        val currentTab = controls[0] as LibraryTab
        val currentFilter = (controls[1] as String).trim()
        val currentSort = controls[2] as SortOrder
        val currentGrid = controls[3] as GridStyle

        @Suppress("UNCHECKED_CAST")
        val songs = primary[0] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val albums = primary[1] as List<Album>
        @Suppress("UNCHECKED_CAST")
        val artists = primary[2] as List<Artist>
        @Suppress("UNCHECKED_CAST")
        val albumArtists = primary[3] as List<Artist>
        @Suppress("UNCHECKED_CAST")
        val genres = secondary[0] as List<Genre>
        @Suppress("UNCHECKED_CAST")
        val folders = secondary[1] as List<FolderSummary>
        @Suppress("UNCHECKED_CAST")
        val moods = secondary[2] as List<MoodBucket>
        @Suppress("UNCHECKED_CAST")
        val favorites = secondary[3] as List<Song>

        fun matches(text: String) = currentFilter.isEmpty() ||
            text.contains(currentFilter, ignoreCase = true)

        LibraryUiState(
            tab = currentTab,
            sort = currentSort,
            grid = currentGrid,
            filter = currentFilter,
            songs = MusicRepository.sort(
                songs.filter { matches(it.title) || matches(it.displayArtist) || matches(it.displayAlbum) },
                currentSort
            ),
            albums = albums.filter { matches(it.name) || matches(it.artist) },
            artists = artists.filter { matches(it.name) },
            albumArtists = albumArtists.filter { matches(it.name) },
            genres = genres.filter { matches(it.name) },
            folders = folders.filter { matches(it.name) || matches(it.path) },
            moods = moods.filter { matches(it.name) },
            playlists = playlists.filter { matches(it.name) },
            favorites = MusicRepository.sort(favorites.filter { matches(it.title) }, currentSort)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun selectTab(value: LibraryTab) {
        tab.value = value
    }

    fun setFilter(value: String) {
        filter.value = value
    }

    fun setSort(value: SortOrder) {
        sort.value = value
        viewModelScope.launch { settingsRepository.setSongSort(value) }
    }

    fun toggleGrid() {
        val next = if (grid.value == GridStyle.LIST) GridStyle.GRID else GridStyle.LIST
        grid.value = next
        viewModelScope.launch { settingsRepository.setGridStyle(next) }
    }

    fun songsOfAlbum(albumId: Long) = musicRepository.songsOfAlbum(albumId)
    fun songsOfArtist(name: String) = musicRepository.songsOfArtist(name)
    fun songsOfGenre(name: String) = musicRepository.songsOfGenre(name)
    fun songsOfMood(name: String) = musicRepository.songsOfMood(name)
    fun songsOfFolder(path: String) = musicRepository.songsOfFolder(path)

    val recentlyAdded: StateFlow<List<Song>> = musicRepository.recentlyAdded
    val recentlyPlayed: StateFlow<List<Song>> = musicRepository.recentlyPlayed
    val mostPlayed: StateFlow<List<Song>> = musicRepository.mostPlayed
    val neverPlayed: StateFlow<List<Song>> = musicRepository.neverPlayed
    val favorites: StateFlow<List<Song>> = musicRepository.favorites
    val allSongs: StateFlow<List<Song>> = musicRepository.songs
    val forgotten: StateFlow<List<Song>> = musicRepository.forgotten
    val excludedFolders = musicRepository.excludedFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun excludeFolder(path: String) {
        viewModelScope.launch { musicRepository.addExcludedFolder(path) }
    }

    fun includeFolder(path: String) {
        viewModelScope.launch { musicRepository.removeExcludedFolder(path) }
    }

    val counts: StateFlow<Map<LibraryTab, Int>> = combine(
        musicRepository.songs,
        musicRepository.albums,
        musicRepository.artists,
        musicRepository.folders,
        playlistRepository.playlists
    ) { songs, albums, artists, folders, playlists ->
        mapOf(
            LibraryTab.SONGS to songs.size,
            LibraryTab.ALBUMS to albums.size,
            LibraryTab.ARTISTS to artists.size,
            LibraryTab.FOLDERS to folders.size,
            LibraryTab.PLAYLISTS to playlists.size,
            LibraryTab.FAVORITES to songs.count { it.isFavorite }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val genresFlow: StateFlow<List<Genre>> = musicRepository.genres
    val moodsFlow: StateFlow<List<MoodBucket>> = musicRepository.moods

    val greetingSongs: StateFlow<List<Song>> = musicRepository.songs
        .map { it.take(24) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
