package com.auralis.player.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.components.CollectionCard
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SongRow
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import java.util.Calendar

data class HomeCallbacks(
    val onPlaySong: (List<Song>, Int) -> Unit,
    val onSongMenu: (Song) -> Unit,
    val onShuffleAll: () -> Unit,
    val onOpenSearch: () -> Unit,
    val onOpenStats: () -> Unit,
    val onQuickAccess: (String) -> Unit,
    val onSeeAll: (String) -> Unit
)

@Composable
fun HomeScreen(
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    favorites: List<Song>,
    forgotten: List<Song>,
    totalSongs: Int,
    currentSongId: Long,
    scanning: Boolean,
    callbacks: HomeCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    if (totalSongs == 0 && !scanning) {
        Box(modifier = modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "No music yet",
                message = "Auralis could not find audio files on this device. Add some music or run a rescan from Settings.",
                actionLabel = "Open library",
                onAction = { callbacks.onQuickAccess("songs") }
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        item {
            Column(modifier = Modifier.statusBarsPadding().padding(horizontal = spacing.screen, vertical = spacing.md)) {
                Text(text = greeting(), style = AuralisTheme.style.display(AuralisType.display), color = colors.textPrimary)
                Text(
                    text = "$totalSongs tracks in your library",
                    style = AuralisType.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                HeroAction(
                    icon = Icons.Rounded.Shuffle,
                    label = "Shuffle all",
                    modifier = Modifier.weight(1f),
                    onClick = callbacks.onShuffleAll
                )
                HeroAction(
                    icon = Icons.Rounded.Search,
                    label = "Search",
                    modifier = Modifier.weight(1f),
                    onClick = callbacks.onOpenSearch
                )
                HeroAction(
                    icon = Icons.Rounded.Insights,
                    label = "Stats",
                    modifier = Modifier.weight(1f),
                    onClick = callbacks.onOpenStats
                )
            }
        }

        item { SectionHeader(title = "Quick access") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                items(quickAccessItems) { entry ->
                    QuickAccessChip(entry.first, entry.second) { callbacks.onQuickAccess(entry.third) }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently played",
                    actionLabel = "See all",
                    onAction = { callbacks.onSeeAll("recently_played") }
                )
            }
            item {
                HorizontalSongRail(recentlyPlayed.take(12), callbacks)
            }
        }

        if (recentlyAdded.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recently added",
                    actionLabel = "See all",
                    onAction = { callbacks.onSeeAll("recently_added") }
                )
            }
            item { HorizontalSongRail(recentlyAdded.take(12), callbacks) }
        }

        if (forgotten.isNotEmpty()) {
            item { SectionHeader(title = "Rediscover") }
            item { HorizontalSongRail(forgotten.take(12), callbacks) }
        }

        if (mostPlayed.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Most played",
                    actionLabel = "See all",
                    onAction = { callbacks.onSeeAll("most_played") }
                )
            }
            items(mostPlayed.take(5), key = { "most-${it.id}" }) { song ->
                SongRow(
                    song = song,
                    onClick = { callbacks.onPlaySong(mostPlayed, mostPlayed.indexOf(song)) },
                    onMenu = { callbacks.onSongMenu(song) },
                    isPlaying = song.id == currentSongId,
                    modifier = Modifier.padding(horizontal = spacing.sm),
                    trailingText = "${song.playCount}×"
                )
            }
        }

        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Favorites",
                    actionLabel = "See all",
                    onAction = { callbacks.onSeeAll("favorites") }
                )
            }
            item { HorizontalSongRail(favorites.take(12), callbacks) }
        }

        item { Box(modifier = Modifier.height(spacing.huge)) }
    }
}

@Composable
private fun HorizontalSongRail(songs: List<Song>, callbacks: HomeCallbacks) {
    val spacing = AuralisTheme.spacing
    LazyRow(
        contentPadding = PaddingValues(horizontal = spacing.screen),
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        items(songs, key = { it.id }) { song ->
            CollectionCard(
                title = song.title,
                subtitle = song.displayArtist,
                artworkSongId = song.id,
                onClick = { callbacks.onPlaySong(songs, songs.indexOf(song)) },
                onLongClick = { callbacks.onSongMenu(song) },
                modifier = Modifier.width(148.dp)
            )
        }
    }
}

@Composable
private fun HeroAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    PressableSurface(onClick = onClick, modifier = modifier) {
        GlassPanel(accentWash = true, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AuralisTheme.spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                Text(text = label, style = AuralisType.label, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QuickAccessChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = AuralisTheme.colors
    PressableSurface(onClick = onClick, shape = AuralisTheme.shapes.card) {
        GlassPanel {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Text(text = label, style = AuralisType.label, color = colors.textPrimary)
            }
        }
    }
}

private val quickAccessItems: List<Triple<ImageVector, String, String>> = listOf(
    Triple(Icons.Rounded.MusicNote, "Songs", "songs"),
    Triple(Icons.Rounded.Person, "Artists", "artists"),
    Triple(Icons.Rounded.Album, "Albums", "albums"),
    Triple(Icons.Rounded.Folder, "Folders", "folders"),
    Triple(Icons.Rounded.QueueMusic, "Playlists", "playlists"),
    Triple(Icons.Rounded.Category, "Genres", "genres"),
    Triple(Icons.Rounded.Mood, "Moods", "moods")
)

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night listening"
    }
}
