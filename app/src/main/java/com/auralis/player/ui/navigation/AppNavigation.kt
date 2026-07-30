import androidx.compose.material3.ExperimentalMaterial3Api
package com.auralis.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.playback.PlaybackController
import com.auralis.player.ui.albums.AlbumDetailScreen
import com.auralis.player.ui.artists.ArtistDetailScreen
import com.auralis.player.ui.library.LibraryScreen
import com.auralis.player.ui.playlists.PlaylistDetailScreen
import com.auralis.player.ui.screens.home.HomeScreen
import com.auralis.player.ui.screens.nowplaying.NowPlayingScreen
import com.auralis.player.ui.search.SearchScreen
import com.auralis.player.ui.settings.SettingsScreen
import com.auralis.player.ui.theme.GoldAccent
import kotlinx.coroutines.launch
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ALBUM_DETAIL = "albums/{albumId}"
    const val ARTIST_DETAIL = "artists/{artistName}"
    const val PLAYLIST_DETAIL = "playlists/{playlistId}"
    const val SMART_PLAYLIST = "smart_playlists/{type}"
    const val NOW_PLAYING = "now_playing"

    fun albumDetail(albumId: Long) = "albums/$albumId"
    fun artistDetail(artistName: String): String {
        val encoded = URLEncoder.encode(artistName, "UTF-8")
        return "artists/$encoded"
    }
    fun playlistDetail(playlistId: Long) = "playlists/$playlistId"
    fun smartPlaylist(type: String) = "smart_playlists/$type"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.LIBRARY, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    playbackController: PlaybackController,
    musicRepository: MusicRepository
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isBottomBarVisible = currentDestination?.route in bottomNavItems.map { it.route }
        && currentDestination?.route != Routes.NOW_PLAYING

    // ── Mini player state ──────────────────────────────────────────────────
    val player = playbackController.player
    val isPlaying by remember { androidx.compose.runtime.derivedStateOf { player.isPlaying } }
    val currentTitle by remember {
        androidx.compose.runtime.derivedStateOf {
            player.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
        }
    }
    val currentArtist by remember {
        androidx.compose.runtime.derivedStateOf {
            player.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
        }
    }
    val hasCurrentSong = currentTitle.isNotEmpty()

    // Periodic refresh for mini-player progress
    var tick by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            tick++
        }
    }
    val progress by remember(tick) {
        androidx.compose.runtime.derivedStateOf {
            if (player.duration > 0) player.currentPosition.toFloat() / player.duration.toFloat()
            else 0f
        }
    }

    val showMiniPlayer = hasCurrentSong && isBottomBarVisible

    Scaffold(
        containerColor = GoldAccent.Surface,
        topBar = {
            if (isBottomBarVisible) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Auralis",
                            style = MaterialTheme.typography.headlineMedium,
                            color = GoldAccent.TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GoldAccent.Surface
                    )
                )
            }
        },
        bottomBar = {
            Column {
                // ── Shared Mini Player Bar ──────────────────────────────
                if (showMiniPlayer) {
                    MiniPlayerBar(
                        title = currentTitle,
                        artist = currentArtist,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = {
                            if (player.isPlaying) player.pause() else player.play()
                        },
                        onNext = { player.seekToNext() },
                        onClick = { navController.navigate(Routes.NOW_PLAYING) }
                    )
                }

                // ── Bottom Navigation Bar ────────────────────────────────
                if (isBottomBarVisible) {
                    NavigationBar(
                        containerColor = GoldAccent.SurfaceElevated,
                        contentColor = GoldAccent.TextSecondary
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            NavigationBarItem(
                                icon = {
                                    androidx.compose.material3.Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                    },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = GoldAccent.Primary,
                                    selectedTextColor = GoldAccent.Primary,
                                    unselectedIconColor = GoldAccent.TextTertiary,
                                    unselectedTextColor = GoldAccent.TextTertiary,
                                    indicatorColor = GoldAccent.Primary.copy(alpha = 0.12f)
                                ),
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            // ── Home ────────────────────────────────────────────────────────
            composable(Routes.HOME) {
                HomeScreen(
                    onSongClick = { /* Song click handled inside HomeScreen via viewModel.play */ },
                    onOpenNowPlaying = { navController.navigate(Routes.NOW_PLAYING) },
                    onOpenLibrary = {
                        navController.navigate(Routes.LIBRARY) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // ── Library (with Songs/Artists/Albums/Playlists tabs) ─────────
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onSongClick = { /* Song click handled inside LibraryScreen via viewModel.play */ },
                    onShuffleAll = { /* Shuffle handled inside LibraryScreen */ },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.artistDetail(artistName))
                    },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.albumDetail(albumId))
                    },
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Routes.playlistDetail(playlistId))
                    }
                )
            }

            // ── Search ──────────────────────────────────────────────────────
            composable(Routes.SEARCH) {
                SearchScreen(
                    onSongClick = { /* Song click handled inside SearchScreen via viewModel.playSong */ },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.albumDetail(albumId))
                    },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.artistDetail(artistName))
                    }
                )
            }

            // ── Settings ────────────────────────────────────────────────────
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }

            // ── Album Detail ────────────────────────────────────────────────
            composable(
                route = Routes.ALBUM_DETAIL,
                arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
                AlbumDetailScreen(
                    albumId = albumId,
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* Song click handled inside AlbumDetailScreen via viewModel.playSong */ }
                )
            }

            // ── Artist Detail ───────────────────────────────────────────────
            composable(
                route = Routes.ARTIST_DETAIL,
                arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            ) { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
                val decoded = java.net.URLDecoder.decode(artistName, "UTF-8")
                ArtistDetailScreen(
                    artistName = decoded,
                    onBack = { navController.popBackStack() },
                    onSongClick = { /* Song click handled inside ArtistDetailScreen via viewModel.playSong */ }
                )
            }

            // ── Playlist Detail ─────────────────────────────────────────────
            composable(
                route = Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Smart Playlist ──────────────────────────────────────────────
            composable(
                route = Routes.SMART_PLAYLIST,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        tween(300)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Right,
                        tween(300)
                    )
                }
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: return@composable
                com.auralis.player.ui.playlists.SmartPlaylistDetailScreen(
                    type = type,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Now Playing (full-screen) ──────────────────────────────────
            composable(
                route = Routes.NOW_PLAYING,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        tween(350)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        tween(350)
                    )
                }
            ) {
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                NowPlayingScreen(
                    player = player,
                    onDismiss = { navController.popBackStack() },
                    onToggleFavorite = {
                        // Toggle favorite for current song
                        val songId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return@NowPlayingScreen
                        coroutineScope.launch {
                            musicRepository.toggleFavorite(songId)
                        }
                    }
                )
            }
        }
    }
}

// ── Shared Mini Player Bar ─────────────────────────────────────────────────
@Composable
private fun MiniPlayerBar(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = GoldAccent.SurfaceElevated,
        tonalElevation = 8.dp
    ) {
        Column {
            // Content row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = GoldAccent.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                androidx.compose.material3.IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.Primary)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = GoldAccent.OnPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(4.dp))

                androidx.compose.material3.IconButton(onClick = onNext) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Next",
                        tint = GoldAccent.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
                    .size(height = 2.dp, width = 0.dp),
                color = GoldAccent.Primary,
                trackColor = GoldAccent.SurfaceCard,
            )
        }
    }
}
