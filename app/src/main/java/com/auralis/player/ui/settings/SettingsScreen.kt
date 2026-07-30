package com.auralis.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.ThemeMode
import com.auralis.player.ui.theme.GoldAccent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings = settingsRepository.settings

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentPalette) {
        viewModelScope.launch { settingsRepository.setAccent(accent) }
    }

    fun setDynamicArtwork(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicArtwork(enabled) }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(
        initialValue = com.auralis.player.data.prefs.AppSettings()
    )
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldAccent.Surface),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // ── Appearance Section ──────────────────────────────────────────────
        item {
            SectionHeader("Appearance")
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            SettingsRow(
                icon = Icons.Default.DarkMode,
                title = "Theme",
                subtitle = settings.themeMode.name,
                onClick = { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            viewModel.setThemeMode(mode)
                            expanded = false
                        }
                    )
                }
            }
        }

        item {
            SettingsRow(
                icon = Icons.Default.Palette,
                title = "Dynamic Artwork Colors",
                subtitle = if (settings.dynamicArtworkColor) "Enabled" else "Disabled",
                onClick = { viewModel.setDynamicArtwork(!settings.dynamicArtworkColor) }
            )
        }

        // ── Accent Color ────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Accent Color")
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            SettingsRow(
                icon = Icons.Default.ColorLens,
                title = "Accent",
                subtitle = settings.accent.name,
                onClick = { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                AccentPalette.entries.forEach { palette ->
                    DropdownMenuItem(
                        text = { Text(palette.name) },
                        onClick = {
                            viewModel.setAccent(palette)
                            expanded = false
                        }
                    )
                }
            }
        }

        // ── Playback Section ────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Playback")
        }

        item {
            SettingsRow(
                icon = Icons.Default.Speed,
                title = "Playback Speed",
                subtitle = "${settings.playbackSpeed}x",
                onClick = { /* TODO */ }
            )
        }

        item {
            SettingsRow(
                icon = Icons.Default.Equalizer,
                title = "Equalizer",
                subtitle = if (settings.eqEnabled) settings.eqPresetName else "Disabled",
                onClick = { /* TODO */ }
            )
        }

        // ── About Section ───────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("About")
        }

        item {
            SettingsRow(
                icon = Icons.Default.Info,
                title = "Auralis Music Player",
                subtitle = "Version 1.0",
                onClick = { }
            )
        }

        item {
            SettingsRow(
                icon = Icons.Default.Favorite,
                title = "Made with ❤",
                subtitle = "AURUM Edition",
                onClick = { }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = GoldAccent.Primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GoldAccent.SurfaceCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldAccent.Primary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = GoldAccent.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GoldAccent.TextSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = GoldAccent.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}
