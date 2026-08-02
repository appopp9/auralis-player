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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.Playlist
import com.auralis.player.presentation.QuickCollection
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisTextField
import com.auralis.player.ui.components.ConfirmDialog
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle

@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    smartCounts: Map<QuickCollection, Int>,
    smartPlaylists: List<Pair<com.auralis.player.domain.model.SmartPlaylist, Int>>,
    contentPadding: PaddingValues,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmart: (QuickCollection) -> Unit,
    onOpenSmartPlaylist: (Long) -> Unit,
    onEditSmartPlaylist: (Long) -> Unit,
    onDeleteSmartPlaylist: (Long) -> Unit,
    onCreateSmartPlaylist: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteTarget by remember { mutableStateOf<Playlist?>(null) }
    var deleteSmartTarget by remember {
        mutableStateOf<com.auralis.player.domain.model.SmartPlaylist?>(null)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Playlists", style = AuralisTheme.style.display(AuralisType.display), color = colors.textPrimary, modifier = Modifier.weight(1f))
            AccentIconButton(Icons.Rounded.Add, "New playlist", filled = true) { showCreate = true }
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            item { SectionHeader(title = "Smart playlists") }
            items(QuickCollection.entries.toList()) { smart ->
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

            item {
                SectionHeader(
                    title = "Rule based",
                    actionLabel = "New",
                    onAction = onCreateSmartPlaylist
                )
            }

            if (smartPlaylists.isEmpty()) {
                item {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.xs)
                    ) {
                        Text(
                            text = "Build a playlist that fills itself — by artist, year, play count, mood and more.",
                            style = AuralisType.bodySmall,
                            color = colors.textSecondary
                        )
                        com.auralis.player.ui.components.AuralisChip(
                            label = "Create smart playlist",
                            selected = true,
                            modifier = Modifier.padding(top = spacing.sm),
                            onClick = onCreateSmartPlaylist
                        )
                    }
                }
            }

            // Keys must be unique across the WHOLE list, not just within this
            // block: smart playlist #1 and normal playlist #1 are different
            // rows with the same numeric id, and an unprefixed key made
            // LazyColumn throw the moment both existed.
            items(
                items = smartPlaylists,
                key = { "smart-${it.first.id}" },
                contentType = { "smart-playlist" }
            ) { (smart, trackCount) ->
                SmartPlaylistRow(
                    name = smart.name,
                    trackCount = trackCount,
                    onOpen = { onOpenSmartPlaylist(smart.id) },
                    onEdit = { onEditSmartPlaylist(smart.id) },
                    onDelete = { deleteSmartTarget = smart },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = 3.dp)
                )
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

            items(
                items = playlists,
                key = { "playlist-${it.id}" },
                contentType = { "playlist" }
            ) { playlist ->
                PressableSurface(
                    onClick = { onOpenPlaylist(playlist.id) },
                    onLongClick = { renameTarget = playlist },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        Box {
                            SongArtwork(songId = playlist.artworkSongId, modifier = Modifier.size(54.dp))
                            if (playlist.pinned) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(18.dp)
                                        .background(colors.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = "Pinned",
                                        tint = colors.onAccent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = localizedStyle(AuralisType.body, playlist.name),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.songCount} tracks" + if (playlist.pinned) " • pinned" else "",
                                style = AuralisType.bodySmall,
                                color = if (playlist.pinned) colors.accent else colors.textSecondary
                            )
                        }
                        AccentIconButton(
                            icon = Icons.Rounded.PushPin,
                            contentDescription = if (playlist.pinned) "Unpin ${playlist.name}" else "Pin ${playlist.name}",
                            size = 38.dp,
                            tint = if (playlist.pinned) colors.accent else colors.textTertiary
                        ) { onTogglePin(playlist.id) }
                        AccentIconButton(Icons.Rounded.Edit, "Rename ${playlist.name}", size = 38.dp) {
                            renameTarget = playlist
                        }
                        AccentIconButton(
                            Icons.Rounded.Delete,
                            "Delete ${playlist.name}",
                            size = 38.dp,
                            tint = colors.danger
                        ) { deleteTarget = playlist }
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
                onCreate(name)
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
                onRename(target.id, name)
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteSmartTarget?.let { target ->
        ConfirmDialog(
            icon = Icons.Rounded.DeleteOutline,
            title = "Delete smart playlist",
            message = "\"${target.name}\" and its rules will be removed. " +
                "The tracks it matched stay in your library.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = {
                onDeleteSmartPlaylist(target.id)
                deleteSmartTarget = null
            },
            onDismiss = { deleteSmartTarget = null }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            icon = Icons.Rounded.DeleteOutline,
            title = "Delete playlist",
            message = "\"${target.name}\" will be removed. Your audio files are not affected.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            onConfirm = {
                onDelete(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
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
    var value by rememberSaveable { mutableStateOf(initial) }
    var attempted by remember { mutableStateOf(false) }
    val trimmed = value.trim()
    val isError = attempted && trimmed.isEmpty()

    com.auralis.player.ui.components.AuralisDialog(onDismiss = onDismiss) { dismiss ->
        val colors = AuralisTheme.colors
        val spacing = AuralisTheme.spacing
        Text(
            text = title,
            style = AuralisType.title,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        AuralisTextField(
            value = value,
            onValueChange = {
                if (it.length <= 60) value = it
            },
            placeholder = "Name",
            isError = isError,
            errorText = if (isError) "A name is required" else null,
            helperText = "${trimmed.length}/60",
            modifier = Modifier.padding(top = spacing.md)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.End)
        ) {
            com.auralis.player.ui.components.AuralisChip(
                label = "Cancel",
                selected = false,
                onClick = { dismiss() }
            )
            com.auralis.player.ui.components.AuralisChip(
                label = confirmLabel,
                selected = true,
                onClick = {
                    if (trimmed.isNotEmpty()) {
                        onConfirm(trimmed)
                        dismiss()
                    } else {
                        attempted = true
                    }
                }
            )
        }
    }
}
