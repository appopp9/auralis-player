package com.auralis.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.ui.theme.AuralisTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Renders the live spectrum. When real FFT data is unavailable (permission not
 * granted yet) the component animates a subtle idle motion instead of faking a
 * spectrum, and callers surface the enable-capture action.
 */
@Composable
fun AudioVisualizer(
    magnitudes: FloatArray,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    primary: Color? = null,
    secondary: Color? = null,
    intensity: Float = 1f,
    animationSpeed: Float = 1f
) {
    if (mode == VisualizerMode.OFF) return
    val colors = AuralisTheme.colors
    val accent = primary ?: colors.accent
    val glow = secondary ?: colors.accentGlow

    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween((4200 / animationSpeed.coerceIn(0.25f, 3f)).toInt(), easing = LinearEasing)
        ),
        label = "phase"
    )

    val idle = remember(magnitudes.size) { magnitudes.isEmpty() }
    val values = remember(magnitudes, phase, idle, isPlaying) {
        if (!idle) magnitudes
        else FloatArray(40) { index ->
            val base = if (isPlaying) 0.28f else 0.12f
            base + 0.18f * sin(phase + index * 0.35f).let { if (it < 0) -it else it }
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val amp = intensity.coerceIn(0.2f, 2f)
            when (mode) {
                VisualizerMode.BARS -> drawBars(values, accent, glow, amp)
                VisualizerMode.SPECTRUM -> drawSpectrum(values, accent, glow, amp)
                VisualizerMode.WAVE -> drawWave(values, accent, glow, amp, phase)
                VisualizerMode.CIRCULAR -> drawCircular(values, accent, glow, amp)
                VisualizerMode.PARTICLE -> drawParticles(values, accent, glow, amp, phase)
                VisualizerMode.MINIMAL -> drawMinimal(values, accent, amp)
                VisualizerMode.AURORA -> drawAurora(values, accent, glow, amp, phase)
                VisualizerMode.OFF -> Unit
            }
        }
    }
}

private fun DrawScope.drawBars(values: FloatArray, accent: Color, glow: Color, amp: Float) {
    if (values.isEmpty()) return
    val slot = size.width / values.size
    val barWidth = max(2f, slot * 0.6f)
    val brush = Brush.verticalGradient(listOf(glow, accent))
    values.forEachIndexed { index, value ->
        val height = (size.height * value * amp).coerceIn(3f, size.height)
        drawRoundRect(
            brush = brush,
            topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - height),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun DrawScope.drawSpectrum(values: FloatArray, accent: Color, glow: Color, amp: Float) {
    if (values.isEmpty()) return
    val slot = size.width / values.size
    val barWidth = max(2f, slot * 0.45f)
    val center = size.height / 2f
    values.forEachIndexed { index, value ->
        val height = (size.height * value * amp * 0.9f).coerceIn(3f, size.height)
        val color = lerpColor(glow, accent, index.toFloat() / values.size)
        drawRoundRect(
            color = color,
            topLeft = Offset(index * slot + (slot - barWidth) / 2f, center - height / 2f),
            size = Size(barWidth, height),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }
}

private fun DrawScope.drawWave(values: FloatArray, accent: Color, glow: Color, amp: Float, phase: Float) {
    if (values.isEmpty()) return
    val path = Path()
    val step = size.width / (values.size - 1).coerceAtLeast(1)
    val center = size.height / 2f
    values.forEachIndexed { index, value ->
        val y = center - (value * amp * center * 0.9f) * sin(phase + index * 0.25f)
        if (index == 0) path.moveTo(0f, y) else path.lineTo(index * step, y)
    }
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(listOf(glow, accent)),
        style = Stroke(width = 4f)
    )
}

private fun DrawScope.drawCircular(values: FloatArray, accent: Color, glow: Color, amp: Float) {
    if (values.isEmpty()) return
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = minOf(size.width, size.height) / 3.4f
    val count = values.size
    for (i in 0 until count) {
        val angle = (2 * PI * i / count).toFloat()
        val magnitude = radius * 0.75f * values[i] * amp
        val start = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
        val end = Offset(
            center.x + cos(angle) * (radius + magnitude),
            center.y + sin(angle) * (radius + magnitude)
        )
        drawLine(
            color = lerpColor(accent, glow, values[i]),
            start = start,
            end = end,
            strokeWidth = 4f
        )
    }
    drawCircle(color = accent.copy(alpha = 0.18f), radius = radius, center = center, style = Stroke(2f))
}

private fun DrawScope.drawParticles(values: FloatArray, accent: Color, glow: Color, amp: Float, phase: Float) {
    if (values.isEmpty()) return
    val count = values.size
    for (i in 0 until count) {
        val progress = (i.toFloat() / count)
        val x = size.width * progress
        val bounce = sin(phase * 1.4f + i).let { if (it < 0) -it else it }
        val y = size.height - (size.height * values[i] * amp * (0.4f + 0.6f * bounce))
        val radius = 3f + values[i] * 9f * amp
        drawCircle(
            color = lerpColor(accent, glow, progress).copy(alpha = 0.35f + values[i] * 0.6f),
            radius = radius,
            center = Offset(x, y.coerceIn(radius, size.height - radius))
        )
    }
}

private fun DrawScope.drawMinimal(values: FloatArray, accent: Color, amp: Float) {
    if (values.isEmpty()) return
    val level = values.average().toFloat() * amp
    val height = (size.height * level).coerceIn(2f, size.height)
    drawRoundRect(
        color = accent.copy(alpha = 0.85f),
        topLeft = Offset(0f, size.height - height),
        size = Size(size.width, height),
        cornerRadius = CornerRadius(height / 2f, height / 2f)
    )
}

private fun DrawScope.drawAurora(values: FloatArray, accent: Color, glow: Color, amp: Float, phase: Float) {
    if (values.isEmpty()) return
    val layers = 3
    for (layer in 0 until layers) {
        val path = Path()
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val offsetY = size.height * (0.35f + layer * 0.18f)
        path.moveTo(0f, size.height)
        values.forEachIndexed { index, value ->
            val wave = sin(phase * (0.5f + layer * 0.25f) + index * 0.2f)
            val y = offsetY - value * amp * size.height * 0.35f * (0.6f + 0.4f * wave)
            path.lineTo(index * step, y)
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                listOf(
                    lerpColor(accent, glow, layer / layers.toFloat()).copy(alpha = 0.30f - layer * 0.07f),
                    Color.Transparent
                )
            )
        )
    }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val f = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f
    )
}
