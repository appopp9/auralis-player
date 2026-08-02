package com.auralis.player.ui.screens.nowplaying

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.auralis.player.domain.model.LyricLine
import com.auralis.player.domain.model.Lyrics
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.AuralisDialog
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Visual treatments available for a shared lyric card. */
enum class LyricCardStyle(val label: String) {
    GRADIENT("Gradient"),
    ARTWORK("Aurora"),
    DARK("Dark"),
    LIGHT("Light")
}

/**
 * Lyric share sheet.
 *
 * Replaces the old one-tap "share whatever line is playing" flow: the user
 * picks any number of lines (tap to toggle, long-press to extend a range),
 * sees a live preview of the resulting card, picks a style and which extras
 * to include, then shares it as an image, as text, or copies it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricShareSheet(
    song: Song?,
    lyrics: Lyrics?,
    activeIndex: Int,
    persianFont: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val graphicsLayer = rememberGraphicsLayer()

    val lines: List<LyricLine> = lyrics?.lines.orEmpty()
    val translations: List<LyricLine> = lyrics?.translationLines.orEmpty()

    // Selection starts on the line that is playing right now, so "open and
    // share" is still a two-tap flow for the common case.
    val selected = remember(lyrics, song?.id) {
        mutableStateListOf<Int>().apply {
            if (lines.isNotEmpty()) add(activeIndex.coerceIn(0, lines.lastIndex))
        }
    }
    var anchor by remember(lyrics, song?.id) {
        mutableStateOf(activeIndex.coerceAtLeast(0))
    }
    var style by remember { mutableStateOf(LyricCardStyle.GRADIENT) }
    var showArtwork by remember { mutableStateOf(true) }
    var showTrackInfo by remember { mutableStateOf(true) }
    var showTranslation by remember { mutableStateOf(translations.isNotEmpty()) }

    // The list is positioned ONCE, on the line that was playing when the sheet
    // opened. After that it never moves on its own: while the user is picking
    // lines, playback must not yank the list around. The currently playing
    // line stays visually highlighted, that is all.
    val listState = rememberLazyListState()
    var initialised by remember(lyrics, song?.id) { mutableStateOf(false) }
    LaunchedEffect(lyrics, song?.id, lines.size) {
        if (!initialised && lines.isNotEmpty()) {
            listState.scrollToItem((activeIndex - 2).coerceIn(0, lines.lastIndex))
            initialised = true
        }
    }

    fun translationFor(index: Int): String? {
        val line = lines.getOrNull(index) ?: return null
        if (translations.isEmpty()) return null
        return translations
            .minByOrNull { abs(it.timeMs - line.timeMs) }
            ?.takeIf { abs(it.timeMs - line.timeMs) <= 1200L }
            ?.text
    }

    val ordered = selected.sorted()
    val selectedText = ordered.mapNotNull { lines.getOrNull(it)?.text }
    val selectedTranslations = if (showTranslation) {
        ordered.map { translationFor(it).orEmpty() }
    } else {
        emptyList()
    }

    AuralisDialog(onDismiss = onDismiss) { dismiss ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Text(
                "Share lyrics",
                style = AuralisTheme.style.display(AuralisType.sectionTitle),
                color = colors.textPrimary
            )

            if (lines.isEmpty()) {
                Text(
                    "There are no lyrics to share yet. Add or import lyrics first.",
                    style = AuralisType.body,
                    color = colors.textSecondary
                )
                AuralisChip(label = "Close", selected = false, onClick = dismiss)
                return@AuralisDialog
            }

            // ---- Live preview --------------------------------------------
            Text("PREVIEW", style = AuralisType.overline, color = colors.textTertiary)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // Draw the card normally AND record it into a graphics layer, so
                // the preview is always visible and the exported PNG is exactly
                // what the user sees.
                Box(
                    modifier = Modifier.drawWithContent {
                        graphicsLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(graphicsLayer)
                    }
                ) {
                    LyricCardContent(
                        song = song,
                        lines = selectedText,
                        translations = selectedTranslations,
                        style = style,
                        showArtwork = showArtwork,
                        showTrackInfo = showTrackInfo,
                        persianFont = persianFont
                    )
                }
            }

            // ---- Style + toggles -----------------------------------------
            Text("STYLE", style = AuralisType.overline, color = colors.textTertiary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                LyricCardStyle.values().forEach { option ->
                    AuralisChip(
                        label = option.label,
                        selected = style == option,
                        onClick = { style = option }
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                AuralisChip(
                    label = "Artwork",
                    selected = showArtwork,
                    onClick = { showArtwork = !showArtwork }
                )
                AuralisChip(
                    label = "Track info",
                    selected = showTrackInfo,
                    onClick = { showTrackInfo = !showTrackInfo }
                )
                if (translations.isNotEmpty()) {
                    AuralisChip(
                        label = "Translation",
                        selected = showTranslation,
                        onClick = { showTranslation = !showTranslation }
                    )
                }
            }

            // ---- Line picker ----------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LINES · ${selected.size} SELECTED",
                    style = AuralisType.overline,
                    color = colors.textTertiary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    AuralisChip(label = "Clear", selected = false) { selected.clear() }
                    AuralisChip(label = "All", selected = false) {
                        selected.clear()
                        selected.addAll(lines.indices)
                    }
                }
            }
            Text(
                "Tap a line to add it. Long-press to select everything between it and your last tap.",
                style = AuralisType.bodySmall,
                color = colors.textTertiary
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(AuralisTheme.shapes.card)
                    .background(colors.surfaceMuted),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(spacing.xs)
            ) {
                items(lines.size) { index ->
                    val line = lines[index]
                    val isSelected = selected.contains(index)
                    SelectableLyricRow(
                        text = line.text,
                        translation = if (showTranslation) translationFor(index) else null,
                        selected = isSelected,
                        isPlaying = index == activeIndex,
                        persianFont = persianFont,
                        onClick = {
                            if (isSelected) selected.remove(index) else selected.add(index)
                            anchor = index
                        },
                        onLongClick = {
                            val from = minOf(anchor, index)
                            val to = maxOf(anchor, index)
                            (from..to).forEach { if (!selected.contains(it)) selected.add(it) }
                        }
                    )
                }
            }

            // ---- Actions ----------------------------------------------------
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                ShareAction(
                    icon = Icons.Rounded.Image,
                    label = "Share image",
                    enabled = selectedText.isNotEmpty()
                ) {
                    scope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        shareLyricCard(context, bitmap, song)
                        dismiss()
                    }
                }
                ShareAction(
                    icon = Icons.Rounded.Share,
                    label = "Share text",
                    enabled = selectedText.isNotEmpty()
                ) {
                    shareLyricText(context, buildShareText(song, selectedText, selectedTranslations))
                    dismiss()
                }
                ShareAction(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy",
                    enabled = selectedText.isNotEmpty()
                ) {
                    copyToClipboard(context, buildShareText(song, selectedText, selectedTranslations))
                    dismiss()
                }
            }
        }
    }
}

/** One tappable, checkable lyric row in the picker list. */
@Composable
private fun SelectableLyricRow(
    text: String,
    translation: String?,
    selected: Boolean,
    isPlaying: Boolean,
    persianFont: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AuralisTheme.shapes.small)
            .background(if (selected) colors.accent.copy(alpha = 0.16f) else Color.Transparent)
            .combinedClickableCompat(onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (selected) colors.accent else colors.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text.ifBlank { "♪" },
                style = localizedStyle(AuralisType.body, text, persianFont),
                color = if (isPlaying) colors.accent else colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!translation.isNullOrBlank()) {
                Text(
                    text = translation,
                    style = localizedStyle(AuralisType.bodySmall, translation, persianFont),
                    color = colors.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ShareAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    Row(
        modifier = Modifier
            .clip(AuralisTheme.shapes.chip)
            .background(if (enabled) colors.accent.copy(alpha = 0.14f) else colors.surfaceMuted)
            .combinedClickableCompat(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) colors.accent else colors.textTertiary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            label,
            style = AuralisType.label,
            color = if (enabled) colors.textPrimary else colors.textTertiary
        )
    }
}

/** The capturable card — rendered on screen as the live preview and shared as PNG. */
@Composable
private fun LyricCardContent(
    song: Song?,
    lines: List<String>,
    translations: List<String>,
    style: LyricCardStyle,
    showArtwork: Boolean,
    showTrackInfo: Boolean,
    persianFont: Boolean
) {
    val colors = AuralisTheme.colors
    val onCard = when (style) {
        LyricCardStyle.LIGHT -> Color(0xFF14161A)
        LyricCardStyle.DARK -> Color(0xFFF3F5F8)
        else -> colors.textPrimary
    }
    val secondary = onCard.copy(alpha = 0.68f)
    val background: Modifier = when (style) {
        LyricCardStyle.GRADIENT -> Modifier.background(
            Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.55f), colors.background))
        )
        LyricCardStyle.ARTWORK -> Modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.accentAlt.copy(alpha = 0.65f),
                    colors.accent.copy(alpha = 0.35f),
                    colors.background
                )
            )
        )
        LyricCardStyle.DARK -> Modifier.background(Color(0xFF0B0D10))
        LyricCardStyle.LIGHT -> Modifier.background(Color(0xFFF7F7F9))
    }

    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(background)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showArtwork) {
            SongArtwork(
                songId = song?.id ?: -1L,
                modifier = Modifier
                    .size(if (lines.size > 4) 96.dp else 140.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(18.dp))
        }

        if (lines.isEmpty()) {
            Text(
                "Select one or more lines below",
                style = AuralisType.body,
                color = secondary,
                textAlign = TextAlign.Center
            )
        } else {
            val lineStyle = if (lines.size > 5) AuralisType.title else AuralisType.headline
            lines.forEachIndexed { index, line ->
                Text(
                    text = line.ifBlank { "♪" },
                    style = localizedStyle(
                        AuralisTheme.style.display(lineStyle),
                        line,
                        persianFont
                    ),
                    color = onCard,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 6.dp)
                )
                translations.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { translation ->
                    Text(
                        text = translation,
                        style = localizedStyle(AuralisType.bodySmall, translation, persianFont),
                        color = secondary,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        if (showTrackInfo) {
            Text(
                text = song?.title.orEmpty(),
                style = localizedStyle(AuralisType.title, song?.title.orEmpty(), persianFont)
                    .copy(fontWeight = FontWeight.SemiBold),
                color = onCard,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 22.dp)
            )
            Text(
                text = song?.displayArtist.orEmpty(),
                style = localizedStyle(
                    AuralisType.bodySmall,
                    song?.displayArtist.orEmpty(),
                    persianFont
                ),
                color = secondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "Auralis",
            style = AuralisType.overline,
            color = if (style == LyricCardStyle.LIGHT) colors.accent else onCard.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
        )
    }
}

/** Plain-text version of the selection, used for text share and copy. */
private fun buildShareText(
    song: Song?,
    lines: List<String>,
    translations: List<String>
): String {
    val body = lines.mapIndexed { index, line ->
        val translation = translations.getOrNull(index)?.takeIf { it.isNotBlank() }
        if (translation != null) "$line\n$translation" else line
    }.joinToString("\n")
    val credit = listOfNotNull(
        song?.title?.takeIf { it.isNotBlank() },
        song?.displayArtist?.takeIf { it.isNotBlank() }
    ).joinToString(" — ")
    return if (credit.isBlank()) body else "$body\n\n— $credit"
}

private fun shareLyricText(context: Context, text: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share lyrics").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics", text))
        Toast.makeText(context, "Lyrics copied", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun shareLyricCard(context: Context, bitmap: Bitmap, song: Song?) =
    withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "lyric_cards").apply { mkdirs() }
            val file = File(dir, "lyric_card_${song?.id ?: "shared"}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${song?.title.orEmpty()} — ${song?.displayArtist.orEmpty()}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share lyric card")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
