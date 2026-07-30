package com.auralis.player.ui.artists

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.auralis.player.domain.model.Artist
import com.auralis.player.ui.theme.GoldAccent

@Composable
fun ArtistsScreen(
    onArtistClick: (String) -> Unit,
    viewModel: ArtistsViewModel = hiltViewModel()
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldAccent.Surface)
    ) {
        if (artists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No artists found",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldAccent.TextSecondary
                )
            }
        } else {
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = artists,
                    key = { it.name }
                ) { artist ->
                    AnimatedArtistCard(
                        artist = artist,
                        onClick = { onArtistClick(artist.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedArtistCard(artist: Artist, onClick: () -> Unit) {
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
        ArtistCard(artist = artist, onClick = onClick)
    }
}

@Composable
private fun ArtistCard(artist: Artist, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular artist image with gold ring
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Gold border ring
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(
                                GoldAccent.Primary,
                                GoldAccent.PrimaryLight,
                                GoldAccent.Primary,
                                GoldAccent.PrimaryDark,
                                GoldAccent.Primary
                            )
                        )
                    )
            )

            // Inner circular image
            AsyncImage(
                model = artist.artworkUri,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GoldAccent.SurfaceCard)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Artist name
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = GoldAccent.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Song count
        Text(
            text = "${artist.songCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = GoldAccent.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
