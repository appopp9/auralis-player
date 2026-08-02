package com.auralis.player.ui.screens.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.GridStyle
import com.auralis.player.domain.model.LibraryTab
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.SortOrder
import com.auralis.player.presentation.LibraryUiState
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.ScreenEnterWindow
import com.auralis.player.ui.components.CollectionCard
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.SongGridCard
import com.auralis.player.ui.components.SongRow
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display

data class LibraryCallbacks(
    val onSelectTab: (LibraryTab) -> Unit,
    val onSetSort: (SortOrder) -> Unit,
    val onToggleGrid: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onPlaySongs: (List<Song>, Int) -> Unit,
    val onSongMenu: (Song) -> Unit,
    val onOpenAlbum: (Long) -> Unit,
    val onOpenArtist: (String) -> Unit,
    val onOpenGenre: (String) -> Unit,
    val onOpenMood: (String) -> Unit,
    val onOpenFolder: (String) -> Unit,
    val onOpenPlaylist: (Long) -> Unit
)

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    currentSongId: Long,
    pinnedIds: Set<Long>,
    callbacks: LibraryCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Persisted across navigation: coming back to the library keeps the scroll.
    val listState = com.auralis.player.ui.components.rememberPersistentListState("library_main")

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = com.auralis.player.ui.i18n.LocalStrings.current.library,
                style = AuralisTheme.style.display(AuralisType.display),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            AccentIconButton(Icons.Rounded.Search, "Search library") { callbacks.onOpenSearch() }
            Box {
                AccentIconButton(Icons.Rounded.Sort, "Sort options") { sortMenuOpen = true }
                com.auralis.player.ui.components.SortMenu(
                    expanded = sortMenuOpen,
                    current = state.sort,
                    options = SortOrder.entries.map { it to sortLabel(it) },
                    onSelect = { order ->
                        sortMenuOpen = false
                        callbacks.onSetSort(order)
                    },
                    onDismiss = { sortMenuOpen = false }
                )
            }
            AccentIconButton(
                icon = if (state.grid == GridStyle.LIST) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                contentDescription = "Toggle grid or list layout",
                onClick = callbacks.onToggleGrid
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            modifier = Modifier.padding(bottom = spacing.sm)
        ) {
            items(LibraryTab.entries.toList()) { tab ->
                AuralisChip(
                    label = tabLabel(tab),
                    selected = state.tab == tab,
                    onClick = { callbacks.onSelectTab(tab) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            val motion = AuralisTheme.motion
            AnimatedContent(
                targetState = state.tab,
                transitionSpec = {
                    val forward = targetState.ordinal >= initialState.ordinal
                    (fadeIn(motion.tweenMedium()) + slideInHorizontally(motion.tweenMedium()) {
                        if (forward) it / 12 else -it / 12
                    }) togetherWith fadeOut(motion.tweenFast())
                },
                label = "libraryTab"
            ) { tab ->
                ScreenEnterWindow {
                    LibraryPane(tab, state, currentSongId, pinnedIds, callbacks, contentPadding, listState)
                }
            }
        }
    }
}

@Composable
private fun LibraryPane(
    tab: LibraryTab,
    state: LibraryUiState,
    currentSongId: Long,
    pinnedIds: Set<Long>,
    callbacks: LibraryCallbacks,
    contentPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                LibraryTab.SONGS -> SongsPane(state.songs, state, currentSongId, pinnedIds, callbacks, contentPadding, listState)
                LibraryTab.FAVORITES -> SongsPane(state.favorites, state, currentSongId, pinnedIds, callbacks, contentPadding, listState)
                LibraryTab.ALBUMS -> GridPane(
                    items = state.albums.map { GridEntry(it.name, "${it.artist} • ${it.songCount} tracks", it.artworkSongId) { callbacks.onOpenAlbum(it.id) } },
                    contentPadding = contentPadding,
                    emptyTitle = com.auralis.player.ui.i18n.LocalStrings.current.nothingHere
                )
                LibraryTab.ARTISTS -> GridPane(
                    items = state.artists.map { GridEntry(it.name, "${it.songCount} tracks • ${it.albumCount} albums", it.artworkSongId) { callbacks.onOpenArtist(it.name) } },
                    contentPadding = contentPadding,
                    emptyTitle = com.auralis.player.ui.i18n.LocalStrings.current.nothingHere,
                    circular = true
                )
                LibraryTab.ALBUM_ARTISTS -> GridPane(
                    items = state.albumArtists.map { GridEntry(it.name, "${it.songCount} tracks", it.artworkSongId) { callbacks.onOpenArtist(it.name) } },
                    contentPadding = contentPadding,
                    emptyTitle = "No album artists",
                    circular = true
                )
                LibraryTab.GENRES -> GridPane(
                    items = state.genres.map { GridEntry(it.name, "${it.songCount} tracks", it.artworkSongId) { callbacks.onOpenGenre(it.name) } },
                    contentPadding = contentPadding,
                    emptyTitle = "No genres"
                )
                LibraryTab.MOODS -> GridPane(
                    items = state.moods.map { GridEntry(it.name, "${it.songCount} tracks", it.artworkSongId) { callbacks.onOpenMood(it.name) } },
                    contentPadding = contentPadding,
                    emptyTitle = "No moods yet"
                )
                LibraryTab.FOLDERS -> ListPane(
                    items = state.folders.map { GridEntry(it.name, "${it.songCount} tracks • ${it.path}", it.artworkSongId) { callbacks.onOpenFolder(it.path) } },
                    contentPadding = contentPadding,
                    emptyTitle = "No folders"
                )
                LibraryTab.PLAYLISTS -> ListPane(
                    items = state.playlists.map { GridEntry(it.name, "${it.songCount} tracks", it.artworkSongId) { callbacks.onOpenPlaylist(it.id) } },
                    contentPadding = contentPadding,
                    emptyTitle = "No playlists"
                )
            }
    }
}

data class GridEntry(
    val title: String,
    val subtitle: String,
    val artworkSongId: Long,
    val onClick: () -> Unit
)

@Composable
private fun SongsPane(
    rawSongs: List<Song>,
    state: LibraryUiState,
    currentSongId: Long,
    pinnedIds: Set<Long>,
    callbacks: LibraryCallbacks,
    contentPadding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    // Pinned songs float to the top; stable sort keeps user sort within groups.
    val songs = androidx.compose.runtime.remember(rawSongs, pinnedIds) {
        if (pinnedIds.isEmpty()) rawSongs
        else rawSongs.sortedByDescending { it.id in pinnedIds }
    }
    if (songs.isEmpty()) {
        EmptyPane(
            com.auralis.player.ui.i18n.LocalStrings.current.nothingHere,
            com.auralis.player.ui.i18n.LocalStrings.current.nothingHereMessage
        )
        return
    }
    if (state.grid == GridStyle.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
            modifier = Modifier.padding(horizontal = AuralisTheme.spacing.md)
        ) {
            items(songs, key = { it.id }) { song ->
                SongGridCard(
                    song = song,
                    onClick = { callbacks.onPlaySongs(songs, songs.indexOf(song)) },
                    onMenu = { callbacks.onSongMenu(song) },
                    isPlaying = song.id == currentSongId
                )
            }
        }
    } else {
        // The alphabet rail was removed: the sort menu already orders the
        // library by letter, so the rail only stole horizontal space and
        // pushed the row overflow button under the user's thumb.
        LazyColumn(state = listState, contentPadding = contentPadding) {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id },
                contentType = { _, _ -> "song" }
            ) { index, song ->
                SongRow(
                    song = song,
                    onClick = { callbacks.onPlaySongs(songs, index) },
                    onMenu = { callbacks.onSongMenu(song) },
                    isPlaying = song.id == currentSongId,
                    isPinned = song.id in pinnedIds,
                    modifier = Modifier.padding(horizontal = AuralisTheme.spacing.sm)
                )
            }
        }
    }
}

@Composable
private fun GridPane(
    items: List<GridEntry>,
    contentPadding: PaddingValues,
    emptyTitle: String,
    circular: Boolean = false
) {
    if (items.isEmpty()) {
        EmptyPane(emptyTitle, "Add music to your device to populate this section.")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.sm),
        modifier = Modifier.padding(horizontal = AuralisTheme.spacing.md)
    ) {
        items(items, key = { it.title + it.artworkSongId }) { entry ->
            CollectionCard(
                title = entry.title,
                subtitle = entry.subtitle,
                artworkSongId = entry.artworkSongId,
                onClick = entry.onClick,
                circular = circular
            )
        }
    }
}

@Composable
private fun ListPane(
    items: List<GridEntry>,
    contentPadding: PaddingValues,
    emptyTitle: String
) {
    if (items.isEmpty()) {
        EmptyPane(emptyTitle, "Nothing to show here yet.")
        return
    }
    LazyColumn(contentPadding = contentPadding) {
        items(items, key = { it.title + it.artworkSongId }) { entry ->
            com.auralis.player.ui.components.PressableSurface(
                onClick = entry.onClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = AuralisTheme.spacing.sm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuralisTheme.spacing.md, vertical = AuralisTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)
                ) {
                    com.auralis.player.ui.components.SongArtwork(
                        songId = entry.artworkSongId,
                        modifier = Modifier.height(52.dp).aspectRatio(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.title, style = AuralisType.body, color = AuralisTheme.colors.textPrimary, maxLines = 1)
                        Text(text = entry.subtitle, style = AuralisType.bodySmall, color = AuralisTheme.colors.textSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPane(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(icon = Icons.Rounded.LibraryMusic, title = title, message = message)
    }
}

@androidx.compose.runtime.Composable
fun tabLabel(tab: LibraryTab): String {
    val strings = com.auralis.player.ui.i18n.LocalStrings.current
    return when (tab) {
        LibraryTab.SONGS -> strings.songs
        LibraryTab.ARTISTS -> strings.artists
        LibraryTab.ALBUMS -> strings.albums
        LibraryTab.ALBUM_ARTISTS -> strings.albumArtists
        LibraryTab.GENRES -> strings.genres
        LibraryTab.FOLDERS -> strings.folders
        LibraryTab.PLAYLISTS -> strings.playlists
        LibraryTab.MOODS -> strings.moods
        LibraryTab.FAVORITES -> strings.favorites
    }
}

@androidx.compose.runtime.Composable
fun sortLabel(order: SortOrder): String {
    val strings = com.auralis.player.ui.i18n.LocalStrings.current
    return when (order) {
        SortOrder.TITLE_ASC -> strings.sortTitleAsc
        SortOrder.TITLE_DESC -> strings.sortTitleDesc
        SortOrder.RECENTLY_ADDED -> strings.sortRecentlyAdded
        SortOrder.RECENTLY_PLAYED -> strings.sortRecentlyPlayed
        SortOrder.MOST_PLAYED -> strings.sortMostPlayed
        SortOrder.DURATION -> strings.sortDuration
        SortOrder.YEAR -> strings.sortYear
        SortOrder.ARTIST -> strings.sortArtist
        SortOrder.ALBUM -> strings.sortAlbum
    }
}
