package com.auralis.player.ui.screens.stats

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.ListeningStats
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.SectionHeader
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@Composable
fun StatsScreen(
    stats: ListeningStats,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
            Text(
                text = "Statistics",
                style = AuralisType.display,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).padding(start = spacing.sm)
            )
        }

        LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                ) {
                    StatTile("Tracks", stats.totalSongs.toString(), Modifier.weight(1f))
                    StatTile("Listened", Formatters.longDuration(stats.totalListeningMs), Modifier.weight(1f))
                    StatTile("Plays", stats.history.size.toString(), Modifier.weight(1f))
                }
            }

            if (stats.dailyMinutes.isNotEmpty()) {
                item { SectionHeader(title = "Last 14 days") }
                item {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md)
                    ) {
                        BarChart(
                            values = stats.dailyMinutes.map { it.second.toInt() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .padding(spacing.md)
                        )
                    }
                }
            }

            if (stats.topSongs.isNotEmpty()) {
                item { SectionHeader(title = "Most played songs") }
                items(stats.topSongs.size) { index ->
                    val song = stats.topSongs[index].first
                    val playCount = stats.topSongs[index].second
                    RankRow(
                        rank = index + 1,
                        artworkSongId = song.id,
                        title = song.title,
                        subtitle = "${song.displayArtist} • $playCount plays"
                    )
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item { SectionHeader(title = "Top artists") }
                items(stats.topArtists.size) { index ->
                    val artist = stats.topArtists[index]
                    RankRow(
                        rank = index + 1,
                        artworkSongId = 0L,
                        title = artist.first,
                        subtitle = "${artist.second} tracks"
                    )
                }
            }

            if (stats.topAlbums.isNotEmpty()) {
                item { SectionHeader(title = "Top albums") }
                items(stats.topAlbums.size) { index ->
                    val album = stats.topAlbums[index]
                    RankRow(
                        rank = index + 1,
                        artworkSongId = 0L,
                        title = album.first,
                        subtitle = "${album.second}"
                    )
                }
            }

            if (stats.topGenres.isNotEmpty()) {
                item { SectionHeader(title = "Top genres") }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = spacing.screen),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        val max = stats.topGenres.maxOf { it.second }.coerceAtLeast(1)
                        stats.topGenres.forEach { genre ->
                            Column {
                                Text(
                                    "${genre.first} • ${genre.second}",
                                    style = AuralisType.bodySmall,
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(genre.second.toFloat() / max)
                                        .height(8.dp)
                                        .padding(top = 2.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRoundRect(
                                            color = colors.accent,
                                            cornerRadius = CornerRadius(6f, 6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (stats.history.isNotEmpty()) {
                item { SectionHeader(title = "Listening history") }
                items(stats.history.size) { index ->
                    val entry = stats.history[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.screen, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = AuralisType.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.artist, style = AuralisType.bodySmall, color = colors.textSecondary, maxLines = 1)
                        }
                        Text(
                            Formatters.duration(entry.playedMs),
                            style = AuralisType.numeric,
                            color = colors.textTertiary
                        )
                    }
                }
                item {
                    Box(modifier = Modifier.padding(horizontal = spacing.screen, vertical = spacing.md)) {
                        AuralisChip(label = "Clear history", selected = false, onClick = onClearHistory)
                    }
                }
            }

            if (stats.totalSongs == 0) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Insights,
                        title = "No statistics yet",
                        message = "Play a few tracks and your listening trends will show up here."
                    )
                }
            }

            item { Box(modifier = Modifier.padding(spacing.xl)) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    GlassPanel(modifier = modifier, accentWash = true) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AuralisTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = AuralisType.headline, color = colors.textPrimary, maxLines = 1)
            Text(label, style = AuralisType.overline, color = colors.textTertiary)
        }
    }
}

@Composable
private fun RankRow(rank: Int, artworkSongId: Long, title: String, subtitle: String) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AuralisTheme.spacing.screen, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)
    ) {
        Text("$rank", style = AuralisType.numeric, color = colors.accent)
        SongArtwork(songId = artworkSongId, modifier = Modifier.size(44.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AuralisType.body, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = AuralisType.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val accent = AuralisTheme.colors.accent
    val muted = AuralisTheme.colors.surfaceMuted
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val gap = size.width * 0.02f / values.size.coerceAtLeast(1)
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / max
            val barHeight = size.height * ratio.coerceIn(0.02f, 1f)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = muted,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
