package com.auralis.player.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.auralis.player.domain.model.Album
import com.auralis.player.domain.model.Artist
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.theme.GoldAccent

@Composable
fun SearchScreen(
    onSongClick: (Long) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldAccent.Surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Search Bar ─────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = {
                            Text(
                                "Search songs, artists, albums...",
                                color = GoldAccent.TextTertiary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = GoldAccent.Primary
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearSearch() }) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = GoldAccent.TextSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = GoldAccent.TextPrimary,
                            unfocusedTextColor = GoldAccent.TextPrimary,
                            focusedBorderColor = GoldAccent.Primary,
                            unfocusedBorderColor = GoldAccent.Divider,
                            cursorColor = GoldAccent.Primary,
                            focusedContainerColor = GoldAccent.SurfaceCard,
                            unfocusedContainerColor = GoldAccent.SurfaceCard
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.onSearch(query)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }

            // ── Recent Searches (when no query) ────────────────────────
            if (query.isBlank() && recentSearches.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent.TextSecondary
                        )
                    }
                }

                items(
                    items = recentSearches,
                    key = { it }
                ) { search ->
                    RecentSearchRow(
                        query = search,
                        onClick = { viewModel.onSearch(search) },
                        onRemove = { viewModel.removeRecentSearch(search) }
                    )
                }
            }

            // ── Results tabs (when there are results) ──────────────────
            if (query.isNotBlank() && !results.isEmpty) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResultTab(
                            title = "Songs",
                            count = results.songs.size,
                            selected = selectedTab == SearchTab.SONGS,
                            onClick = { viewModel.selectTab(SearchTab.SONGS) }
                        )
                        ResultTab(
                            title = "Albums",
                            count = results.albums.size,
                            selected = selectedTab == SearchTab.ALBUMS,
                            onClick = { viewModel.selectTab(SearchTab.ALBUMS) }
                        )
                        ResultTab(
                            title = "Artists",
                            count = results.artists.size,
                            selected = selectedTab == SearchTab.ARTISTS,
                            onClick = { viewModel.selectTab(SearchTab.ARTISTS) }
                        )
                    }
                }

                // Songs results
                if (selectedTab == SearchTab.SONGS) {
                    items(
                        items = results.songs,
                        key = { it.id }
                    ) { song ->
                        AnimatedSearchSongRow(
                            song = song,
                            onClick = {
                                viewModel.playSong(song)
                                onSongClick(song.id)
                            }
                        )
                    }
                }

                // Albums results
                if (selectedTab == SearchTab.ALBUMS) {
                    items(
                        items = results.albums,
                        key = { it.id }
                    ) { album ->
                        AnimatedSearchAlbumRow(
                            album = album,
                            onClick = {
                                viewModel.playAlbum(album)
                                onAlbumClick(album.id)
                            }
                        )
                    }
                }

                // Artists results
                if (selectedTab == SearchTab.ARTISTS) {
                    items(
                        items = results.artists,
                        key = { it.name }
                    ) { artist ->
                        AnimatedSearchArtistRow(
                            artist = artist,
                            onClick = {
                                viewModel.playArtist(artist)
                                onArtistClick(artist.name)
                            }
                        )
                    }
                }
            }

            // ── Empty state ────────────────────────────────────────────
            if (query.isNotBlank() && results.isEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = GoldAccent.TextTertiary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No results for \"$query\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldAccent.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultTab(title: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) GoldAccent.Primary.copy(alpha = 0.15f) else Color.Transparent,
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) GoldAccent.Primary else GoldAccent.TextSecondary
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) GoldAccent.Primary else GoldAccent.TextTertiary
                )
            }
        }
    }
}

// ── Animated row wrappers ──────────────────────────────────────────────────

@Composable
private fun AnimatedSearchSongRow(song: Song, onClick: () -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = true
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            animationSpec = tween(220), initialOffsetY = { it / 10 }
        )
    ) {
        SearchResultRow(
            title = song.title,
            subtitle = "${song.displayArtist} • ${song.displayAlbum}",
            artworkUri = song.artworkUri,
            typeBadge = "Song",
            onClick = onClick
        )
    }
}

@Composable
private fun AnimatedSearchAlbumRow(album: Album, onClick: () -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = true
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            animationSpec = tween(220), initialOffsetY = { it / 10 }
        )
    ) {
        SearchResultRow(
            title = album.name,
            subtitle = "${album.artist} • ${album.songCount} tracks",
            artworkUri = album.artworkUri,
            typeBadge = "Album",
            onClick = onClick
        )
    }
}

@Composable
private fun AnimatedSearchArtistRow(artist: Artist, onClick: () -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = true
    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(animationSpec = tween(180)) + slideInVertically(
            animationSpec = tween(220), initialOffsetY = { it / 10 }
        )
    ) {
        SearchResultRow(
            title = artist.name,
            subtitle = "${artist.songCount} songs • ${artist.albumCount} albums",
            artworkUri = artist.artworkUri,
            typeBadge = "Artist",
            onClick = onClick
        )
    }
}

// ── Unified result row ─────────────────────────────────────────────────────

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    artworkUri: String,
    typeBadge: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GoldAccent.SurfaceCard)
        )

        Spacer(Modifier.width(14.dp))

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = GoldAccent.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Type badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = GoldAccent.BadgeBackground
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (typeBadge) {
                        "Song" -> Icons.Default.MusicNote
                        "Album" -> Icons.Default.Album
                        else -> Icons.Default.Person
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = GoldAccent.TextTertiary
                )
                Text(
                    text = typeBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent.TextTertiary
                )
            }
        }
    }
}

// ── Recent search row ──────────────────────────────────────────────────────

@Composable
private fun RecentSearchRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = GoldAccent.TextTertiary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(14.dp))

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = GoldAccent.TextPrimary,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = GoldAccent.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
