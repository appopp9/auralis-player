package com.auralis.player.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.auralis.player.ui.albums.AlbumDetailScreen
import com.auralis.player.ui.albums.AlbumsScreen
import com.auralis.player.ui.artists.ArtistDetailScreen
import com.auralis.player.ui.artists.ArtistsScreen
import com.auralis.player.ui.library.LibraryScreen
import com.auralis.player.ui.playlists.PlaylistsScreen
import com.auralis.player.ui.search.SearchScreen
import com.auralis.player.ui.theme.GoldAccent
import java.net.URLEncoder

object Routes {
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"
    const val SEARCH = "search"
    const val ALBUM_DETAIL = "albums/{albumId}"
    const val ARTIST_DETAIL = "artists/{artistName}"

    fun albumDetail(albumId: Long) = "albums/$albumId"
    fun artistDetail(artistName: String): String {
        val encoded = URLEncoder.encode(artistName, "UTF-8")
        return "artists/$encoded"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.SONGS, "Songs", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    BottomNavItem(Routes.ALBUMS, "Albums", Icons.Filled.Album, Icons.Outlined.Album),
    BottomNavItem(Routes.ARTISTS, "Artists", Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem(Routes.PLAYLISTS, "Playlists", Icons.Filled.PlaylistPlay, Icons.Outlined.PlaylistPlay),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isBottomBarVisible = currentDestination?.route in bottomNavItems.map { it.route }

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
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = GoldAccent.SurfaceElevated,
                    contentColor = GoldAccent.TextSecondary
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SONGS,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Routes.SONGS) {
                LibraryScreen()
            }

            composable(Routes.ALBUMS) {
                AlbumsScreen(
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.albumDetail(albumId))
                    }
                )
            }

            composable(Routes.ARTISTS) {
                ArtistsScreen(
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.artistDetail(artistName))
                    }
                )
            }

            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(
                    onPlaylistClick = { /* TODO: playlist detail */ },
                    onSmartPlaylistClick = { /* TODO: smart playlist detail */ }
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(
                    onSongClick = { /* navigate to player or do nothing */ },
                    onAlbumClick = { albumId ->
                        navController.navigate(Routes.albumDetail(albumId))
                    },
                    onArtistClick = { artistName ->
                        navController.navigate(Routes.artistDetail(artistName))
                    }
                )
            }

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
                    onSongClick = { /* player integration */ }
                )
            }

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
                    onSongClick = { /* player integration */ }
                )
            }
        }
    }
}
