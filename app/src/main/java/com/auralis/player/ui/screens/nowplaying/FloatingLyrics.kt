package com.auralis.player.ui.screens.nowplaying

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.data.lyrics.LyricsParser
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.playback.PlaybackPosition
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.theme.AuralisMotion
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.localizedStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * SoundCloud-style floating synced lyric line. The current line floats over the
 * player and, as the song advances, each line slides/fades out while the next
 * one rises in — a continuous come-and-go karaoke strip. Tapping opens the
 * full lyrics stage. The 4 Hz position is collected only while this layer is
 * composed, so it costs nothing when hidden.
 */
@Composable
fun FloatingLyricsLayer(
    lyrics: Lyrics?,
    positionFlow: StateFlow<PlaybackPosition>,
    settings: AppSettings,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics == null || !lyrics.synchronized || lyrics.lines.isEmpty()) return

    val colors = AuralisTheme.colors
    val position by positionFlow.collectAsStateWithLifecycle()

    val activeIndex = remember(lyrics, position.positionMs) {
        var idx = -1
        val lines = lyrics.lines
        for (i in lines.indices) {
            if (lines[i].timeMs <= position.positionMs) idx = i else break
        }
        idx
    }
    if (activeIndex < 0) return

    val activeTime = lyrics.lines[activeIndex].timeMs
    val translation = if (settings.lyricsShowTranslation) {
        LyricsParser.translationAt(lyrics.translationLines, activeTime)
    } else {
        null
    }
    val nextLine = lyrics.lines.getOrNull(activeIndex + 1)?.text?.takeIf { it.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AuralisTheme.shapes.small)
            .combinedClickableCompat { onOpenLyrics() }
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The active line, animated come-and-go on every line change.
        AnimatedContent(
            targetState = activeIndex,
            transitionSpec = {
                (slideInVertically(tween(360, easing = AuralisMotion.EmphasizedEasing)) { it / 2 } +
                    fadeIn(tween(300, easing = AuralisMotion.EmphasizedEasing)))
                    .togetherWith(
                        slideOutVertically(tween(220, easing = AuralisMotion.ExitEasing)) { -it / 2 } +
                            fadeOut(tween(180))
                    )
            },
            label = "floatingLyric"
        ) { _ ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = lyrics.lines[activeIndex].text.ifBlank { "♪" },
                    style = localizedStyle(
                        AuralisType.headline.copy(
                            fontWeight = if (settings.lyricsBoldActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = (settings.lyricsFontSize * 0.9f).coerceIn(16f, 30f).sp,
                            lineHeight = (settings.lyricsFontSize * 1.25f).sp
                        ),
                        lyrics.lines[activeIndex].text,
                        settings.lyricsPersianFont
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!translation.isNullOrBlank()) {
                    Text(
                        text = translation,
                        style = localizedStyle(
                            AuralisType.bodySmall.copy(
                                fontSize = (settings.lyricsFontSize * settings.lyricsTranslationScale * 0.8f).sp
                            ),
                            translation,
                            true
                        ),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp)
                    )
                }
            }
        }

        // Upcoming line, dimmed — the "what's next" whisper SoundCloud shows.
        if (!nextLine.isNullOrBlank()) {
            Text(
                text = nextLine,
                style = localizedStyle(
                    AuralisType.body.copy(fontSize = (settings.lyricsFontSize * 0.66f).sp),
                    nextLine,
                    settings.lyricsPersianFont
                ),
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp)
                    .graphicsLayer { alpha = 0.55f }
            )
        }
    }
}
