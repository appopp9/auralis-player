package com.auralis.player.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.HeatmapDay
import com.auralis.player.domain.model.ListeningStats
import com.auralis.player.domain.model.Song
import com.auralis.player.domain.model.Trend
import com.auralis.player.domain.model.TrendingSong
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.GlassPanel
import com.auralis.player.ui.components.PressableSurface
import com.auralis.player.ui.components.SectionHeader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle

@Composable
fun StatsScreen(
    stats: ListeningStats,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onPlaySongId: (Long) -> Unit,
    onOpenWrapped: () -> Unit,
    trending: List<TrendingSong>,
    duplicates: List<List<Song>>,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val hasChartData = stats.dailyMinutes.any { it.second > 0L }
    val topSongs = stats.topSongs.map { it.first }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back") { onBack() }
            Text(
                text = "Statistics",
                style = AuralisTheme.style.display(AuralisType.display),
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

            item {
                val year = Calendar.getInstance().get(Calendar.YEAR)
                PressableSurface(
                    onClick = onOpenWrapped,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AuralisTheme.shapes.card)
                            .background(Brush.linearGradient(listOf(colors.accent, colors.accentAlt)))
                            .padding(spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(26.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your $year Wrapped", style = AuralisType.title, color = colors.onAccent)
                            Text("Your year in music, beautifully summarized", style = AuralisType.bodySmall, color = colors.onAccent.copy(alpha = 0.85f))
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item { SectionHeader(title = "Last 14 days") }
            item {
                if (hasChartData) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md)
                    ) {
                        BarChart(
                            values = stats.dailyMinutes.map { it.second.toInt() },
                            labels = stats.dailyMinutes.map { it.first },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .padding(spacing.md)
                        )
                    }
                } else {
                    EmptyChartPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.md)
                    )
                }
            }

            item { SectionHeader(title = "Listening activity") }
            item {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md)
                ) {
                    ActivityHeatmap(
                        stats = stats,
                        modifier = Modifier.padding(spacing.md)
                    )
                }
            }

            if (trending.isNotEmpty()) {
                item { SectionHeader(title = "Trending this week") }
                items(trending.size) { index ->
                    val entry = trending[index]
                    val song = entry.song ?: return@items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.screen, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        SongArtwork(songId = song.id, modifier = Modifier.size(44.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                song.title,
                                style = localizedStyle(AuralisType.body, song.title),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${song.displayArtist} • ${entry.plays} plays this week",
                                style = localizedStyle(AuralisType.bodySmall, song.displayArtist),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                        TrendBadge(entry.trend)
                    }
                }
            }

            if (stats.topSongs.isNotEmpty()) {
                item { SectionHeader(title = "Most played songs") }
                items(stats.topSongs.size) { index ->
                    val (song, plays) = stats.topSongs[index]
                    RankRow(
                        rank = index + 1,
                        artworkSongId = song.id,
                        title = song.title,
                        subtitle = "${song.displayArtist} • $plays plays",
                        onClick = { onPlaySongs(topSongs, index) }
                    )
                }
            }

            if (stats.topArtists.isNotEmpty()) {
                item { SectionHeader(title = "Top artists") }
                items(stats.topArtists.size) { index ->
                    val (name, plays) = stats.topArtists[index]
                    RankRow(
                        rank = index + 1,
                        artworkSongId = 0L,
                        title = name,
                        subtitle = "$plays plays"
                    )
                }
            }

            if (stats.topAlbums.isNotEmpty()) {
                item { SectionHeader(title = "Top albums") }
                items(stats.topAlbums.size) { index ->
                    val (name, plays) = stats.topAlbums[index]
                    RankRow(
                        rank = index + 1,
                        artworkSongId = 0L,
                        title = name,
                        subtitle = "$plays plays"
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
                        stats.topGenres.forEach { (genreName, count) ->
                            Column {
                                Text(
                                    "$genreName • $count",
                                    style = localizedStyle(AuralisType.bodySmall, genreName),
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(count.toFloat() / max)
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
                            .clip(AuralisTheme.shapes.small)
                            .combinedClickableCompat { onPlaySongId(entry.songId) }
                            .padding(horizontal = spacing.screen, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                style = localizedStyle(AuralisType.body, entry.title),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                entry.artist,
                                style = localizedStyle(AuralisType.bodySmall, entry.artist),
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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

            if (duplicates.isNotEmpty()) {
                item { SectionHeader(title = "Possible duplicates") }
                items(duplicates.size) { index ->
                    val group = duplicates[index]
                    val first = group.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.screen, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md)
                    ) {
                        SongArtwork(songId = first.id, modifier = Modifier.size(44.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                first.title,
                                style = localizedStyle(AuralisType.body, first.title),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${first.displayArtist} • ${group.size} copies in library",
                                style = localizedStyle(AuralisType.bodySmall, first.displayArtist),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                        Text("${group.size}×", style = AuralisType.label, color = colors.accent)
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
private fun RankRow(
    rank: Int,
    artworkSongId: Long,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .padding(horizontal = AuralisTheme.spacing.sm)
                        .clip(AuralisTheme.shapes.small)
                        .combinedClickableCompat { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = if (onClick != null) AuralisTheme.spacing.sm else AuralisTheme.spacing.screen,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md)
    ) {
        Text("$rank", style = AuralisType.numeric, color = colors.accent)
        SongArtwork(songId = artworkSongId, modifier = Modifier.size(44.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = localizedStyle(AuralisType.body, title),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = localizedStyle(AuralisType.bodySmall, subtitle),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Elegant placeholder shown until there is real listening data to chart. */
@Composable
private fun EmptyChartPanel(modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    GlassPanel(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xl, horizontal = spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.BarChart,
                contentDescription = null,
                tint = colors.accent.copy(alpha = 0.7f),
                modifier = Modifier.size(30.dp)
            )
            Text(
                "No listening data yet",
                style = AuralisType.title,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Your last 14 days of listening will appear here once you play some music.",
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Calendar-aligned listening heatmap.
 *
 * Each column is a real week (weekday rows are consistent), future days are
 * rendered as faint outlines, and tapping any cell reveals how long that day
 * was. Streaks and averages sit above it so the grid actually means something
 * even before there is much history.
 */
@Composable
private fun ActivityHeatmap(stats: ListeningStats, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val weeks = stats.heatmapWeeks
    val maxMs = stats.heatmapMaxMs.coerceAtLeast(1L)
    var selected by remember { mutableStateOf<HeatmapDay?>(null) }

    val dayLabel = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val monthLabel = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        // ---- Summary chips ------------------------------------------------
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ActivityMetric(
                value = "${stats.currentStreakDays}",
                label = if (stats.currentStreakDays == 1) "day streak" else "day streak",
                modifier = Modifier.weight(1f)
            )
            ActivityMetric(
                value = "${stats.longestStreakDays}",
                label = "best streak",
                modifier = Modifier.weight(1f)
            )
            ActivityMetric(
                value = "${stats.activeDays}",
                label = "active days",
                modifier = Modifier.weight(1f)
            )
            ActivityMetric(
                value = Formatters.duration(stats.averageDailyMs),
                label = "daily avg",
                modifier = Modifier.weight(1f)
            )
        }

        if (weeks.isEmpty()) {
            Text(
                "Your activity grid fills in as you listen.",
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )
            return@Column
        }

        // ---- Month labels + grid -------------------------------------------
        // Today is the LAST column, so the grid opens scrolled fully to the end.
        // The user should never have to hunt for today.
        val gridScroll = rememberScrollState()
        LaunchedEffect(weeks.size) { gridScroll.scrollTo(gridScroll.maxValue) }
        Row(
            modifier = Modifier.horizontalScroll(gridScroll),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Weekday gutter — only alternate rows labelled, to stay readable.
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 16.dp, end = 2.dp)
            ) {
                val firstWeek = weeks.first()
                firstWeek.forEachIndexed { row, day ->
                    Box(modifier = Modifier.size(width = 24.dp, height = 14.dp)) {
                        if (row % 2 == 1) {
                            Text(
                                text = remember(day.dayStartMs) {
                                    SimpleDateFormat("EEE", Locale.getDefault())
                                        .format(Date(day.dayStartMs))
                                        .take(3)
                                },
                                style = AuralisType.overline,
                                color = colors.textTertiary
                            )
                        }
                    }
                }
            }

            weeks.forEachIndexed { weekIndex, week ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Month label above the first week of each month.
                    val firstDay = week.firstOrNull()
                    val previousDay = weeks.getOrNull(weekIndex - 1)?.firstOrNull()
                    val showMonth = firstDay != null && (
                        previousDay == null ||
                            monthLabel.format(Date(firstDay.dayStartMs)) !=
                            monthLabel.format(Date(previousDay.dayStartMs))
                        )
                    Box(modifier = Modifier.size(width = 14.dp, height = 14.dp)) {
                        if (showMonth && firstDay != null) {
                            Text(
                                text = monthLabel.format(Date(firstDay.dayStartMs)),
                                style = AuralisType.overline,
                                color = colors.textTertiary,
                                maxLines = 1
                            )
                        }
                    }

                    week.forEach { day ->
                        val isSelected = selected?.dayStartMs == day.dayStartMs
                        val intensity = if (day.listenedMs <= 0L) {
                            0f
                        } else {
                            (0.28f + 0.72f * (day.listenedMs.toFloat() / maxMs)).coerceIn(0f, 1f)
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        day.inFuture -> Color.Transparent
                                        day.listenedMs > 0L -> colors.accent.copy(alpha = intensity)
                                        else -> colors.surfaceMuted
                                    }
                                )
                                .then(
                                    if (day.inFuture) {
                                        Modifier.border(
                                            1.dp,
                                            colors.surfaceMuted.copy(alpha = 0.6f),
                                            RoundedCornerShape(4.dp)
                                        )
                                    } else if (isSelected) {
                                        Modifier.border(
                                            1.5.dp,
                                            colors.textPrimary,
                                            RoundedCornerShape(4.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .combinedClickableCompat(enabled = !day.inFuture) {
                                    selected = if (isSelected) null else day
                                }
                        )
                    }
                }
            }
        }

        // ---- Selection detail / legend --------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val detail = selected ?: stats.bestDay
            Text(
                text = when {
                    selected != null ->
                        "${dayLabel.format(Date(selected!!.dayStartMs))} · " +
                            if (selected!!.listenedMs > 0L) {
                                Formatters.longDuration(selected!!.listenedMs)
                            } else {
                                "nothing played"
                            }
                    detail != null && detail.listenedMs > 0L ->
                        "Best day: ${dayLabel.format(Date(detail.dayStartMs))} · " +
                            Formatters.longDuration(detail.listenedMs)
                    else -> "Tap a day to see details"
                },
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("Less", style = AuralisType.overline, color = colors.textTertiary)
                listOf(0f, 0.3f, 0.55f, 0.8f, 1f).forEach { level ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (level == 0f) colors.surfaceMuted
                                else colors.accent.copy(alpha = level)
                            )
                    )
                }
                Text("More", style = AuralisType.overline, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ActivityMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = AuralisTheme.colors
    Column(modifier = modifier) {
        Text(value, style = AuralisType.numeric, color = colors.textPrimary, maxLines = 1)
        Text(label, style = AuralisType.overline, color = colors.textTertiary, maxLines = 1)
    }
}

@Composable
private fun TrendBadge(trend: Trend) {
    val colors = AuralisTheme.colors
    when (trend) {
        Trend.UP -> Icon(Icons.Rounded.ArrowUpward, contentDescription = "Trending up", tint = colors.success, modifier = Modifier.size(18.dp))
        Trend.DOWN -> Icon(Icons.Rounded.ArrowDownward, contentDescription = "Trending down", tint = colors.danger, modifier = Modifier.size(18.dp))
        Trend.NEW -> Text("NEW", style = AuralisType.overline, color = colors.accent)
        Trend.SAME -> Icon(Icons.Rounded.Remove, contentDescription = "Same as last week", tint = colors.textTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>, modifier: Modifier = Modifier) {
    val accent = AuralisTheme.colors.accent
    val muted = AuralisTheme.colors.surfaceMuted
    val labelColor = AuralisTheme.colors.textTertiary
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val max = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val labelSpace = 14.dp.toPx()
        val chartHeight = size.height - labelSpace
        val gap = size.width * 0.02f / values.size.coerceAtLeast(1)
        val barWidth = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { index, value ->
            val ratio = value.toFloat() / max
            val x = index * (barWidth + gap)
            // track
            drawRoundRect(
                color = muted,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
            // value bar (only when there is real listening that day)
            if (value > 0) {
                val barHeight = chartHeight * ratio.coerceIn(0.04f, 1f)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}
