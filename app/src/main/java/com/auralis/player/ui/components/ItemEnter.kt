package com.auralis.player.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import kotlinx.coroutines.delay

/** Timestamp of the moment the current screen started appearing. */
val LocalEnterWindow: ProvidableCompositionLocal<Long> = staticCompositionLocalOf { 0L }

private const val WINDOW_MS = 520L

/** Marks a subtree as a freshly entered screen. */
@Composable
fun ScreenEnterWindow(content: @Composable () -> Unit) {
    val start = remember { System.currentTimeMillis() }
    CompositionLocalProvider(LocalEnterWindow provides start, content = content)
}

/**
 * Staggered fade + rise entrance for list items.
 *
 * Animates **only** while the screen is inside its entrance window (the first
 * [WINDOW_MS] after navigation). Rows composed later — which is exactly what
 * happens while scrolling — return `Modifier` untouched, so fast flings carry
 * zero animation or graphics-layer cost.
 *
 * This is a `@Composable` factory rather than `Modifier.composed {}`: composed
 * materialises on every use and was measurable overhead per row; this version
 * is free once the window has closed.
 */
@Composable
fun Modifier.appear(
    index: Int = 0,
    distance: Dp = 18.dp,
    staggerMs: Int = 26
): Modifier {
    val motion = AuralisTheme.motion
    val window = LocalEnterWindow.current
    val inWindow = motion.enabled &&
        window != 0L &&
        System.currentTimeMillis() - window < WINDOW_MS

    if (!inWindow) return this

    val distancePx = with(LocalDensity.current) { distance.toPx() }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, 12) * staggerMs.toLong())
        progress.animateTo(1f, motion.softSpring())
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = (p * 1.35f).coerceIn(0f, 1f)
        translationY = (1f - p) * distancePx
        // A very slight scale-up makes rows feel like they settle into place
        // instead of merely sliding.
        val s = 0.965f + 0.035f * p
        scaleX = s
        scaleY = s
    }
}
