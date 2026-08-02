package com.auralis.player.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.components.combinedClickableCompat
import com.auralis.player.ui.components.themedContainer
import com.auralis.player.ui.i18n.LocalStrings
import com.auralis.player.ui.i18n.Strings
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType
import com.auralis.player.ui.theme.NavBarStyle

/**
 * Theme-aware bottom navigation.
 *
 * FLOATING  → detached pill hovering above the inset (Aurora / Dynamic /
 *             Experimental)
 * NEUMORPH  → extruded floating bar (Soft UI)
 * HAIRLINE  → opaque bar crowned by a metallic hairline (Luxury)
 * FLAT      → opaque bar with a plain 1dp divider (Scandinavian / AMOLED)
 */
@Composable
fun AuralisBottomBar(
    current: String?,
    onSelect: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val strings = LocalStrings.current

    when (style.navBar) {
        NavBarStyle.FLOATING, NavBarStyle.NEUMORPH -> {
            // Detached pill: content behind it stays visible around the edges.
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = AuralisTheme.spacing.lg, vertical = AuralisTheme.spacing.sm)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .themedContainer(AuralisTheme.shapes.large)
                        .padding(horizontal = AuralisTheme.spacing.sm, vertical = AuralisTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopDestination.entries.forEach { destination ->
                        NavItem(
                            destination = destination,
                            selected = current == destination.route,
                            strings = strings,
                            onClick = { onSelect(destination) }
                        )
                    }
                }
            }
        }

        NavBarStyle.HAIRLINE, NavBarStyle.FLAT -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colors.backgroundElevated)
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            if (style.navBar == NavBarStyle.HAIRLINE) {
                                colors.hairline
                            } else {
                                colors.outline.copy(alpha = 0.6f)
                            }
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AuralisTheme.spacing.sm, vertical = AuralisTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopDestination.entries.forEach { destination ->
                        NavItem(
                            destination = destination,
                            selected = current == destination.route,
                            strings = strings,
                            onClick = { onSelect(destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: TopDestination,
    selected: Boolean,
    strings: Strings,
    onClick: () -> Unit
) {
    val colors = AuralisTheme.colors
    val style = AuralisTheme.style
    val motion = AuralisTheme.motion
    val haptic = LocalHapticFeedback.current
    val tint by animateColorAsState(
        if (selected) colors.accent else colors.textTertiary,
        animationSpec = motion.tweenFast(),
        label = "navTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 0.92f,
        animationSpec = motion.popSpring(),
        label = "navScale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected && !style.flatChrome) 1f else 0f,
        animationSpec = motion.tweenFast(),
        label = "navPill"
    )
    val underlineAlpha by animateFloatAsState(
        targetValue = if (selected && style.flatChrome) 1f else 0f,
        animationSpec = motion.tweenFast(),
        label = "navUnderline"
    )
    val label = destinationLabel(destination, strings)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AuralisTheme.shapes.card)
            .combinedClickableCompat(role = Role.Tab) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(AuralisTheme.shapes.chip)
                    .background(colors.accentSoft)
            )
            Icon(
                imageVector = destination.icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .size(22.dp)
            )
        }
        Text(
            text = label,
            style = AuralisType.overline,
            color = tint,
            modifier = Modifier.padding(top = 3.dp)
        )
        // Flat themes mark selection with a minimal underline, not a pill.
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = 18.dp, height = 2.dp)
                .graphicsLayer { alpha = underlineAlpha }
                .background(colors.accent)
        )
    }
}

/** Navigation rail used on tablets / expanded widths. */
@Composable
fun AuralisNavigationRail(
    current: String?,
    onSelect: (TopDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AuralisTheme.colors
    val strings = LocalStrings.current
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(84.dp)
                .background(colors.backgroundElevated)
                .padding(vertical = AuralisTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AuralisTheme.spacing.md, Alignment.CenterVertically)
        ) {
            TopDestination.entries.forEach { destination ->
                NavItem(
                    destination = destination,
                    selected = current == destination.route,
                    strings = strings,
                    onClick = { onSelect(destination) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(colors.outline.copy(alpha = 0.4f))
        )
    }
}

/** Localised label for a top-level destination. */
fun destinationLabel(destination: TopDestination, strings: Strings): String = when (destination) {
    TopDestination.HOME -> strings.home
    TopDestination.LIBRARY -> strings.library
    TopDestination.PLAYLISTS -> strings.playlists
    TopDestination.FAVORITES -> strings.favorites
    TopDestination.SETTINGS -> strings.settings
}
