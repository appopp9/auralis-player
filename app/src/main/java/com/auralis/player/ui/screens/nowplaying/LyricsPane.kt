package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.player.data.lyrics.LyricsParser
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.domain.model.Song
import com.auralis.player.presentation.LyricsDownloadState
import androidx.compose.foundation.clickable
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.EmptyState
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.localizedStyle
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Transparent full-screen touch shield. Consumes every down/move/up so the
 * gesture detectors on the player beneath never fire while lyrics are open.
 * It sits as the bottom layer, so interactive children on top (list, buttons,
 * lyric lines) keep working normally.
 */
private fun Modifier.consumeAllTouches(): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false).consume()
        do {
            val event = awaitPointerEvent()
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

private fun lyricsTextAlign(align: Int): TextAlign = when (align) {
    0 -> TextAlign.Start
    2 -> TextAlign.End
    else -> TextAlign.Center
}

/**
 * Full-screen lyrics stage: dual (original + translation) karaoke-style lines,
 * live-appearance settings, precise download feedback and a hard touch shield.
 */
@Composable
fun LyricsPane(
    lyrics: Lyrics?,
    loading: Boolean,
    downloadState: LyricsDownloadState,
    settings: AppSettings,
    activeIndex: Int,
    title: String,
    artist: String,
    currentSong: Song?,
    onBack: () -> Unit,
    onDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeekToLine: (Long) -> Unit,
    onConsumeFeedback: () -> Unit,
    lyricsOffsetMs: Int,
    onAdjustOffset: (Int) -> Unit,
    onImport: () -> Unit,
    onEditLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val listState = rememberLazyListState()
    var showShareSheet by remember { mutableStateOf(false) }

    // Smoothly keep the active line vertically centered.
    LaunchedEffect(activeIndex, lyrics?.lines?.size) {
        if (activeIndex >= 0 && lyrics?.synchronized == true) {
            val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            if (viewport > 0) {
                runCatching {
                    listState.animateScrollToItem(
                        index = maxOf(0, activeIndex),
                        scrollOffset = -(viewport / 2 - 120)
                    )
                }
            }
        }
    }

    // Transient feedback banner (offline / saved / not-found / failed).
    val feedbackText = when (val s = downloadState) {
        is LyricsDownloadState.Saved -> if (s.justNow) "Lyrics saved for offline use" else null
        LyricsDownloadState.Offline -> "No internet connection"
        LyricsDownloadState.NotFound -> "No lyrics found online for this track"
        is LyricsDownloadState.Failed -> "Couldn't download lyrics"
        else -> null
    }
    LaunchedEffect(feedbackText) {
        if (feedbackText != null) {
            delay(2400)
            onConsumeFeedback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Touch shield — bottom layer, blocks the player underneath.
        Box(modifier = Modifier.fillMaxSize().consumeAllTouches())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                colors.accent.copy(alpha = if (colors.isDark) 0.20f else 0.12f),
                                colors.background.copy(alpha = 0f)
                            ),
                            endY = size.height * 0.5f
                        )
                    )
                }
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ---- header -----------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccentIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back to player") { onBack() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = localizedStyle(AuralisType.title, title, settings.lyricsPersianFont),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = localizedStyle(AuralisType.bodySmall, artist, settings.lyricsPersianFont),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AccentIconButton(Icons.Rounded.Tune, "Lyrics appearance") { onOpenSettings() }
                AccentIconButton(Icons.Rounded.Edit, "Add or edit lyrics") { onEditLyrics() }
                AccentIconButton(Icons.Rounded.Share, "Share lyrics") { showShareSheet = true }
                DownloadButton(downloadState = downloadState, hasLyrics = lyrics != null, onDownload = onDownload)
            }

            // Manual lyrics sync offset (only meaningful for synced lyrics).
            if (lyrics?.synchronized == true) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // C17 — coarse and fine nudges, live, without leaving the
                    // lyrics view. Tapping the readout resets to zero.
                    NudgeChip("-0.5s") { onAdjustOffset(-500) }
                    NudgeChip("-0.1s") { onAdjustOffset(-100) }
                    Text(
                        text = if (lyricsOffsetMs == 0) {
                            "In sync"
                        } else {
                            "${if (lyricsOffsetMs > 0) "+" else ""}${lyricsOffsetMs / 1000f}s · reset"
                        },
                        style = AuralisType.label,
                        color = if (lyricsOffsetMs == 0) colors.textSecondary else colors.accent,
                        modifier = Modifier
                            .padding(horizontal = spacing.sm)
                            .clickable(enabled = lyricsOffsetMs != 0) { onAdjustOffset(-lyricsOffsetMs) }
                    )
                    NudgeChip("+0.1s") { onAdjustOffset(100) }
                    NudgeChip("+0.5s") { onAdjustOffset(500) }
                }
            }

            AnimatedVisibility(visible = feedbackText != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = feedbackText.orEmpty(),
                    style = AuralisType.bodySmall,
                    color = if (downloadState is LyricsDownloadState.Saved) colors.success else colors.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.screen, vertical = 2.dp)
                )
            }

            // ---- body -------------------------------------------------------
            when {
                loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.accent)
                        Text(
                            "Loading lyrics…",
                            style = AuralisType.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = spacing.md)
                        )
                    }
                }

                lyrics == null || (lyrics.lines.isEmpty() && lyrics.plainText.isBlank()) -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyState(
                            icon = Icons.Rounded.Lyrics,
                            title = "No lyrics yet",
                            message = "Search online, import an .lrc/.txt file, or write the " +
                                "lyrics yourself — translations too.",
                            actionLabel = "Search online",
                            onAction = onDownload
                        )
                        Row(
                            modifier = Modifier.padding(top = spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            AuralisChip(label = "Write lyrics", selected = true) { onEditLyrics() }
                            AuralisChip(label = "Import file", selected = false) { onImport() }
                        }
                    }
                }

                lyrics.synchronized && lyrics.lines.isNotEmpty() -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = spacing.xl, vertical = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.lg)
                ) {
                    itemsIndexed(lyrics.lines, key = { index, _ -> index }) { index, line ->
                        val translation = if (settings.lyricsShowTranslation) {
                            LyricsParser.translationAt(lyrics.translationLines, line.timeMs)
                        } else {
                            null
                        }
                        LyricLineItem(
                            text = line.text,
                            translation = translation,
                            active = index == activeIndex,
                            distance = if (activeIndex < 0) 3 else abs(index - activeIndex),
                            settings = settings,
                            onClick = { onSeekToLine(line.timeMs) }
                        )
                    }
                }

                else -> UnsyncedLyrics(lyrics = lyrics, settings = settings)
            }
        }

        // Share sheet: multi-line selection with a live preview of the card.
        if (showShareSheet) {
            LyricShareSheet(
                song = currentSong,
                lyrics = lyrics,
                activeIndex = activeIndex,
                persianFont = settings.lyricsPersianFont,
                onDismiss = { showShareSheet = false }
            )
        }
    }
}

/** Small tap target used by the live lyrics sync nudges (C17). */
@Composable
private fun NudgeChip(label: String, onClick: () -> Unit) {
    AuralisChip(label = label, selected = false, onClick = onClick)
}

@Composable
private fun DownloadButton(
    downloadState: LyricsDownloadState,
    hasLyrics: Boolean,
    onDownload: () -> Unit
) {
    val colors = AuralisTheme.colors
    when {
        downloadState == LyricsDownloadState.Downloading -> Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
        }
        downloadState == LyricsDownloadState.Offline ->
            AccentIconButton(Icons.Rounded.CloudOff, "No internet connection") { onDownload() }
        hasLyrics ->
            AccentIconButton(Icons.Rounded.CloudDone, "Lyrics saved for offline use", tint = colors.accent) { onDownload() }
        else -> AccentIconButton(Icons.Rounded.CloudDownload, "Download lyrics") { onDownload() }
    }
}

@Composable
private fun LyricLineItem(
    text: String,
    translation: String?,
    active: Boolean,
    distance: Int,
    settings: AppSettings,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val fill = lyricsTextAlign(settings.lyricsAlign)

    val inactiveBase = settings.lyricsInactiveAlpha.coerceIn(0.1f, 1f)
    val color by animateColorAsState(
        targetValue = if (active) colors.accent else colors.textPrimary,
        animationSpec = motion.tweenMedium(),
        label = "lyricColor"
    )
    val alpha by animateFloatAsState(
        targetValue = when {
            active -> 1f
            distance == 1 -> (inactiveBase + 0.45f).coerceAtMost(1f)
            distance == 2 -> (inactiveBase + 0.2f).coerceAtMost(1f)
            else -> inactiveBase
        },
        animationSpec = motion.tweenMedium(),
        label = "lyricAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.05f else 1f,
        animationSpec = motion.softSpring(),
        label = "lyricScale"
    )

    val fontSize = settings.lyricsFontSize.coerceIn(14f, 40f)
    val lineHeight = fontSize * settings.lyricsLineSpacing.coerceIn(1f, 2.2f)
    val mainWeight = when {
        active && settings.lyricsBoldActive -> FontWeight.Bold
        active -> FontWeight.SemiBold
        else -> FontWeight.Medium
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .clip(AuralisTheme.shapes.small)
            .combinedClickableCompat { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = when (settings.lyricsAlign) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
        Text(
            text = text.ifBlank { "♪" },
            style = localizedStyle(
                AuralisType.headline.copy(
                    fontWeight = mainWeight,
                    fontSize = fontSize.sp,
                    lineHeight = lineHeight.sp
                ),
                text,
                settings.lyricsPersianFont
            ),
            color = color,
            textAlign = fill,
            modifier = Modifier.fillMaxWidth()
        )

        if (!translation.isNullOrBlank()) {
            Text(
                text = translation,
                style = localizedStyle(
                    AuralisType.body.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = (fontSize * settings.lyricsTranslationScale).sp,
                        lineHeight = (fontSize * settings.lyricsTranslationScale * 1.4f).sp
                    ),
                    translation,
                    true
                ),
                color = if (active) colors.textSecondary else colors.textTertiary,
                textAlign = fill,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = settings.lyricsTranslationGap.dp)
            )
        }
    }
}

@Composable
private fun UnsyncedLyrics(lyrics: Lyrics, settings: AppSettings) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val align = lyricsTextAlign(settings.lyricsAlign)
    val fontSize = settings.lyricsFontSize.coerceIn(14f, 40f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl, vertical = spacing.xxl),
        horizontalAlignment = when (settings.lyricsAlign) {
            0 -> Alignment.Start
            2 -> Alignment.End
            else -> Alignment.CenterHorizontally
        }
    ) {
        Text(
            text = lyrics.plainText,
            style = localizedStyle(
                AuralisType.title.copy(
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * settings.lyricsLineSpacing).sp
                ),
                lyrics.plainText,
                settings.lyricsPersianFont
            ),
            color = colors.textSecondary,
            textAlign = align,
            modifier = Modifier.fillMaxWidth()
        )
        if (settings.lyricsShowTranslation && !lyrics.translationPlainText.isNullOrBlank()) {
            Text(
                text = lyrics.translationPlainText,
                style = localizedStyle(
                    AuralisType.body.copy(
                        fontSize = (fontSize * settings.lyricsTranslationScale).sp,
                        lineHeight = (fontSize * settings.lyricsTranslationScale * 1.5f).sp
                    ),
                    lyrics.translationPlainText,
                    true
                ),
                color = colors.textTertiary,
                textAlign = align,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl)
            )
        }
    }
}
