package com.auralis.player.ui.playlists

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.auralis.player.domain.model.Playlist
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.theme.GoldAccent

@Composable
fun PlaylistsScreen(
    onPlaylistClick: (Long) -> Unit,
    onSmartPlaylistClick: (String) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val mostPlayed by viewModel.mostPlayed.collectAsStateWithLifecycle()
    val showCreateDialog by viewModel.showCreateDialog.collectAsStateWithLifecycle()

    // Create playlist dialog
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { viewModel.dismissCreateDialog() },
            onConfirm = { name -> viewModel.createPlaylist(name) }
        )
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
            // ── Smart Playlists Section ────────────────────────────────
            item {
                SectionHeader("Smart Playlists")
            }

            item {
                SmartPlaylistCard(
                    title = "Favorites",
                    subtitle = "${favorites.size} songs",
                    icon = Icons.Default.Favorite,
                    gradientColors = listOf(Color(0xFF8E24AA), Color(0xFFE91E63)),
                    onClick = { onSmartPlaylistClick("favorites") }
                )
            }

            item {
                SmartPlaylistCard(
                    title = "Recently Played",
                    subtitle = "${recentlyPlayed.size} songs",
                    icon = Icons.Default.History,
                    gradientColors = listOf(Color(0xFF1565C0), Color(0xFF00BCD4)),
                    onClick = { onSmartPlaylistClick("recently_played") }
                )
            }

            item {
                SmartPlaylistCard(
                    title = "Most Played",
                    subtitle = "${mostPlayed.size} songs",
                    icon = Icons.Default.TrendingUp,
                    gradientColors = listOf(GoldAccent.PrimaryDark, GoldAccent.Primary),
                    onClick = { onSmartPlaylistClick("most_played") }
                )
            }

            // ── User Playlists Section ─────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Your Playlists")
            }

            // Create new playlist card
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(durationMillis = 200))
                ) {
                    CreatePlaylistCard(onClick = { viewModel.showCreateDialog() })
                }
            }

            // Playlist cards
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                AnimatedPlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) }
                )
            }

            if (playlists.isEmpty()) {
                item {
                    Text(
                        text = "No playlists yet. Create one above!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldAccent.TextTertiary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = GoldAccent.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    colors = gradientColors.map { it.copy(alpha = 0.15f) }
                )
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GoldAccent.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent.TextSecondary
            )
        }

        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = null,
            tint = gradientColors.last().copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GoldAccent.SurfaceCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gold + icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GoldAccent.Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create playlist",
                tint = GoldAccent.Primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = "Create new playlist",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = GoldAccent.Primary
        )
    }
}

@Composable
private fun AnimatedPlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = true

    AnimatedVisibility(
        visibleState = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 250),
                    initialOffsetY = { it / 8 }
                )
    ) {
        PlaylistCard(playlist = playlist, onClick = onClick)
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GoldAccent.SurfaceCard)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork thumbnail with gradient overlay
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = playlist.artworkUri,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay on artwork
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = GoldAccent.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent.TextSecondary
            )
        }

        // Trailing artwork indicator
        if (playlist.artworkSongId > 0L) {
            AsyncImage(
                model = playlist.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GoldAccent.SurfaceElevated,
        titleContentColor = GoldAccent.TextPrimary,
        textContentColor = GoldAccent.TextSecondary,
        title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text("Playlist name", color = GoldAccent.TextTertiary)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GoldAccent.TextPrimary,
                    unfocusedTextColor = GoldAccent.TextPrimary,
                    focusedBorderColor = GoldAccent.Primary,
                    unfocusedBorderColor = GoldAccent.Divider,
                    cursorColor = GoldAccent.Primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GoldAccent.Primary
                )
            ) {
                Text("Create", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = GoldAccent.TextSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
