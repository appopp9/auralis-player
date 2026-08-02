package com.auralis.player.ui.screens.stats

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.WrappedData
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.appear
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle

/**
 * Auralis Wrapped — a year-in-review presented as a series of big, animated
 * hero panels (Spotify-Wrapped style) driven entirely by the local play
 * history.
 */
@Composable
fun WrappedScreen(
    wrapped: WrappedData?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.accent.copy(alpha = if (colors.isDark) 0.28f else 0.16f),
                        colors.background
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back") { onBack() }
            Text(
                "Wrapped",
                style = AuralisTheme.style.display(AuralisType.title),
                color = colors.textPrimary,
                modifier = Modifier.padding(start = spacing.sm)
            )
        }

        if (wrapped == null) {
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Nothing to wrap yet",
                    message = "Listen to some music this year and your personal Wrapped will appear here."
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screen, vertical = spacing.lg)
                        .appear(0),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        "Your ${wrapped.year} Wrapped",
                        style = AuralisTheme.style.display(AuralisType.display),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "A look back at your year in music",
                        style = AuralisType.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                HeroPanel(index = 1, title = "You listened for") {
                    Text(
                        Formatters.longDuration(wrapped.totalMs),
                        style = AuralisType.display,
                        color = colors.accent
                    )
                    Text(
                        "across ${wrapped.totalPlays} plays this year",
                        style = AuralisType.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            wrapped.topSong?.let { song ->
                item {
                    HeroPanel(index = 2, title = "Your top track") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.md)
                        ) {
                            SongArtwork(songId = song.id, modifier = Modifier.size(76.dp), shape = AuralisTheme.shapes.card)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    style = localizedStyle(AuralisType.headline, song.title),
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.displayArtist,
                                    style = localizedStyle(AuralisType.body, song.displayArtist),
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${wrapped.topSongPlays} plays",
                                    style = AuralisType.label,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }

            wrapped.topArtist?.let { artist ->
                item {
                    HeroPanel(index = 3, title = "Your top artist", icon = Icons.Rounded.Person) {
                        Text(
                            artist,
                            style = localizedStyle(AuralisType.display, artist),
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${wrapped.topArtistPlays} plays",
                            style = AuralisType.bodySmall,
                            color = colors.accent
                        )
                    }
                }
            }

            if (wrapped.topAlbum != null || wrapped.topGenre != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.screen)
                            .appear(4),
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        wrapped.topAlbum?.let { album ->
                            MiniPanel(
                                icon = Icons.Rounded.Album,
                                label = "Top album",
                                value = album,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        wrapped.topGenre?.let { genre ->
                            MiniPanel(
                                icon = Icons.Rounded.Category,
                                label = "Top genre",
                                value = genre,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (wrapped.topSongs.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = spacing.screen).appear(5)) {
                        Text("Your top tracks", style = AuralisType.title, color = colors.textPrimary)
                        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = spacing.sm)) {
                            Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                wrapped.topSongs.forEachIndexed { index, (song, plays) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                        Text("${index + 1}", style = AuralisType.numeric, color = colors.accent)
                                        SongArtwork(songId = song.id, modifier = Modifier.size(40.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(song.title, style = localizedStyle(AuralisType.body, song.title), color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(song.displayArtist, style = localizedStyle(AuralisType.bodySmall, song.displayArtist), color = colors.textSecondary, maxLines = 1)
                                        }
                                        Text("$plays×", style = AuralisType.label, color = colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (wrapped.topArtists.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = spacing.screen).appear(6)) {
                        Text("Your top artists", style = AuralisType.title, color = colors.textPrimary)
                        GlassPanel(modifier = Modifier.fillMaxWidth().padding(top = spacing.sm)) {
                            Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                wrapped.topArtists.forEachIndexed { index, (artist, plays) ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                        Text("${index + 1}", style = AuralisType.numeric, color = colors.accent)
                                        Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
                                        Text(
                                            artist,
                                            style = localizedStyle(AuralisType.body, artist),
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text("$plays×", style = AuralisType.label, color = colors.textTertiary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Box(modifier = Modifier.padding(spacing.xl)) }
        }
    }
}

@Composable
private fun HeroPanel(
    index: Int,
    title: String,
    icon: ImageVector = Icons.Rounded.MusicNote,
    content: @Composable () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    GlassPanel(
        accentWash = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screen)
            .appear(index)
    ) {
        Column(modifier = Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                Text(title, style = AuralisType.overline, color = colors.textTertiary)
            }
            content()
        }
    }
}

@Composable
private fun MiniPanel(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Text(value, style = localizedStyle(AuralisType.title, value), color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = AuralisType.overline, color = colors.textTertiary)
        }
    }
}
