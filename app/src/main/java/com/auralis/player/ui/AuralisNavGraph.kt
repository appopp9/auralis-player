package com.auralis.player.ui

import android.os.Environment
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.auralis.player.presentation.QuickCollection
import com.auralis.player.presentation.SmartPlaylistsViewModel
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
import com.auralis.player.ui.screens.playlists.SmartPlaylistEditorScreen
import com.auralis.player.ui.screens.search.SearchScreen
import com.auralis.player.ui.screens.settings.SettingsScreen
import com.auralis.player.ui.screens.stats.StatsScreen
import com.auralis.player.ui.screens.stats.WrappedScreen
import com.auralis.player.ui.screens.tageditor.TagEditorScreen
import com.auralis.player.ui.theme.AuralisMotion
import kotlinx.coroutines.flow.map

@Composable
fun AuralisNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    libraryViewModel: LibraryViewModel,
    playlistsViewModel: PlaylistsViewModel,
    searchViewModel: SearchViewModel,
    statsViewModel: StatsViewModel,
    startRoute: String = Routes.HOME,
    contentPadding: PaddingValues,
    onSongMenu: (Song) -> Unit,
    onOpenNowPlaying: () -> Unit,
    onCreatePlaylistWith: (List<Long>) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
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

    // Fast shared-axis transitions: a short rise + fade in ~260 ms instead of
    // navigation-compose's sleepy 700 ms default cross-fade. Exit is quicker
    // than enter so the new screen always feels like it is chasing the tap.
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            fadeIn(tween(240, easing = AuralisMotion.EmphasizedEasing)) +
                slideInVertically(tween(300, easing = AuralisMotion.EmphasizedEasing)) { it / 14 } +
                scaleIn(tween(300, easing = AuralisMotion.EmphasizedEasing), initialScale = 0.985f)
        },
        exitTransition = {
            fadeOut(tween(110)) +
                scaleOut(tween(220, easing = AuralisMotion.ExitEasing), targetScale = 0.985f)
        },
        popEnterTransition = {
            fadeIn(tween(220, easing = AuralisMotion.EmphasizedEasing)) +
                scaleIn(tween(260, easing = AuralisMotion.EmphasizedEasing), initialScale = 1.01f)
        },
        popExitTransition = {
            fadeOut(tween(130)) +
                slideOutVertically(tween(240, easing = AuralisMotion.ExitEasing)) { it / 18 } +
                scaleOut(tween(240, easing = AuralisMotion.ExitEasing), targetScale = 0.98f)
        }
    ) {

        composable(Routes.HOME) {
            val recentlyPlayed by libraryViewModel.recentlyPlayed.collectAsStateWithLifecycle()
            val recentlyAdded by libraryViewModel.recentlyAdded.collectAsStateWithLifecycle()
            val mostPlayed by libraryViewModel.mostPlayed.collectAsStateWithLifecycle()
            val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
            val forgotten by libraryViewModel.forgotten.collectAsStateWithLifecycle()
            val allSongs by libraryViewModel.allSongs.collectAsStateWithLifecycle()
            val scan by mainViewModel.scanProgress.collectAsStateWithLifecycle()

            HomeScreen(
                recentlyPlayed = recentlyPlayed,
                recentlyAdded = recentlyAdded,
                mostPlayed = mostPlayed,
                favorites = favorites,
                forgotten = forgotten,
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
                            "recently_played" -> navController.navigate(Routes.smart(QuickCollection.RECENTLY_PLAYED.name))
                            "recently_added" -> navController.navigate(Routes.smart(QuickCollection.RECENTLY_ADDED.name))
                            "most_played" -> navController.navigate(Routes.smart(QuickCollection.MOST_PLAYED.name))
                            else -> navController.navigate(Routes.FAVORITES)
                        }
                    }
                )
            )
        }

        composable(Routes.LIBRARY) {
            val state by libraryViewModel.state.collectAsStateWithLifecycle()
            val pinnedIds by remember {
                mainViewModel.settings.map { it.pinnedSongs }
            }.collectAsStateWithLifecycle(initialValue = emptySet())
            LibraryScreen(
                state = state,
                currentSongId = currentSongId,
                pinnedIds = pinnedIds,
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
            val smartViewModel: SmartPlaylistsViewModel = hiltViewModel()
            val smartEntries by smartViewModel.playlists.collectAsStateWithLifecycle()
            PlaylistsScreen(
                playlists = playlists,
                smartCounts = smartCounts,
                smartPlaylists = smartEntries.map { it.playlist to it.songs.size },
                contentPadding = contentPadding,
                onOpenPlaylist = { navController.navigate(Routes.playlist(it)) },
                onOpenSmart = { navController.navigate(Routes.smart(it.name)) },
                onOpenSmartPlaylist = { navController.navigate(Routes.smartCustom(it)) },
                onEditSmartPlaylist = { navController.navigate(Routes.smartEditor(it)) },
                onDeleteSmartPlaylist = { smartViewModel.delete(it) },
                onCreateSmartPlaylist = { navController.navigate(Routes.smartEditor(0L)) },
                onCreate = { playlistsViewModel.create(it) },
                onRename = { id, name -> playlistsViewModel.rename(id, name) },
                onDelete = { playlistsViewModel.delete(it) },
                onTogglePin = { playlistsViewModel.togglePinned(it) }
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
                onOpenStats = { navController.navigate(Routes.STATS) },
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup
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
            val trending by statsViewModel.trending.collectAsStateWithLifecycle()
            val duplicates by statsViewModel.duplicates.collectAsStateWithLifecycle()
            StatsScreen(
                stats = stats,
                trending = trending,
                duplicates = duplicates,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onClearHistory = { statsViewModel.clearHistory() },
                onPlaySongs = { songs, index -> play(songs, index) },
                onPlaySongId = { songId ->
                    libraryViewModel.allSongs.value.firstOrNull { it.id == songId }?.let { song ->
                        play(listOf(song), 0)
                    }
                },
                onOpenWrapped = { navController.navigate(Routes.WRAPPED) }
            )
        }

        composable(Routes.WRAPPED) {
            val wrapped by statsViewModel.wrapped.collectAsStateWithLifecycle()
            WrappedScreen(
                wrapped = wrapped,
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() }
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
            val smart = runCatching { QuickCollection.valueOf(kind) }.getOrDefault(QuickCollection.RECENTLY_ADDED)
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
            route = Routes.SMART_CUSTOM,
            arguments = listOf(navArgument("smartId") { type = NavType.LongType })
        ) { entry ->
            val smartId = entry.arguments?.getLong("smartId") ?: 0L
            val smartViewModel: SmartPlaylistsViewModel = hiltViewModel()
            val entries by smartViewModel.playlists.collectAsStateWithLifecycle()
            val match = entries.firstOrNull { it.playlist.id == smartId }
            DetailScreen(
                title = match?.playlist?.name ?: "Smart playlist",
                subtitle = "Smart playlist • updates itself",
                songs = match?.songs.orEmpty(),
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
            route = Routes.SMART_EDITOR,
            arguments = listOf(navArgument("smartId") { type = NavType.LongType })
        ) { entry ->
            val smartId = entry.arguments?.getLong("smartId") ?: 0L
            val smartViewModel: SmartPlaylistsViewModel = hiltViewModel()
            val entries by smartViewModel.playlists.collectAsStateWithLifecycle()
            val existing = entries.firstOrNull { it.playlist.id == smartId }?.playlist
            SmartPlaylistEditorScreen(
                initial = existing ?: com.auralis.player.domain.model.SmartPlaylist(),
                matchCount = { draft -> smartViewModel.preview(draft).size },
                contentPadding = contentPadding,
                onBack = { navController.popBackStack() },
                onSave = {
                    smartViewModel.save(it)
                    navController.popBackStack()
                },
                onDelete = {
                    smartViewModel.delete(it)
                    navController.popBackStack()
                }
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
                onRemoveSong = { playlistsViewModel.removeSong(playlistId, it.id) },
                onReorder = { ids -> playlistsViewModel.reorder(playlistId, ids) }
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
