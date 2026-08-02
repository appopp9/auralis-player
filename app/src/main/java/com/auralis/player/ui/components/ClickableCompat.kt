package com.auralis.player.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.semantics.Role

@Composable
fun rememberInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * Clickable wrapper without ripple.
 *
 * Ripple forces an extra draw layer on every pressed item; the design instead
 * expresses press state with an instant tint and a small scale, which keeps
 * long lists cheap to draw.
 */
fun Modifier.combinedClickableCompat(
    enabled: Boolean = true,
    role: Role? = null,
    interactionSource: MutableInteractionSource? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val source = interactionSource ?: rememberInteraction()
    combinedClickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onLongClick = onLongClick,
        onClick = onClick
    )
}
