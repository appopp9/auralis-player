package com.auralis.player.ui

import android.os.Environment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.Song
import com.auralis.player.presentation.LibraryViewModel
import com.auralis.player.presentation.MainViewModel
import com.auralis.player.presentation.PlaylistsViewModel
import com.auralis.player.presentation.SearchViewModel
import com.auralis.player.presentation.SmartPlaylist
import com.auralis.player.presentation.StatsViewModel
import com.auralis.player.ui.navigation.Routes
import com.auralis.player.ui.screens.favorites.FavoritesScreen
import com.auralis.player.ui.screens.folders.FolderBrowserScreen
import com.auralis.player.ui.screens.home.HomeCallbacks
import com.auralis.player.ui.screens.home.HomeScreen
import com.auralis.player.ui.screens.library.LibraryCallbacks
import com.auralis.player.ui.screens.library.LibraryScreen
import com.auralis.player.ui.screens.library.SongListScreen
import com.auralis.player.ui.screens.playlists.PlaylistsScreen
import com.auralis.player.ui.screens.search.SearchScreen
import com.auralis.player.ui.screens.settings.SettingsScreen
import com.auralis.player.ui.screens.stats.StatsScreen
import com.auralis.player.ui.screens.tageditor.TagEditorScreen
import kotlinx.coroutines.flow.map

@Composable
fun AuralisNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    libraryViewModel: LibraryViewModel,
    playlistsViewModel: PlaylistsViewModel,
    searchViewModel: SearchViewModel,
    statsViewModel: StatsViewModel,
    contentPadding: PaddingValues,
    onSongMenu: (Song) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onCreatePlaylistWith: (List<Long>) -> Unit
) {
    val playerState by mainViewModel.playerState.collectAsStateWithLifecycle()
    val currentSongId = playerState.currentSong?.id ?: -1L

    val play: (List<Song>, Int) -> Unit = { songs, index ->
        mainViewModel.playAll(songs, index)
        onOpenNowPlaying()
    }
    val shuffle: (List<Song>) -> Unit = { songs ->
        mainViewModel.shuffleAll(songs)
        onOpenNowPlaying()
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
            val recentlyAdded by libraryViewModel.recentlyAdded.collectAsStateWithLifecycle()
            val mostPlayed by libraryViewModel.mostPlayed.collectAsStateWithLifecycle()
            val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
            val allSongs by libraryViewModel.allSongs.collectAsStateWithLifecycle()
            val scan by mainViewModel.scanProgress.collectAsStateWithLifecycle()

            HomeScreen(
                recentlyPlayed = recentlyPlayed,
                recentlyAdded = recentlyAdded,
                mostPlayed = mostPlayed,
                favorites = favorites,
                totalSongs = allSongs.size,
                currentSongId = currentSongId,
                scanning = scan.running,
                contentPadding = contentPadding,
                callbacks = HomeCallbacks(
                    onPlaySong = play,
                    onSongMenu = onSongMenu,
                    onShuffleAll = { shuffle(allSongs) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onQuickAccess = { key ->
                        val tab = when (key) {
                            "songs" -> LibraryTab.SONGS
                            "artists" -> LibraryTab.ARTISTS
                            "albums" -> LibraryTab.ALBUMS
                            "folders" -> LibraryTab.FOLDERS
                            "playlists" -> LibraryTab.PLAYLISTS
                            "genres" -> LibraryTab.GENRES
                            "moods" -> LibraryTab.MOODS
                            else -> LibraryTab.SONGS
                        }
                        libraryViewModel.selectTab(tab)
                        navController.navigate(Routes.LIBRARY)
                    },
                    onSeeAll = { key ->
                        when (key) {
                            "recently_played" -> navController.navigate(Routes.smart(SmartPlaylist.RECENTLY_PLAYED.name))
                            "recently_added" -> navController.navigate(Routes.smart(SmartPlaylist.RECENTLY_ADDED.name))
                            "most_played" -> navController.navigate(Routes.smart(SmartPlaylist.MOST_PLAYED.name))
                            else -> navController.navigate(Routes.FAVORITES)
                        }
                    }
                )
            )
        }

        composable(Routes.LIBRARY) {
            val state by libraryViewModel.state.collectAsStateWithLifecycle()
            LibraryScreen(
                state = state,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                callbacks = LibraryCallbacks(
                    onSelectTab = { libraryViewModel.selectTab(it) },
                    onSetSort = { libraryViewModel.setSort(it) },
                    onToggleGrid = { libraryViewModel.toggleGrid() },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onPlaySongs = play,
                    onSongMenu = onSongMenu,
                    onOpenAlbum = { navController.navigate(Routes.album(it)) },
                    onOpenArtist = { navController.navigate(Routes.artist(it)) },
                    onOpenGenre = { navController.navigate(Routes.genre(it)) },
                    onOpenMood = { navController.navigate(Routes.mood(it)) },
                    onOpenFolder = { navController.navigate(Routes.folder(it)) },
                    onOpenPlaylist = { navController.navigate(Routes.playlist(it)) }
                )
            )
        }

        composable(Routes.PLAYLISTS) {
            val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
            val smartCounts by playlistsViewModel.smartCounts.collectAsStateWithLifecycle()
            PlaylistsScreen(
                playlists = playlists,
                smartCounts = smartCounts,
                contentPadding = contentPadding,
                onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                onOpenSmart = { navController.navigate(Routes.smart(it.name)) },
                onCreate = { playlistsViewModel.create(it) },
                onRename = { id, name -> playlistsViewModel.rename(id, name) },
                onDelete = { playlistsViewModel.delete(it) }
            )
        }

        composable(Routes.FAVORITES) {
            val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
            FavoritesScreen(
                favorites = favorites,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                onPlay = play,
                onShuffle = shuffle,
                onSongMenu = onSongMenu,
                onBrowseLibrary = { navController.navigate(Routes.LIBRARY) }
            )
        }

        composable(Routes.SETTINGS) {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val scan by mainViewModel.scanProgress.collectAsStateWithLifecycle()
            val excluded by libraryViewModel.excludedFolders.collectAsStateWithLifecycle()
            val allSongs by libraryViewModel.allSongs.collectAsStateWithLifecycle()

            SettingsScreen(
                settings = settings,
                scan = scan,
                excludedFolders = excluded,
                totalSongs = allSongs.size,
                contentPadding = contentPadding,
                onUpdate = { updated -> mainViewModel.applySettings(settings, updated) },
                onRescan = { mainViewModel.rescan() },
                onIncludeFolder = { libraryViewModel.includeFolder(it) },
                onOpenFolderBrowser = { navController.navigate(Routes.FOLDER_BROWSER) },
                onOpenStats = { navController.navigate(Routes.STATS) }
            )
        }

        composable(Routes.SEARCH) {
            val state by searchViewModel.state.collectAsStateWithLifecycle()
            val liveQuery by searchViewModel.query.collectAsStateWithLifecycle()
            val suggestions by searchViewModel.suggestions.collectAsStateWithLifecycle()
            SearchScreen(
                state = state,
                query = liveQuery,
                suggestions = suggestions,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                onQueryChange = { searchViewModel.setQuery(it) },
                onBack = { navController.popBackStack() },
                onPlaySongs = play,
                onSongMenu = onSongMenu,
                onOpenAlbum = { navController.navigate(Routes.album(it)) },
                onOpenArtist = { navController.navigate(Routes.artist(it)) },
                onOpenGenre = { navController.navigate(Routes.genre(it)) },
                onOpenFolder = { navController.navigate(Routes.folder(it)) },
                onOpenPlaylist = { navController.navigate(Routes.playlist(it)) }
            )
        }

        composable(Routes.STATS) {
            val stats by statsViewModel.stats.collectAsStateWithLifecycle()
            StatsScreen(
                stats = stats,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onClearHistory = { statsViewModel.clearHistory() }
            )
        }

        composable(Routes.FOLDER_BROWSER) {
            val excluded by libraryViewModel.excludedFolders.collectAsStateWithLifecycle()
            val roots = remember {
                buildList {
                    add("Internal" to Environment.getExternalStorageDirectory().absolutePath)
                    add("Music" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath)
                    add("Downloads" to Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath)
                }
            }
            FolderBrowserScreen(
                roots = roots,
                excludedFolders = excluded,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onPlayFolder = { path ->
                    val songs = libraryViewModel.songsOfFolder(path)
                    if (songs.isEmpty()) mainViewModel.notify("No indexed tracks in this folder") else play(songs, 0)
                },
                onExcludeFolder = {
                    libraryViewModel.excludeFolder(it)
                    mainViewModel.notify("Folder excluded from the library")
                },
                onIncludeFolder = { libraryViewModel.includeFolder(it) },
                onOpenLibraryFolder = { navController.navigate(Routes.folder(it)) },
                onMessage = { mainViewModel.notify(it) }
            )
        }

        composable(
            route = Routes.ALBUM,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { entry ->
            val albumId = entry.arguments?.getLong("albumId") ?: 0L
            val songs = libraryViewModel.songsOfAlbum(albumId)
            DetailScreen(
                title = songs.firstOrNull()?.displayAlbum ?: "Album",
                subtitle = songs.firstOrNull()?.displayArtist.orEmpty(),
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.ARTIST,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = Routes.decode(entry.arguments?.getString("name").orEmpty())
            val songs = libraryViewModel.songsOfArtist(name)
            DetailScreen(
                title = name,
                subtitle = "Artist",
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.GENRE,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = Routes.decode(entry.arguments?.getString("name").orEmpty())
            val songs = libraryViewModel.songsOfGenre(name)
            DetailScreen(
                title = name,
                subtitle = "Genre",
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.MOOD,
            arguments = listOf(navArgument("name") { type = NavType.StringType })
        ) { entry ->
            val name = Routes.decode(entry.arguments?.getString("name").orEmpty())
            val songs = libraryViewModel.songsOfMood(name)
            DetailScreen(
                title = name,
                subtitle = "Mood",
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.FOLDER,
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { entry ->
            val path = Routes.decode(entry.arguments?.getString("path").orEmpty())
            val songs = libraryViewModel.songsOfFolder(path)
            DetailScreen(
                title = path.substringAfterLast('/').ifBlank { path },
                subtitle = path,
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.SMART_PLAYLIST,
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) { entry ->
            val kind = entry.arguments?.getString("kind").orEmpty()
            val smart = runCatching { SmartPlaylist.valueOf(kind) }.getOrDefault(SmartPlaylist.RECENTLY_ADDED)
            val songs = playlistsViewModel.smartSongs(smart)
            DetailScreen(
                title = smart.title,
                subtitle = "Smart playlist",
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                navController = navController,
                play = play,
                shuffle = shuffle,
                onSongMenu = onSongMenu,
                onCreatePlaylistWith = onCreatePlaylistWith
            )
        }

        composable(
            route = Routes.PLAYLIST,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
            val songs by playlistsViewModel.songsOf(playlistId)
                .collectAsStateWithLifecycle(initialValue = emptyList())
            val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()
            val name = playlists.firstOrNull { it.id == playlistId }?.name ?: "Playlist"

            SongListScreen(
                title = name,
                subtitle = "Playlist",
                songs = songs,
                currentSongId = currentSongId,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onPlay = play,
                onShuffle = shuffle,
                onSongMenu = onSongMenu,
                onRemoveSong = { playlistsViewModel.removeSong(playlistId, it.id) }
            )
        }

        composable(
            route = Routes.TAG_EDITOR,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { entry ->
            val songId = entry.arguments?.getLong("songId") ?: 0L
            val allSongs by libraryViewModel.allSongs.collectAsStateWithLifecycle()
            TagEditorScreen(
                song = allSongs.firstOrNull { it.id == songId },
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onSave = { song, update ->
                    mainViewModel.saveTags(song, update) { done ->
                        if (done) navController.popBackStack()
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long,
    contentPadding: PaddingValues,
    navController: NavHostController,
    play: (List<Song>, Int) -> Unit,
    shuffle: (List<Song>) -> Unit,
    onSongMenu: (Song) -> Unit,
    onCreatePlaylistWith: (List<Long>) -> Unit
) {
    SongListScreen(
        title = title,
        subtitle = subtitle,
        songs = songs,
        currentSongId = currentSongId,
        contentPadding = contentPadding,
        onBack = { navController.popBackStack() },
        onPlay = play,
        onShuffle = shuffle,
        onSongMenu = onSongMenu,
        extraAction = "Save as playlist" to { onCreatePlaylistWith(songs.map { it.id }) }
    )
}
