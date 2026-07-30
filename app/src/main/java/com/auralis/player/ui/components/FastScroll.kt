package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlinx.coroutines.launch

/**
 * Alphabetical fast scroller for long lists. Dragging jumps to the first item
 * whose sort key starts with the selected letter.
 */
@Composable
fun AlphabetFastScroll(
    listState: LazyListState,
    keys: List<String>,
    modifier: Modifier = Modifier
) {
    if (keys.size < 30) return
    val colors = AuralisTheme.colors
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var activeLetter by remember { mutableStateOf<String?>(null) }

    val letters = remember(keys) {
        val available = keys.map { normalizeLetter(it) }.distinct()
        ALPHABET.filter { it in available } + available.filter { it !in ALPHABET }.distinct()
    }
    if (letters.size < 4) return

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .width(26.dp)
                .clip(AuralisTheme.shapes.chip)
                .background(colors.surfaceMuted.copy(alpha = 0.6f))
                .padding(vertical = 6.dp)
                .pointerInput(letters, keys) {
                    detectVerticalDragGestures(
                        onDragEnd = { activeLetter = null },
                        onDragCancel = { activeLetter = null }
                    ) { change, _ ->
                        change.consume()
                        val fraction = (change.position.y / size.height).coerceIn(0f, 0.999f)
                        val letter = letters[(fraction * letters.size).toInt()]
                        if (letter != activeLetter) {
                            activeLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val index = keys.indexOfFirst { normalizeLetter(it) == letter }
                            if (index >= 0) scope.launch { listState.scrollToItem(index) }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter,
                    style = AuralisType.overline,
                    color = if (letter == activeLetter) colors.accent else colors.textTertiary
                )
            }
        }

        AnimatedVisibility(visible = activeLetter != null, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .padding(end = 34.dp)
                    .clip(AuralisTheme.shapes.card)
                    .background(colors.accent)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = activeLetter.orEmpty(), style = AuralisType.title, color = colors.onAccent)
            }
        }
    }
}

private val ALPHABET = ('A'..'Z').map { it.toString() } + "#"

private fun normalizeLetter(value: String): String {
    val first = value.trim().firstOrNull()?.uppercaseChar() ?: return "#"
    return if (first.isLetter()) first.toString() else "#"
}
