package com.auralis.player.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.SongRow
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

/**
 * Shared detail screen for albums, artists, genres, moods, folders, playlists
 * and smart playlists. Everything on it is functional — no placeholders.
 */
@Composable
fun SongListScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    currentSongId: Long,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onSongMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
    artworkSongId: Long = songs.firstOrNull()?.id ?: 0L,
    extraAction: Pair<String, () -> Unit>? = null,
    onRemoveSong: ((Song) -> Unit)? = null,
    /** Non-null enables reorder mode (playlists): emits the new id order. */
    onReorder: ((List<Long>) -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    var reorderMode by remember { mutableStateOf(false) }
    // Local working order for instant feedback; resets whenever the source
    // list changes (i.e. after the repository persists the new order).
    var localOrder by remember(songs) { mutableStateOf(songs) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back") { onBack() }
            Text(
                text = title,
                style = AuralisType.title,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = spacing.sm)
            )
        }

        if (songs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "Nothing here yet",
                    message = "This collection is empty. Add tracks to see them here."
                )
            }
            return
        }

        LazyColumn(
            state = com.auralis.player.ui.components.rememberPersistentListState("detail_$title"),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screen, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SongArtwork(
                        songId = artworkSongId,
                        modifier = Modifier.size(132.dp),
                        shape = AuralisTheme.shapes.artwork,
                        fallbackIconSize = 40.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(text = title, style = AuralisType.headline, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(text = subtitle, style = AuralisType.bodySmall, color = colors.textSecondary)
                        Text(
                            text = "${songs.size} tracks • ${Formatters.longDuration(songs.sumOf { it.durationMs })}",
                            style = AuralisType.bodySmall,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    AuralisChip(
                        label = "Play",
                        selected = true,
                        leadingIcon = Icons.Rounded.PlayArrow,
                        onClick = { onPlay(songs, 0) }
                    )
                    AuralisChip(
                        label = "Shuffle",
                        selected = false,
                        leadingIcon = Icons.Rounded.Shuffle,
                        onClick = { onShuffle(songs) }
                    )
                    if (extraAction != null) {
                        AuralisChip(
                            label = extraAction.first,
                            selected = false,
                            leadingIcon = Icons.Rounded.PlaylistAdd,
                            onClick = extraAction.second
                        )
                    }
                    if (onReorder != null) {
                        AuralisChip(
                            label = if (reorderMode) "Done" else "Reorder",
                            selected = reorderMode,
                            leadingIcon = Icons.Rounded.SwapVert,
                            onClick = { reorderMode = !reorderMode }
                        )
                    }
                }
            }

            if (reorderMode && onReorder != null) {
                items(localOrder, key = { it.id }) { song ->
                    val index = localOrder.indexOfFirst { it.id == song.id }
                    fun move(to: Int) {
                        if (to < 0 || to > localOrder.lastIndex) return
                        val mutable = localOrder.toMutableList()
                        val item = mutable.removeAt(index)
                        mutable.add(to, item)
                        localOrder = mutable
                        onReorder(mutable.map { it.id })
                    }
                    ReorderRow(
                        song = song,
                        canUp = index > 0,
                        canDown = index < localOrder.lastIndex,
                        onUp = { move(index - 1) },
                        onDown = { move(index + 1) },
                        modifier = Modifier
                            .animateItem(placementSpec = AuralisTheme.motion.softSpring())
                            .padding(horizontal = spacing.sm)
                    )
                }
            } else {
                // contentType lets Compose reuse row nodes across the whole
                // list, and the index comes from itemsIndexed instead of an
                // O(n) indexOf on every tap — both matter on 10k-song lists.
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "song" }
                ) { index, song ->
                    SongRow(
                        song = song,
                        onClick = { onPlay(songs, index) },
                        onMenu = { onSongMenu(song) },
                        isPlaying = song.id == currentSongId,
                        modifier = Modifier.padding(horizontal = spacing.sm)
                    )
                }
            }
        }
    }
}

/** Compact row shown while reordering a playlist: artwork, title, up/down. */
@Composable
private fun ReorderRow(
    song: Song,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        SongArtwork(
            songId = song.id,
            modifier = Modifier.size(44.dp),
            shape = AuralisTheme.shapes.small
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = AuralisType.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.displayArtist,
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AccentIconButton(
            icon = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "Move up",
            size = 38.dp,
            tint = if (canUp) colors.textPrimary else colors.textTertiary
        ) { onUp() }
        AccentIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Move down",
            size = 38.dp,
            tint = if (canDown) colors.textPrimary else colors.textTertiary
        ) { onDown() }
    }
}
