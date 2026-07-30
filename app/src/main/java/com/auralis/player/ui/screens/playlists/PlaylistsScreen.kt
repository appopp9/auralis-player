package com.auralis.player.ui.screens.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.Playlist
import com.auralis.player.presentation.SmartPlaylist
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    smartCounts: Map<SmartPlaylist, Int>,
    contentPadding: PaddingValues,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmart: (SmartPlaylist) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Playlists", style = AuralisType.display, color = colors.textPrimary, modifier = Modifier.weight(1f))
            AccentIconButton(Icons.Rounded.Add, "New playlist", onClick = { showCreate = true }, filled = true)
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            item { SectionHeader(title = "Smart playlists") }
            items(SmartPlaylist.entries.toList()) { smart ->
                PressableSurface(
                    onClick = { onOpenSmart(smart) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = 3.dp)
                ) {
                    GlassPanel(accentWash = true, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = smart.title, style = AuralisType.body, color = colors.textPrimary)
                                Text(
                                    text = "${smartCounts[smart] ?: 0} tracks",
                                    style = AuralisType.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Your playlists") }

            if (playlists.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.QueueMusic,
                        title = "No playlists yet",
                        message = "Create your first playlist to group tracks the way you like.",
                        actionLabel = "Create playlist",
                        onAction = { showCreate = true }
                    )
                }
            }

            items(playlists, key = { it.id }) { playlist ->
                PressableSurface(
                    onClick = { onOpenPlaylist(playlist.id) },
                    onLongClick = { renameTarget = playlist },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        SongArtwork(songId = playlist.artworkSongId, modifier = Modifier.size(54.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = AuralisType.body,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.songCount} tracks",
                                style = AuralisType.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        AccentIconButton(Icons.Rounded.Edit, "Rename ${playlist.name}", onClick = {
                            renameTarget = playlist
                        }, size = 38.dp)
                        AccentIconButton(
                            Icons.Rounded.Delete,
                            "Delete ${playlist.name}",
                            onClick = { deleteTarget = playlist },
                            size = 38.dp,
                            tint = colors.danger
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        TextPromptDialog(
            title = "New playlist",
            initial = "",
            confirmLabel = "Create",
            onConfirm = { name ->
                showCreate = false
                if (name.isNotBlank()) onCreate(name)
            },
            onDismiss = { showCreate = false }
        )
    }

    renameTarget?.let { target ->
        TextPromptDialog(
            title = "Rename playlist",
            initial = target.name,
            confirmLabel = "Save",
            onConfirm = { name ->
                renameTarget = null
                if (name.isNotBlank()) onRename(target.id, name)
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete playlist") },
            text = { Text("\"${target.name}\" will be removed. Your audio files are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    onDelete(target.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun TextPromptDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Name") }
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.trim()) }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
