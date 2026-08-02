package com.auralis.player.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.auralis.player.ui.theme.AuralisMotion
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.Song
import com.auralis.player.presentation.LibraryViewModel
import com.auralis.player.presentation.MainViewModel
import com.auralis.player.presentation.PlaylistsViewModel
import com.auralis.player.presentation.SearchViewModel
import com.auralis.player.presentation.StatsViewModel
import com.auralis.player.ui.components.MiniPlayer
import com.auralis.player.ui.components.PlaylistPickerSheet
import com.auralis.player.ui.components.RingtoneTrimmerSheet
import com.auralis.player.ui.components.SongActions
import com.auralis.player.ui.components.SongContextSheet
import com.auralis.player.ui.navigation.AuralisBottomBar
import com.auralis.player.ui.navigation.AuralisNavigationRail
import com.auralis.player.ui.navigation.Routes
import com.auralis.player.ui.navigation.TopDestination
import com.auralis.player.ui.screens.favorites.FavoritesScreen
import com.auralis.player.ui.screens.folders.FolderBrowserScreen
import com.auralis.player.ui.screens.home.HomeCallbacks
import com.auralis.player.ui.screens.home.HomeScreen
import com.auralis.player.ui.screens.library.LibraryCallbacks
import com.auralis.player.ui.screens.library.LibraryScreen
import com.auralis.player.ui.screens.library.SongListScreen
import com.auralis.player.ui.screens.playlists.PlaylistsScreen
import com.auralis.player.ui.screens.playlists.TextPromptDialog
import com.auralis.player.ui.screens.search.SearchScreen
import com.auralis.player.ui.screens.settings.SettingsScreen
import com.auralis.player.ui.screens.stats.StatsScreen
import com.auralis.player.ui.screens.tageditor.TagEditorScreen
import com.auralis.player.ui.theme.AuralisTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AuralisAppScaffold(
    mainViewModel: MainViewModel,
    wideLayout: Boolean,
    openNowPlayingSignal: Int,
    startRoute: String = Routes.HOME,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val playerState by mainViewModel.playerState.collectAsStateWithLifecycle()
    val scanProgress by mainViewModel.scanProgress.collectAsStateWithLifecycle()

    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val playlistsViewModel: PlaylistsViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val statsViewModel: StatsViewModel = hiltViewModel()

    var nowPlayingOpen by remember { mutableStateOf(false) }
    var contextSong by remember { mutableStateOf<Song?>(null) }
    var playlistPickerFor by remember { mutableStateOf<List<Long>?>(null) }
    var newPlaylistFor by remember { mutableStateOf<List<Long>?>(null) }
    var ringtoneFor by remember { mutableStateOf<Song?>(null) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(openNowPlayingSignal) {
        if (openNowPlayingSignal > 0) nowPlayingOpen = true
    }

    // Resolve the user's preferred start screen. Because composition is gated
    // until the persisted settings load, this is correct on the very first
    // frame — no post-render navigation jump, no flash of the default page.
    val resolvedStart = remember(startRoute) {
        when (startRoute) {
            Routes.LIBRARY, Routes.PLAYLISTS, Routes.FAVORITES -> startRoute
            else -> Routes.HOME
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.messages.collectLatest { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    val allSongs by libraryViewModel.allSongs.collectAsStateWithLifecycle()
    val playlists by playlistsViewModel.playlists.collectAsStateWithLifecycle()

    fun openSong(song: Song, list: List<Song>) {
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        mainViewModel.playAll(list, index)
    }

    val songActions = SongActions(
        onPlay = { song -> openSong(song, allSongs) },
        onPlayNext = { song -> mainViewModel.playNext(song) },
        onAddToQueue = { song -> mainViewModel.addToQueue(listOf(song)) },
        onAddToPlaylist = { song -> playlistPickerFor = listOf(song.id) },
        onToggleFavorite = { song -> mainViewModel.toggleFavorite(song) },
        onTogglePin = { song -> mainViewModel.togglePinned(song) },
        // Navigation targets must surface in the main UI: collapse the
        // full-screen player first, otherwise they open hidden behind it.
        onViewAlbum = { song ->
            nowPlayingOpen = false
            navController.navigate(Routes.album(song.albumId))
        },
        onViewArtist = { song ->
            nowPlayingOpen = false
            navController.navigate(Routes.artist(song.artist))
        },
        onEditTags = { song ->
            nowPlayingOpen = false
            navController.navigate(Routes.tagEditor(song.id))
        },
        onSetRingtone = { song ->
            if (mainViewModel.canSetRingtone()) {
                ringtoneFor = song
            } else {
                context.startActivity(mainViewModel.writeSettingsIntent())
            }
        },
        onShare = { song ->
            runCatching {
                context.startActivity(Intent.createChooser(mainViewModel.shareIntent(song), "Share track"))
            }.onFailure { mainViewModel.notify("Unable to share this track") }
        },
        onDelete = { song -> mainViewModel.deleteSong(song) }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuralisTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!wideLayout) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    playerState.currentSong?.let { song ->
                        MiniPlayer(
                            song = song,
                            isPlaying = playerState.isPlaying,
                            positionFlow = mainViewModel.player.position,
                            onExpand = { nowPlayingOpen = true },
                            onTogglePlay = { mainViewModel.player.togglePlayPause() },
                            onNext = { mainViewModel.player.next() },
                            onPrevious = { mainViewModel.player.previous() }
                        )
                    }
                    AuralisBottomBar(
                        current = currentRoute,
                        onSelect = { destination -> navigateTop(navController, destination) }
                    )
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (wideLayout) {
                AuralisNavigationRail(
                    current = currentRoute,
                    onSelect = { destination -> navigateTop(navController, destination) }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                AuralisNavGraph(
                    navController = navController,
                    mainViewModel = mainViewModel,
                    libraryViewModel = libraryViewModel,
                    playlistsViewModel = playlistsViewModel,
                    searchViewModel = searchViewModel,
                    statsViewModel = statsViewModel,
                    startRoute = resolvedStart,
                    contentPadding = padding,
                    onSongMenu = { contextSong = it },
                    onOpenNowPlaying = { nowPlayingOpen = true },
                    onCreatePlaylistWith = { ids -> newPlaylistFor = ids },
                    onExportBackup = onExportBackup,
                    onImportBackup = onImportBackup
                )

                if (wideLayout) {
                    playerState.currentSong?.let { song ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(androidx.compose.ui.Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 12.dp)
                        ) {
                            MiniPlayer(
                                song = song,
                                isPlaying = playerState.isPlaying,
                                positionFlow = mainViewModel.player.position,
                                onExpand = { nowPlayingOpen = true },
                                onTogglePlay = { mainViewModel.player.togglePlayPause() },
                                onNext = { mainViewModel.player.next() },
                                onPrevious = { mainViewModel.player.previous() }
                            )
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = nowPlayingOpen && playerState.currentSong != null,
        enter = slideInVertically(
            animationSpec = tween(420, easing = AuralisMotion.EmphasizedEasing)
        ) { it } + fadeIn(tween(220)) + scaleIn(tween(420, easing = AuralisMotion.EmphasizedEasing), initialScale = 0.94f),
        exit = slideOutVertically(
            animationSpec = tween(280, easing = AuralisMotion.ExitEasing)
        ) { it } + fadeOut(tween(160)) + scaleOut(tween(280), targetScale = 0.96f)
    ) {
        NowPlayingHost(
            mainViewModel = mainViewModel,
            onCollapse = { nowPlayingOpen = false },
            onSongMenu = { contextSong = it },
            onOpenAlbum = { song ->
                nowPlayingOpen = false
                navController.navigate(Routes.album(song.albumId))
            },
            onOpenArtist = { song ->
                nowPlayingOpen = false
                navController.navigate(Routes.artist(song.artist))
            }
        )
    }

    contextSong?.let { song ->
        SongContextSheet(
            song = song,
            isPinned = settings.pinnedSongs.contains(song.id),
            actions = songActions,
            onDismiss = { contextSong = null }
        )
    }

    playlistPickerFor?.let { ids ->
        PlaylistPickerSheet(
            playlists = playlists,
            onPick = { playlistId ->
                mainViewModel.addToPlaylist(playlistId, ids)
                playlistPickerFor = null
            },
            onCreateNew = {
                playlistPickerFor = null
                newPlaylistFor = ids
            },
            onDismiss = { playlistPickerFor = null }
        )
    }

    newPlaylistFor?.let { ids ->
        TextPromptDialog(
            title = "New playlist",
            initial = "My playlist",
            confirmLabel = "Create",
            onConfirm = { name ->
                if (name.isNotBlank()) mainViewModel.createPlaylistWith(name, ids)
                newPlaylistFor = null
            },
            onDismiss = { newPlaylistFor = null }
        )
    }

    ringtoneFor?.let { song ->
        RingtoneTrimmerSheet(
            song = song,
            onSetTrimmed = { start, end -> mainViewModel.trimRingtone(song, start, end) },
            onSetFull = { mainViewModel.setAsRingtone(song) },
            onDismiss = { ringtoneFor = null }
        )
    }
}

private fun navigateTop(navController: NavHostController, destination: TopDestination) {
    // Fast path: the tab is already in the back stack — pop back to its root,
    // dropping any sub-screens above it (e.g. Statistics). The tab's own state
    // and scroll survive because its back-stack entry is never destroyed, and
    // normal tab-to-tab switching keeps working exactly as before.
    val popped = navController.popBackStack(destination.route, inclusive = false)
    if (!popped) {
        // The tab isn't on the back stack yet — navigate to it with the
        // standard bottom-navigation pattern (preserves each tab's state).
        navController.navigate(destination.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}
