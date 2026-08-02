package com.auralis.player.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Auralis input kit — custom-drawn controls with springy, tactile motion.
// ---------------------------------------------------------------------------

/**
 * Custom switch: pill track that floods with accent, thumb that stretches
 * while pressed and springs across. No Material ripple.
 */
@Composable
fun AuralisSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val style = AuralisTheme.style
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val trackWidth = 52.dp
    val trackHeight = 30.dp
    val thumbSize by animateDpAsState(
        targetValue = if (pressed) 26.dp else 22.dp,
        animationSpec = motion.popSpring(),
        label = "thumbSize"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 4.dp else 4.dp,
        animationSpec = motion.popSpring(),
        label = "thumbOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.surfaceMuted,
        animationSpec = motion.tweenFast(),
        label = "trackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.onAccent else colors.textSecondary,
        animationSpec = motion.tweenFast(),
        label = "thumbColor"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .then(
                if (checked && style.gradientAccent) {
                    Modifier.background(
                        Brush.linearGradient(listOf(colors.accent, colors.accentAlt))
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickableCompat(interactionSource = interaction) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(if (checked) Color.White else thumbColor.copy(alpha = 0.9f))
        )
    }
}

/**
 * Custom slider: rounded track, gradient fill, thumb that grows under the
 * finger with a halo. Emits continuously; [onChangeFinished] on release.
 */
@Composable
fun AuralisSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onChangeFinished: (() -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val style = AuralisTheme.style
    var widthPx by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val min = valueRange.start
    val max = valueRange.endInclusive
    val fraction = if (max > min) ((value - min) / (max - min)).coerceIn(0f, 1f) else 0f

    val thumbSize by animateDpAsState(
        targetValue = if (dragging) 24.dp else 18.dp,
        animationSpec = motion.popSpring(),
        label = "sliderThumb"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (dragging) 0.22f else 0f,
        animationSpec = motion.tweenFast(),
        label = "sliderHalo"
    )

    fun valueAt(x: Float): Float {
        if (widthPx <= 0) return value
        val f = (x / widthPx).coerceIn(0f, 1f)
        return min + f * (max - min)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(min, max) {
                detectTapGestures(
                    onTap = { offset ->
                        onValueChange(valueAt(offset.x))
                        onChangeFinished?.invoke()
                    }
                )
            }
            .pointerInput(min, max) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        onValueChange(valueAt(offset.x))
                    },
                    onDragEnd = {
                        dragging = false
                        onChangeFinished?.invoke()
                    },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    onValueChange(valueAt(change.position.x))
                }
            }
            .drawBehind {
                val trackHeight = 6.dp.toPx()
                val y = size.height / 2f
                val radius = CornerRadius(trackHeight / 2f)
                // base track
                drawRoundRect(
                    color = colors.surfaceMuted,
                    topLeft = Offset(0f, y - trackHeight / 2f),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius
                )
                // active fill
                val activeWidth = size.width * fraction
                if (activeWidth > 0f) {
                    if (style.gradientAccent) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(colors.accent, colors.accentAlt),
                                endX = size.width
                            ),
                            topLeft = Offset(0f, y - trackHeight / 2f),
                            size = Size(activeWidth, trackHeight),
                            cornerRadius = radius
                        )
                    } else {
                        drawRoundRect(
                            color = colors.accent,
                            topLeft = Offset(0f, y - trackHeight / 2f),
                            size = Size(activeWidth, trackHeight),
                            cornerRadius = radius
                        )
                    }
                }
                // halo under thumb while dragging
                if (haloAlpha > 0f) {
                    drawCircle(
                        color = colors.accent.copy(alpha = haloAlpha),
                        radius = 20.dp.toPx(),
                        center = Offset(activeWidth, y)
                    )
                }
            }
    ) {
        val thumbPx = with(density) { thumbSize.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((widthPx * fraction) - thumbPx / 2f).roundToInt()
                            .coerceIn(0, (widthPx - thumbPx).roundToInt().coerceAtLeast(0)),
                        y = ((38.dp.toPx() - thumbPx) / 2f).roundToInt()
                    )
                }
                .size(thumbSize)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .drawBehind {
                    drawCircle(
                        color = colors.accent,
                        radius = size.minDimension * 0.32f
                    )
                    drawCircle(
                        color = colors.accent.copy(alpha = 0.5f),
                        radius = size.minDimension * 0.5f - 1.dp.toPx(),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
        )
    }
}

/**
 * Segmented control with a sliding elevated indicator — replaces cramped chip
 * rows for small closed sets like theme mode.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(AuralisTheme.shapes.chip)
            .background(colors.surfaceMuted)
            .padding(3.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = motion.popSpring(),
            label = "segIndicator"
        )
        // sliding indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(2.dp, AuralisTheme.shapes.chip)
                .clip(AuralisTheme.shapes.chip)
                .background(colors.backgroundElevated)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (selected) colors.accent else colors.textSecondary,
                    animationSpec = motion.tweenFast(),
                    label = "segText$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AuralisTheme.shapes.chip)
                        .combinedClickableCompat {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AuralisType.label,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Labeled slider row with live value readout, used across settings/sheets. */
@Composable
fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    onChangeFinished: (() -> Unit)? = null,
    onValueChange: (Float) -> Unit
) {
    val colors = AuralisTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = AuralisType.bodySmall, color = colors.textSecondary)
            if (valueText != null) {
                Text(valueText, style = AuralisType.numeric, color = colors.accent)
            }
        }
        AuralisSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            onChangeFinished = onChangeFinished
        )
    }
}

/** Switch row: label + optional description + AuralisSwitch. */
@Composable
fun SwitchSetting(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    description: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = AuralisTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AuralisTheme.shapes.small)
            .combinedClickableCompat { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = AuralisType.body, color = colors.textPrimary)
            if (description != null) {
                Text(description, style = AuralisType.bodySmall, color = colors.textTertiary)
            }
        }
        AuralisSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
