package com.auralis.player.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.PanelStyle
import com.auralis.player.ui.theme.display

// ---------------------------------------------------------------------------
// Themed drawing primitives
// ---------------------------------------------------------------------------

/**
 * Dual-source neumorphic shadow (top-light / bottom-shade). Uses the native
 * shadow layer, hardware-accelerated from API 28; earlier devices silently
 * degrade to the flat surface, which still looks correct.
 */
fun Modifier.neumorph(
    cornerRadius: Dp,
    hi: Color,
    lo: Color,
    elevation: Dp = 7.dp
): Modifier = drawBehind {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return@drawBehind
    if (hi.alpha == 0f && lo.alpha == 0f) return@drawBehind
    val r = cornerRadius.toPx()
    val blur = elevation.toPx() * 1.7f
    val off = elevation.toPx() * 0.8f
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fw = paint.asFrameworkPaint()
        fw.isAntiAlias = true
        fw.color = android.graphics.Color.TRANSPARENT
        fw.setShadowLayer(blur, -off, -off, hi.copy(alpha = (hi.alpha * 0.95f)).toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        fw.setShadowLayer(blur, off, off, lo.copy(alpha = (lo.alpha * 0.95f)).toArgb())
        canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
    }
}

/** Accent fill: flat colour normally, two-stop gradient on gradient themes. */
@Composable
fun accentBrush(): Brush {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    return remember(colors.accent, colors.accentAlt, style.gradientAccent) {
        if (style.gradientAccent) {
            Brush.linearGradient(listOf(colors.accent, colors.accentAlt))
        } else {
            SolidColor(colors.accent)
        }
    }
}

/**
 * Container treatment for floating chrome (mini player, nav pill, sheets).
 * Same visual language as [GlassPanel] but usable inside modifier chains that
 * also need gestures.
 */
@Composable
fun Modifier.themedContainer(
    shape: Shape,
    elevated: Boolean = true
): Modifier {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val fill = if (elevated) colors.backgroundElevated else colors.surface
    return when (style.panel) {
        PanelStyle.NEUMORPH -> this
            .neumorph(cornerRadius = 22.dp, hi = colors.neumorphHi, lo = colors.neumorphLo)
            .clip(shape)
            .background(colors.surface)

        PanelStyle.GLASS -> this
            .clip(shape)
            .background(colors.surfaceGlass)
            .border(1.dp, Color.White.copy(alpha = if (colors.isDark) 0.09f else 0.45f), shape)

        PanelStyle.LUXE -> this
            .clip(shape)
            .background(fill)
            .border(1.dp, colors.hairline, shape)

        PanelStyle.OUTLINE -> this
            .clip(shape)
            .background(fill)
            .border(1.dp, colors.outline, shape)

        PanelStyle.TONAL -> this
            .clip(shape)
            .background(fill)
            .border(1.dp, colors.outline.copy(alpha = if (colors.isDark) 0.4f else 0.55f), shape)
    }
}

/**
 * The workhorse surface. Renders according to the active design system:
 *  - TONAL    → soft tinted fill + subtle hairline
 *  - GLASS    → translucent fill, luminous top edge
 *  - NEUMORPH → canvas-coloured fill extruded with dual shadows
 *  - OUTLINE  → flat background + 1dp outline (Scandinavian / AMOLED)
 *  - LUXE     → near-black fill framed by a metallic hairline
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AuralisTheme.shapes.card,
    accentWash: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style

    val base = when (style.panel) {
        PanelStyle.NEUMORPH -> modifier
            .neumorph(cornerRadius = 22.dp, hi = colors.neumorphHi, lo = colors.neumorphLo)
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.surface)

        PanelStyle.GLASS -> modifier
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.surfaceGlass)
            .border(1.dp, Color.White.copy(alpha = if (colors.isDark) 0.08f else 0.4f), shape)
            .drawBehind {
                // luminous top edge
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, colors.accent.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.5f
                )
            }

        PanelStyle.OUTLINE -> modifier
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.backgroundElevated)
            .border(1.dp, colors.outline, shape)

        PanelStyle.LUXE -> modifier
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.surface)
            .border(1.dp, colors.hairline, shape)

        PanelStyle.TONAL -> modifier
            .clip(shape)
            .background(if (accentWash) colors.accentSoft else colors.surface)
            .border(
                1.dp,
                colors.outline.copy(alpha = if (colors.isDark) 0.35f else 0.5f),
                shape
            )
    }

    Box(modifier = base, content = content)
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
    val style = AuralisTheme.style
    val spacing = AuralisTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screen, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = style.display(AuralisType.headline),
            color = colors.textPrimary
        )
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

/**
 * Selection chip. `onClick` is the trailing parameter so call sites can use
 * lambda syntax. Selected fill follows the theme: gradient on gradient themes,
 * outlined tick on Scandinavian, flat accent elsewhere.
 */
@Composable
fun AuralisChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val motion = AuralisTheme.motion
    val shape = AuralisTheme.shapes.chip

    val background by animateColorAsState(
        when {
            selected && style.gradientAccent -> Color.Transparent // gradient painted below
            selected -> colors.accent
            style.panel == PanelStyle.OUTLINE || style.panel == PanelStyle.LUXE -> Color.Transparent
            else -> colors.surfaceMuted
        },
        animationSpec = motion.tweenFast(),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        if (selected) {
            if (style.gradientAccent) Color.White else colors.onAccent
        } else {
            colors.textSecondary
        },
        animationSpec = motion.tweenFast(),
        label = "chipFg"
    )

    val decorated = when {
        selected && style.gradientAccent -> Modifier.background(accentBrush(), shape)
        !selected && (style.panel == PanelStyle.OUTLINE || style.panel == PanelStyle.LUXE) ->
            Modifier.border(1.dp, colors.outline, shape)
        else -> Modifier
    }

    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(decorated)
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

/**
 * Icon button with press shrink. `onClick` is trailing for lambda call sites.
 */
@Composable
fun AccentIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    filled: Boolean = false,
    tint: Color? = null,
    haptics: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val motion = AuralisTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.86f else 1f,
        animationSpec = motion.popSpring(),
        label = "iconPress"
    )
    val haptic = LocalHapticFeedback.current

    val fillModifier = when {
        filled && style.gradientAccent -> Modifier.background(accentBrush())
        filled -> Modifier.background(colors.accent)
        pressed -> Modifier.background(colors.accentSoft)
        else -> Modifier
    }
    val neumorphModifier = if (!filled && style.panel == PanelStyle.NEUMORPH) {
        Modifier.neumorph(cornerRadius = size.value.dp / 2, hi = colors.neumorphHi, lo = colors.neumorphLo, elevation = 4.dp)
    } else {
        Modifier
    }
    val iconTint = tint ?: when {
        filled && style.gradientAccent -> Color.White
        filled -> colors.onAccent
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(neumorphModifier)
            .clip(CircleShape)
            .then(fillModifier)
            .combinedClickableCompat(
                interactionSource = interaction,
                onLongClick = onLongClick?.let {
                    {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
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
