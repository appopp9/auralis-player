package com.auralis.player.ui.playlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.theme.GoldAccent

@Composable
fun SmartPlaylistDetailScreen(
    type: String,
    onBack: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val songs by when (type) {
        "favorites" -> viewModel.favorites.collectAsStateWithLifecycle()
        "recently_played" -> viewModel.recentlyPlayed.collectAsStateWithLifecycle()
        "most_played" -> viewModel.mostPlayed.collectAsStateWithLifecycle()
        else -> viewModel.favorites.collectAsStateWithLifecycle()
    }

    val title = when (type) {
        "favorites" -> "Favorites"
        "recently_played" -> "Recently Played"
        "most_played" -> "Most Played"
        else -> "Smart Playlist"
    }

    val icon = when (type) {
        "favorites" -> Icons.Default.Favorite
        "recently_played" -> Icons.Default.History
        "most_played" -> Icons.Default.TrendingUp
        else -> Icons.Default.Favorite
    }

    val gradientColors = when (type) {
        "favorites" -> listOf(Color(0xFF8E24AA), Color(0xFFE91E63))
        "recently_played" -> listOf(Color(0xFF1565C0), Color(0xFF00BCD4))
        "most_played" -> listOf(GoldAccent.PrimaryDark, GoldAccent.Primary)
        else -> listOf(GoldAccent.Primary, GoldAccent.Primary)
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
            // ── Header ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = gradientColors.map { it.copy(alpha = 0.6f) } + GoldAccent.Surface
                                )
                            )
                    )

                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Icon and info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${songs.size} songs",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Play / Shuffle buttons ─────────────────────────────────
            if (songs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.playSongList(songs) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent.Primary,
                                contentColor = GoldAccent.OnPrimary
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.playSongList(songs.shuffled()) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = GoldAccent.Primary
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(GoldAccent.Primary, GoldAccent.PrimaryDark))
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Shuffle", fontWeight = FontWeight.Medium)
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        color = GoldAccent.Divider
                    )
                }
            }

            // ── Song list ───────────────────────────────────────────────
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = GoldAccent.TextTertiary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No songs here yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GoldAccent.TextTertiary
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    SmartPlaylistSongRow(
                        trackNumber = index + 1,
                        song = song,
                        onClick = { viewModel.playSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistSongRow(trackNumber: Int, song: Song, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number
        Text(
            text = "%02d".format(trackNumber),
            style = MaterialTheme.typography.labelMedium,
            color = GoldAccent.TextTertiary,
            modifier = Modifier.width(28.dp)
        )

        // Artwork
        AsyncImage(
            model = song.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(GoldAccent.SurfaceCard)
        )

        // Song info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = GoldAccent.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.displayArtist} • ${song.displayAlbum}",
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Play count for most played
        if (song.playCount > 0) {
            Text(
                text = "${song.playCount}",
                style = MaterialTheme.typography.labelMedium,
                color = GoldAccent.TextTertiary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Duration
        Text(
            text = formatDurationSmart(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = GoldAccent.TextTertiary
        )
    }
}

private fun formatDurationSmart(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
