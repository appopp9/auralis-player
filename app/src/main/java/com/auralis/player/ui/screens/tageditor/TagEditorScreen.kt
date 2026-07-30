package com.auralis.player.ui.screens.tageditor

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.player.data.tags.TagUpdate
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.components.AccentIconButton
import com.auralis.player.ui.components.SongArtwork
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

@Composable
fun TagEditorScreen(
    song: Song?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSave: (Song, TagUpdate) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing

    if (song == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Track not available", style = AuralisType.body, color = colors.textSecondary)
        }
        return
    }

    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var albumArtist by remember(song.id) { mutableStateOf(song.albumArtist) }
    var genre by remember(song.id) { mutableStateOf(song.genre) }
    var composer by remember(song.id) { mutableStateOf(song.composer) }
    var year by remember(song.id) { mutableStateOf(if (song.year > 0) song.year.toString() else "") }
    var track by remember(song.id) { mutableStateOf(if (song.trackNumber > 0) song.trackNumber.toString() else "") }
    var disc by remember(song.id) { mutableStateOf(if (song.discNumber > 0) song.discNumber.toString() else "") }
    var lyrics by remember(song.id) { mutableStateOf(song.lyrics ?: "") }

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
                text = "Edit tags",
                style = AuralisType.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).padding(start = spacing.sm)
            )
            AccentIconButton(Icons.Rounded.Save, "Save tags", onClick = {
                onSave(
                    song,
                    TagUpdate(
                        title = title.trim().ifBlank { song.title },
                        artist = artist.trim(),
                        album = album.trim(),
                        albumArtist = albumArtist.trim(),
                        genre = genre.trim(),
                        composer = composer.trim(),
                        year = year.trim().toIntOrNull() ?: 0,
                        track = track.trim().toIntOrNull() ?: 0,
                        disc = disc.trim().toIntOrNull() ?: 0,
                        lyrics = lyrics.ifBlank { null }
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(horizontal = spacing.screen),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                SongArtwork(
                    songId = song.id,
                    modifier = Modifier.size(96.dp),
                    shape = AuralisTheme.shapes.artwork,
                    fallbackIconSize = 32.dp
                )
                Column {
                    Text(song.path, style = AuralisType.bodySmall, color = colors.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Cover artwork is read from the file. Replace the file's embedded art to change it.",
                        style = AuralisType.bodySmall,
                        color = colors.textTertiary
                    )
                }
            }

            Field("Title", title) { title = it }
            Field("Artist", artist) { artist = it }
            Field("Album", album) { album = it }
            Field("Album artist", albumArtist) { albumArtist = it }
            Field("Genre", genre) { genre = it }
            Field("Composer", composer) { composer = it }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Field("Year", year, Modifier.weight(1f), numeric = true) { year = it }
                Field("Track", track, Modifier.weight(1f), numeric = true) { track = it }
                Field("Disc", disc, Modifier.weight(1f), numeric = true) { disc = it }
            }
            OutlinedTextField(
                value = lyrics,
                onValueChange = { lyrics = it },
                label = { Text("Lyrics") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Box(modifier = Modifier.padding(spacing.xl))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.then(if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier),
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
    )
}
