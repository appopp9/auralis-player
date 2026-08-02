package com.auralis.player.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.auralis.player.domain.model.Song
import com.auralis.player.presentation.SearchUiState
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.CollectionCard
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SongRow
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(
    state: SearchUiState,
    query: String,
    suggestions: List<String>,
    currentSongId: Long,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onSongMenu: (Song) -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenGenre: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val focusRequester = remember { FocusRequester() }

    // The field owns its own text + cursor. It is never driven by a debounced
    // flow, which is what previously reordered characters while typing.
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (query != field.text) {
            field = TextFieldValue(query, TextRange(query.length))
        }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back") { onBack() }
            com.auralis.player.ui.components.AuralisSearchField(
                value = field,
                onValueChange = { value ->
                    field = value
                    if (value.text != query) onQueryChange(value.text)
                },
                placeholder = "Songs, artists, albums, folders…",
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f)
            )
        }

        if (query.isBlank()) {
            if (suggestions.isNotEmpty()) {
                SectionHeader(title = "Try searching for")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.screen),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    items(suggestions) { suggestion ->
                        AuralisChip(label = suggestion, selected = false, onClick = { onQueryChange(suggestion) })
                    }
                }
            }
            return@Column
        }

        if (state.isEmpty && state.query.trim() == query.trim()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No results",
                    message = "Nothing matched \"${state.query}\". Try fewer characters or a different spelling."
                )
            }
            return@Column
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            if (state.results.songs.isNotEmpty()) {
                item { SectionHeader(title = "Songs") }
                items(state.results.songs, key = { "s-${it.id}" }) { song ->
                    SongRow(
                        song = song,
                        onClick = { onPlaySongs(state.results.songs, state.results.songs.indexOf(song)) },
                        onMenu = { onSongMenu(song) },
                        isPlaying = song.id == currentSongId,
                        modifier = Modifier.padding(horizontal = spacing.sm)
                    )
                }
            }
            if (state.results.artists.isNotEmpty()) {
                item { SectionHeader(title = "Artists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        items(state.results.artists, key = { "ar-${it.name}" }) { artist ->
                            CollectionCard(
                                title = artist.name,
                                subtitle = "${artist.songCount} tracks",
                                artworkSongId = artist.artworkSongId,
                                circular = true,
                                onClick = { onOpenArtist(artist.name) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
            if (state.results.albums.isNotEmpty()) {
                item { SectionHeader(title = "Albums") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        items(state.results.albums, key = { "al-${it.id}" }) { album ->
                            CollectionCard(
                                title = album.name,
                                subtitle = album.artist,
                                artworkSongId = album.artworkSongId,
                                onClick = { onOpenAlbum(album.id) },
                                modifier = Modifier.width(140.dp)
                            )
                        }
                    }
                }
            }
            if (state.results.genres.isNotEmpty()) {
                item { SectionHeader(title = "Genres") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        items(state.results.genres, key = { "g-${it.name}" }) { genre ->
                            AuralisChip(label = "${genre.name} (${genre.songCount})", selected = false) {
                                onOpenGenre(genre.name)
                            }
                        }
                    }
                }
            }
            if (state.playlists.isNotEmpty()) {
                item { SectionHeader(title = "Playlists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        items(state.playlists, key = { "p-${it.id}" }) { playlist ->
                            AuralisChip(label = "${playlist.name} (${playlist.songCount})", selected = false) {
                                onOpenPlaylist(playlist.id)
                            }
                        }
                    }
                }
            }
            if (state.results.folders.isNotEmpty()) {
                item { SectionHeader(title = "Folders") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = spacing.screen),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        items(state.results.folders, key = { "f-${it.path}" }) { folder ->
                            AuralisChip(label = "${folder.name} (${folder.songCount})", selected = false) {
                                onOpenFolder(folder.path)
                            }
                        }
                    }
                }
            }
        }
    }
}
