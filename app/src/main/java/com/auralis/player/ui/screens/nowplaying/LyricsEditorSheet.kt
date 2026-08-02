package com.auralis.player.ui.screens.nowplaying

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.components.AuralisChip
import com.auralis.player.ui.components.AuralisDialog
import com.auralis.player.ui.components.SegmentedControl
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.display
import com.auralis.player.ui.theme.localizedStyle

/**
 * In-app lyrics editor.
 *
 * Lets the listener write or paste lyrics by hand instead of relying purely
 * on the online provider, keep a translation alongside them, import either
 * one from a file, and stamp LRC timestamps while the song plays so plain
 * text can be turned into synced lyrics without leaving the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LyricsEditorSheet(
    initialText: String,
    initialTranslation: String,
    loading: Boolean,
    persianFont: Boolean,
    onImportLyrics: () -> Unit,
    onImportTranslation: () -> Unit,
    onTimestamp: () -> String,
    onSave: (text: String, translation: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    var tab by remember { mutableStateOf(0) }
    var lyricsValue by remember(initialText) {
        mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
    }
    var translationValue by remember(initialTranslation) {
        mutableStateOf(TextFieldValue(initialTranslation, TextRange(initialTranslation.length)))
    }

    val editingLyrics = tab == 0
    val current = if (editingLyrics) lyricsValue else translationValue
    fun setCurrent(value: TextFieldValue) {
        if (editingLyrics) lyricsValue = value else translationValue = value
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
                "Edit lyrics",
                style = AuralisTheme.style.display(AuralisType.sectionTitle),
                color = colors.textPrimary
            )
            Text(
                "Write your own lyrics or paste them in. Lines that start with a " +
                    "[mm:ss.xx] tag become synced lyrics automatically.",
                style = AuralisType.bodySmall,
                color = colors.textSecondary
            )

            SegmentedControl(
                options = listOf("Lyrics", "Translation"),
                selectedIndex = tab,
                onSelect = { tab = it }
            )

            if (loading) {
                Text("Loading…", style = AuralisType.body, color = colors.textSecondary)
            }

            // ---- Editor ----------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 300.dp)
                    .clip(AuralisTheme.shapes.card)
                    .background(colors.surfaceMuted)
                    .border(1.dp, colors.outline, AuralisTheme.shapes.card)
                    .padding(spacing.sm)
            ) {
                if (current.text.isEmpty()) {
                    Text(
                        text = if (editingLyrics) {
                            "[00:12.40] First line\n[00:18.10] Second line…\n\nOr just plain text, one line per lyric."
                        } else {
                            "Translation, one line per lyric — timestamps optional."
                        },
                        style = AuralisType.bodySmall,
                        color = colors.textTertiary
                    )
                }
                BasicTextField(
                    value = current,
                    onValueChange = { setCurrent(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    textStyle = localizedStyle(
                        AuralisType.body,
                        current.text,
                        persianFont
                    ).copy(color = colors.textPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent)
                )
            }

            Text(
                "${current.text.lineSequence().count { it.isNotBlank() }} lines",
                style = AuralisType.overline,
                color = colors.textTertiary
            )

            // ---- Tools ------------------------------------------------------
            Text("TOOLS", style = AuralisType.overline, color = colors.textTertiary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                AuralisChip(label = "Stamp time", selected = false) {
                    // Insert the current playback position at the caret, so the
                    // user can sync a plain-text lyric line by line.
                    val tag = onTimestamp() + " "
                    val text = current.text
                    val caret = current.selection.start.coerceIn(0, text.length)
                    val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0))
                        .let { if (it < 0) 0 else it + 1 }
                    val updated = text.substring(0, lineStart) + tag + text.substring(lineStart)
                    setCurrent(
                        TextFieldValue(updated, TextRange((caret + tag.length).coerceAtMost(updated.length)))
                    )
                }
                AuralisChip(label = "Paste", selected = false) {
                    readClipboard(context)?.let { pasted ->
                        val text = current.text
                        val caret = current.selection.start.coerceIn(0, text.length)
                        val updated = text.substring(0, caret) + pasted + text.substring(caret)
                        setCurrent(TextFieldValue(updated, TextRange(caret + pasted.length)))
                    }
                }
                AuralisChip(
                    label = if (editingLyrics) "Import file" else "Import translation",
                    selected = false
                ) {
                    if (editingLyrics) onImportLyrics() else onImportTranslation()
                }
                AuralisChip(label = "Strip timings", selected = false) {
                    val cleaned = current.text
                        .lineSequence()
                        .joinToString("\n") { line ->
                            line.replace(Regex("\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?]"), "").trimStart()
                        }
                    setCurrent(TextFieldValue(cleaned, TextRange(cleaned.length)))
                }
                AuralisChip(label = "Clear", selected = false) {
                    setCurrent(TextFieldValue(""))
                }
            }

            // ---- Save / cancel ----------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.xs, Alignment.End)
            ) {
                AuralisChip(label = "Cancel", selected = false, onClick = dismiss)
                AuralisChip(label = "Save", selected = true) {
                    onSave(lyricsValue.text.trim(), translationValue.text.trim())
                    dismiss()
                }
            }
        }
    }
}

private fun readClipboard(context: Context): String? = runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
        ?.takeIf { it.isNotBlank() }
}.getOrNull()
