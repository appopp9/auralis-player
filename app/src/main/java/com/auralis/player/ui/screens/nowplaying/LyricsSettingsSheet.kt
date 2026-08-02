package com.auralis.player.ui.screens.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.ui.components.SegmentedControl
import com.auralis.player.ui.components.SheetDivider
import com.auralis.player.ui.components.SheetHandle
import com.auralis.player.ui.components.SliderSetting
import com.auralis.player.ui.components.SwitchSetting
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlin.math.roundToInt

/** Per-setting update actions wired to the view model (live preview). */
data class LyricsSettingsActions(
    val onFontSize: (Float) -> Unit,
    val onLineSpacing: (Float) -> Unit,
    val onAlign: (Int) -> Unit,
    val onBoldActive: (Boolean) -> Unit,
    val onShowTranslation: (Boolean) -> Unit,
    val onTranslationScale: (Float) -> Unit,
    val onTranslationGap: (Float) -> Unit,
    val onInactiveAlpha: (Float) -> Unit,
    val onPersianFont: (Boolean) -> Unit
)

/**
 * Advanced lyrics appearance panel. Every control writes straight to DataStore,
 * and the lyrics surfaces collect that flow — so the preview is truly live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsSheet(
    settings: AppSettings,
    actions: LyricsSettingsActions,
    onDismiss: () -> Unit
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.backgroundElevated,
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.screen, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Column {
                Text("Lyrics appearance", style = AuralisType.headline, color = colors.textPrimary)
                Text("Changes apply instantly", style = AuralisType.bodySmall, color = colors.textSecondary)
            }

            SliderSetting(
                label = "Font size",
                value = settings.lyricsFontSize,
                valueRange = 14f..40f,
                valueText = "${settings.lyricsFontSize.roundToInt()}sp",
                onValueChange = actions.onFontSize
            )
            SliderSetting(
                label = "Line spacing",
                value = settings.lyricsLineSpacing,
                valueRange = 1f..2.2f,
                valueText = String.format("%.2f×", settings.lyricsLineSpacing),
                onValueChange = actions.onLineSpacing
            )

            Column {
                Text("Alignment", style = AuralisType.bodySmall, color = colors.textSecondary)
                SegmentedControl(
                    options = listOf("Start", "Center", "End"),
                    selectedIndex = settings.lyricsAlign.coerceIn(0, 2),
                    onSelect = actions.onAlign
                )
            }

            SwitchSetting(
                label = "Bold active line",
                checked = settings.lyricsBoldActive,
                description = "Emphasise the line being sung"
            ) { actions.onBoldActive(it) }

            SwitchSetting(
                label = "Persian font (Vazir)",
                checked = settings.lyricsPersianFont,
                description = "Use Vazir for Persian lyrics and titles"
            ) { actions.onPersianFont(it) }

            SliderSetting(
                label = "Inactive line opacity",
                value = settings.lyricsInactiveAlpha,
                valueRange = 0.1f..0.9f,
                valueText = "${(settings.lyricsInactiveAlpha * 100).roundToInt()}%",
                onValueChange = actions.onInactiveAlpha
            )

            SheetDivider()
            Text("Translation", style = AuralisType.title, color = colors.textPrimary)

            SwitchSetting(
                label = "Show translation",
                checked = settings.lyricsShowTranslation,
                description = "Display translation under the original line"
            ) { actions.onShowTranslation(it) }

            SliderSetting(
                label = "Translation size",
                value = settings.lyricsTranslationScale,
                valueRange = 0.6f..1f,
                valueText = "${(settings.lyricsTranslationScale * 100).roundToInt()}%",
                onValueChange = actions.onTranslationScale
            )

            SliderSetting(
                label = "Gap above translation",
                value = settings.lyricsTranslationGap,
                valueRange = 0f..24f,
                valueText = "${settings.lyricsTranslationGap.roundToInt()}dp",
                onValueChange = actions.onTranslationGap
            )

            Box(modifier = Modifier.height(spacing.lg))
        }
    }
}
