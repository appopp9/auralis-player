package com.auralis.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

data class SongActions(
    val onPlay: (Song) -> Unit,
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onToggleFavorite: (Song) -> Unit,
    val onViewAlbum: (Song) -> Unit,
    val onViewArtist: (Song) -> Unit,
    val onEditTags: (Song) -> Unit,
    val onSetRingtone: (Song) -> Unit,
    val onShare: (Song) -> Unit,
    val onDelete: (Song) -> Unit
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SongContextSheet(
    song: Song,
    actions: SongActions,
    onDismiss: () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = spacing.lg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screen, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                SongArtwork(songId = song.id, modifier = Modifier.size(56.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = AuralisType.title,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.displayArtist} • ${Formatters.duration(song.durationMs)}",
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            SheetDivider()

            ActionRow(Icons.Rounded.PlayArrow, "Play") { onDismiss(); actions.onPlay(song) }
            ActionRow(Icons.Rounded.SkipNext, "Play next") { onDismiss(); actions.onPlayNext(song) }
            ActionRow(Icons.Rounded.QueueMusic, "Add to queue") { onDismiss(); actions.onAddToQueue(song) }
            ActionRow(Icons.Rounded.PlaylistAdd, "Add to playlist") { onDismiss(); actions.onAddToPlaylist(song) }
            ActionRow(
                if (song.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (song.isFavorite) "Remove from favorites" else "Add to favorites"
            ) { onDismiss(); actions.onToggleFavorite(song) }

            SheetDivider()

            ActionRow(Icons.Rounded.Album, "View album") { onDismiss(); actions.onViewAlbum(song) }
            ActionRow(Icons.Rounded.Person, "View artist") { onDismiss(); actions.onViewArtist(song) }
            ActionRow(Icons.Rounded.Edit, "Edit tags") { onDismiss(); actions.onEditTags(song) }
            ActionRow(Icons.Rounded.RingVolume, "Set as ringtone") { onDismiss(); actions.onSetRingtone(song) }
            ActionRow(Icons.Rounded.Share, "Share") { onDismiss(); actions.onShare(song) }

            SheetDivider()

            ActionRow(Icons.Rounded.Delete, "Delete from device", destructive = true) {
                onDismiss(); actions.onDelete(song)
            }
        }
    }
}

@Composable
fun SheetHandle() {
    val colors = AuralisTheme.colors
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .background(colors.outline, AuralisTheme.shapes.chip)
        )
    }
}

@Composable
fun SheetDivider() {
    val colors = AuralisTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AuralisTheme.spacing.screen, vertical = AuralisTheme.spacing.xs)
            .height(1.dp)
            .background(colors.outline.copy(alpha = 0.3f))
    )
}

@Composable
fun ActionRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val tint = if (destructive) colors.danger else colors.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = AuralisTheme.spacing.screen, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.lg)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = label, style = AuralisType.body, color = tint)
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PlaylistPickerSheet(
    playlists: List<Playlist>,
    onPick: (Long) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AuralisTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = AuralisTheme.spacing.lg)
        ) {
            Text(
                text = "Add to playlist",
                style = AuralisType.headline,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = AuralisTheme.spacing.screen, vertical = AuralisTheme.spacing.sm)
            )
            ActionRow(Icons.Rounded.PlaylistAdd, "New playlist") { onCreateNew() }
            SheetDivider()
            playlists.forEach { playlist ->
                ActionRow(Icons.Rounded.QueueMusic, "${playlist.name} (${playlist.songCount})") {
                    onPick(playlist.id)
                }
            }
            if (playlists.isEmpty()) {
                Text(
                    text = "No playlists yet",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(AuralisTheme.spacing.screen)
                )
            }
        }
    }
}
