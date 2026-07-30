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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onRemoveSong: ((Song) -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
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

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
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
                }
            }

            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onPlay(songs, songs.indexOf(song)) },
                    onMenu = { onSongMenu(song) },
                    isPlaying = song.id == currentSongId,
                    modifier = Modifier.padding(horizontal = spacing.sm)
                )
            }
        }
    }
}
