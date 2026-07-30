package com.auralis.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

/** Flat panel: one opaque fill plus a hairline. No gradients, no glass. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.card,
    accentWash: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AuralisTheme.colors
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.surface)
            .border(1.dp, colors.outline.copy(alpha = if (colors.isDark) 0.35f else 0.5f), shape),
        content = content
    )
}

/** Press-responsive wrapper: instant tint + tiny scale, no ripple. */
@Composable
fun PressableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    haptics: Boolean = true,
    shape: Shape = AuralisTheme.shapes.card,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.97f else 1f,
        animationSpec = motion.popSpring(),
        label = "press"
    )
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (pressed) colors.accentSoft else Color.Transparent)
            .combinedClickableCompat(
                enabled = enabled,
                interactionSource = interaction,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
                onClick = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        content = content
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    Row(
        modifier = modifier
            .padding(horizontal = spacing.screen, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = AuralisType.headline, color = colors.textPrimary)
        if (actionLabel != null && onAction != null) {
            Box(
                modifier = Modifier
                    .clip(AuralisTheme.shapes.chip)
                    .combinedClickableCompat(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = actionLabel, style = AuralisType.label, color = colors.accent)
            }
        }
    }
}

@Composable
fun AuralisChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val background by animateColorAsState(
        if (selected) colors.accent else colors.surfaceMuted,
        animationSpec = motion.tweenFast(),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        if (selected) colors.onAccent else colors.textSecondary,
        animationSpec = motion.tweenFast(),
        label = "chipFg"
    )
    Row(
        modifier = modifier
            .clip(AuralisTheme.shapes.chip)
            .background(background)
            .combinedClickableCompat(onClick = onClick)
            .defaultMinSize(minHeight = 36.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(text = label, style = AuralisType.label, color = contentColor)
    }
}

@Composable
fun AccentIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    filled: Boolean = false,
    tint: Color? = null,
    haptics: Boolean = true
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.86f else 1f,
        animationSpec = motion.popSpring(),
        label = "iconPress"
    )
    val haptic = LocalHapticFeedback.current
    val background = when {
        filled -> colors.accent
        pressed -> colors.accentSoft
        else -> Color.Transparent
    }
    val iconTint = tint ?: if (filled) colors.onAccent else colors.textPrimary
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(background)
            .combinedClickableCompat(
                interactionSource = interaction,
                onClick = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    Column(
        modifier = modifier.padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(text = title, style = AuralisType.title, color = colors.textPrimary, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = AuralisType.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            AuralisChip(label = actionLabel, selected = true, onClick = onAction)
        }
    }
}
