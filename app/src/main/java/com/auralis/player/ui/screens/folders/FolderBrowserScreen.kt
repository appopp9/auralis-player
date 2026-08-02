package com.auralis.player.ui.screens.folders

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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
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
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.screens.playlists.TextPromptDialog
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import java.io.File

/**
 * Internal file browser backed by java.io.File. Every action is real:
 * navigation, playback of a folder, exclusion and folder creation.
 */
@Composable
fun FolderBrowserScreen(
    roots: List<Pair<String, String>>,
    excludedFolders: List<String>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlayFolder: (String) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onIncludeFolder: (String) -> Unit,
    onOpenLibraryFolder: (String) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    var current by remember { mutableStateOf(roots.firstOrNull()?.second ?: "/storage/emulated/0") }
    var createDialog by remember { mutableStateOf(false) }

    val currentFile = File(current)
    val children = remember(current, excludedFolders) {
        runCatching {
            currentFile.listFiles()
                ?.filter { !it.isHidden }
                ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
                ?: emptyList()
        }.getOrDefault(emptyList())
    }
    val audioChildren = children.filter { !it.isDirectory && isAudio(it.name) }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back") { onBack() }
            Column(modifier = Modifier.weight(1f).padding(start = spacing.sm)) {
                Text("Folders", style = AuralisType.title, color = colors.textPrimary)
                Text(current, style = AuralisType.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AccentIconButton(Icons.Rounded.CreateNewFolder, "Create folder") { createDialog = true }
        }

        Row(
            modifier = Modifier.padding(horizontal = spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            roots.forEach { (label, path) ->
                AuralisChip(label = label, selected = current == path, onClick = { current = path })
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            if (audioChildren.isNotEmpty()) {
                AuralisChip(label = "Play folder", selected = true) { onPlayFolder(current) }
                AuralisChip(label = "Open in library", selected = false) { onOpenLibraryFolder(current) }
            }
            if (excludedFolders.contains(current)) {
                AuralisChip(label = "Include again", selected = false) { onIncludeFolder(current) }
            } else {
                AuralisChip(label = "Exclude", selected = false) { onExcludeFolder(current) }
            }
        }

        if (children.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.Folder,
                    title = "Empty folder",
                    message = "There is nothing readable in this location."
                )
            }
            return
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            currentFile.parentFile?.let { parent ->
                item {
                    EntryRow(
                        icon = Icons.Rounded.ArrowUpward,
                        title = "..",
                        subtitle = parent.absolutePath,
                        excluded = false
                    ) { current = parent.absolutePath }
                }
            }
            items(children, key = { it.absolutePath }) { file ->
                if (file.isDirectory) {
                    EntryRow(
                        icon = Icons.Rounded.Folder,
                        title = file.name,
                        subtitle = "${runCatching { file.list()?.size ?: 0 }.getOrDefault(0)} items",
                        excluded = excludedFolders.contains(file.absolutePath)
                    ) { current = file.absolutePath }
                } else if (isAudio(file.name)) {
                    EntryRow(
                        icon = Icons.Rounded.MusicNote,
                        title = file.name,
                        subtitle = "${file.length() / 1024} KB",
                        excluded = false
                    ) { onPlayFolder(current) }
                }
            }
        }
    }

    if (createDialog) {
        TextPromptDialog(
            title = "Create folder",
            initial = "New folder",
            confirmLabel = "Create",
            onConfirm = { name ->
                createDialog = false
                if (name.isNotBlank()) {
                    val created = runCatching { File(currentFile, name).mkdirs() }.getOrDefault(false)
                    onMessage(if (created) "Folder created" else "Could not create folder here")
                    if (created) current = File(currentFile, name).absolutePath
                }
            },
            onDismiss = { createDialog = false }
        )
    }
}

@Composable
private fun EntryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    excluded: Boolean,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    PressableSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = AuralisTheme.spacing.md, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AuralisTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AuralisType.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = if (excluded) "$subtitle • excluded" else subtitle,
                    style = AuralisType.bodySmall,
                    color = if (excluded) colors.danger else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun isAudio(name: String): Boolean {
    val lower = name.lowercase()
    return listOf(".mp3", ".flac", ".wav", ".aac", ".ogg", ".m4a", ".opus", ".aiff", ".aif", ".wma")
        .any { lower.endsWith(it) }
}
