package com.auralis.player.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.SongRow
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@Composable
fun FavoritesScreen(
    favorites: List<Song>,
    currentSongId: Long,
    contentPadding: PaddingValues,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    onSongMenu: (Song) -> Unit,
    onBrowseLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm)
        ) {
            Text(text = "Favorites", style = AuralisType.display, color = colors.textPrimary)
            Text(
                text = if (favorites.isEmpty()) "Nothing saved yet"
                else "${favorites.size} tracks • ${Formatters.longDuration(favorites.sumOf { it.durationMs })}",
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )
        }

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "No favorites",
                    message = "Tap the heart on any track to keep it here for quick access.",
                    actionLabel = "Browse library",
                    onAction = onBrowseLibrary
                )
            }
            return
        }

        Row(
            modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            AuralisChip(label = "Play", selected = true, onClick = { onPlay(favorites, 0) }, leadingIcon = Icons.Rounded.PlayArrow)
            AuralisChip(label = "Shuffle", selected = false, onClick = { onShuffle(favorites) }, leadingIcon = Icons.Rounded.Shuffle)
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            items(favorites, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onPlay(favorites, favorites.indexOf(song)) },
                    onMenu = { onSongMenu(song) },
                    isPlaying = song.id == currentSongId,
                    modifier = Modifier.padding(horizontal = spacing.sm)
                )
            }
        }
    }
}
