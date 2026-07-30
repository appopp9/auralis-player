package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@Composable
fun LyricsPane(
    lyrics: Lyrics?,
    loading: Boolean,
    activeIndex: Int,
    title: String,
    artist: String,
    onBack: () -> Unit,
    onSearchOnline: () -> Unit,
    onSeekToLine: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            runCatching { listState.animateScrollToItem(maxOf(0, activeIndex - 2)) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentIconButton(Icons.Rounded.ArrowBack, "Back to player", onBack)
            Column(modifier = Modifier.weight(1f).padding(start = spacing.sm)) {
                Text(text = title, style = AuralisType.title, color = colors.textPrimary, maxLines = 1)
                Text(text = artist, style = AuralisType.bodySmall, color = colors.textSecondary, maxLines = 1)
            }
            AccentIconButton(Icons.Rounded.CloudDownload, "Search lyrics online", onSearchOnline)
        }

        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }

            lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainText.isBlank()) -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Rounded.Lyrics,
                    title = "No lyrics",
                    message = "This track has no embedded lyrics. You can try an online lookup.",
                    actionLabel = "Search online",
                    onAction = onSearchOnline
                )
            }

            lyrics.synchronized && lyrics.lines.isNotEmpty() -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.screen, vertical = spacing.huge),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val active = index == activeIndex
                    val color by animateColorAsState(
                        if (active) colors.accent else colors.textTertiary,
                        label = "lyric-color"
                    )
                    val scale by animateFloatAsState(if (active) 1f else 0.96f, label = "lyric-scale")
                    Text(
                        text = line.text.ifBlank { "•" },
                        style = if (active) AuralisType.headline else AuralisType.title,
                        color = color,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .alpha(if (active) 1f else 0.75f)
                            .clickable(onClickLabel = "Seek to lyric line") { onSeekToLine(line.timeMs) }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            else -> Text(
                text = lyrics.plainText,
                style = AuralisType.body,
                color = colors.textSecondary,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screen, vertical = spacing.lg)
            )
        }
    }
}
