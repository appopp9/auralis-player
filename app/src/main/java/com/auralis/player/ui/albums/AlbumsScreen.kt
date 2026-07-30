package com.auralis.player.ui.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.auralis.player.domain.model.Album
import com.auralis.player.ui.theme.GoldAccent

@Composable
fun AlbumsScreen(
    onAlbumClick: (Long) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldAccent.Surface)
    ) {
        if (albums.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No albums found",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldAccent.TextSecondary
                    )
                }
            }
        } else {
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = albums,
                    key = { it.id }
                ) { album ->
                    AnimatedAlbumCard(
                        album = album,
                        onClick = { onAlbumClick(album.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedAlbumCard(album: Album, onClick: () -> Unit) {
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
        AlbumCard(album = album, onClick = onClick)
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GoldAccent.SurfaceCard)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Square artwork with rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = album.artworkUri,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle gradient at bottom for depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Album name
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GoldAccent.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Artist name
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = GoldAccent.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Track count
        Text(
            text = "${album.songCount} tracks",
            style = MaterialTheme.typography.labelSmall,
            color = GoldAccent.TextTertiary
        )
    }
}
