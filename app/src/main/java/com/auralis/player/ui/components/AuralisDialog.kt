package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlinx.coroutines.delay

/**
 * Themed dialog matching the app's design system. Slides/scales in with a soft
 * spring and fades out on dismiss (scrim tap, back or button), instead of the
 * stock Material dialog's hard cut.
 */
@Composable
fun AuralisDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    val motion = AuralisTheme.motion
    var visible by remember { mutableStateOf(false) }
    var closeRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(closeRequested) {
        if (closeRequested) {
            visible = false
            delay(motion.duration(motion.medium).toLong() + 30L)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { closeRequested = true },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(motion.tweenMedium()) + scaleIn(motion.softSpring(), initialScale = 0.86f),
            exit = fadeOut(motion.tweenFast()) + scaleOut(motion.tweenFast(), targetScale = 0.92f)
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(24.dp),
                shape = AuralisTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(AuralisTheme.spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    content { closeRequested = true }
                }
            }
        }
    }
}

/** A primary pill button used inside dialogs. */
@Composable
private fun DialogPillButton(
    label: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val bg = when {
        destructive -> colors.danger
        else -> colors.accent
    }
    PressableSurface(
        onClick = { if (!loading) onClick() },
        enabled = enabled && !loading,
        shape = AuralisTheme.shapes.chip,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(AuralisTheme.shapes.chip)
                .background(bg)
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = colors.onAccent,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(label, style = AuralisType.label, color = colors.onAccent, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A ghost pill button used inside dialogs. */
@Composable
private fun DialogGhostButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    PressableSurface(onClick = onClick, enabled = enabled, shape = AuralisTheme.shapes.chip, modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(AuralisTheme.shapes.chip)
                .background(colors.surfaceMuted)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, style = AuralisType.label, color = colors.textPrimary, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Confirmation dialog with an icon, title, message and two actions. Used for
 * destructive flows like deleting a playlist. Supports a loading state on the
 * confirm button.
 */
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = "Cancel",
    destructive: Boolean = true,
    loading: Boolean = false
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    AuralisDialog(onDismiss = onDismiss) { dismiss ->
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (destructive) colors.danger.copy(alpha = 0.14f) else colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) colors.danger else colors.accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = title,
            style = AuralisType.title,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.md)
        )
        Text(
            text = message,
            style = AuralisType.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.xs)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            DialogGhostButton(
                label = dismissLabel,
                enabled = !loading,
                modifier = Modifier.weight(1f),
                onClick = { dismiss() }
            )
            DialogPillButton(
                label = confirmLabel,
                destructive = destructive,
                loading = loading,
                modifier = Modifier.weight(1f),
                onClick = onConfirm
            )
        }
    }
}
