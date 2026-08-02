package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.auralis.player.domain.model.SortOrder
import com.auralis.player.ui.i18n.LocalStrings
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

private fun sortIcon(order: SortOrder): ImageVector = when (order) {
    SortOrder.TITLE_ASC -> Icons.Rounded.SortByAlpha
    SortOrder.TITLE_DESC -> Icons.Rounded.SortByAlpha
    SortOrder.RECENTLY_ADDED -> Icons.Rounded.DateRange
    SortOrder.RECENTLY_PLAYED -> Icons.Rounded.History
    SortOrder.MOST_PLAYED -> Icons.Rounded.PlayCircle
    SortOrder.DURATION -> Icons.Rounded.Timelapse
    SortOrder.YEAR -> Icons.Rounded.AccessTime
    SortOrder.ARTIST -> Icons.Rounded.Person
    SortOrder.ALBUM -> Icons.Rounded.Album
}

/**
 * Small trailing hint showing which way each order runs, so the user does not
 * have to apply a sort to find out what it does.
 */
private fun sortDirection(order: SortOrder): ImageVector? = when (order) {
    SortOrder.TITLE_ASC, SortOrder.ARTIST, SortOrder.ALBUM -> Icons.Rounded.ArrowUpward
    SortOrder.TITLE_DESC,
    SortOrder.RECENTLY_ADDED,
    SortOrder.RECENTLY_PLAYED,
    SortOrder.MOST_PLAYED,
    SortOrder.DURATION,
    SortOrder.YEAR -> Icons.Rounded.ArrowDownward
}

/**
 * Theme-aware sort menu that replaces the stock dropdown. Scales/fades in from
 * its anchor, gives every option an icon inside a tinted tile, marks the active
 * option with an accent wash plus a check, and hints at each order's direction.
 */
@Composable
fun SortMenu(
    expanded: Boolean,
    current: SortOrder,
    options: List<Pair<SortOrder, String>>,
    onSelect: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val strings = LocalStrings.current

    if (!expanded) return

    Popup(
        alignment = Alignment.TopEnd,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(motion.tweenFast()) + scaleIn(
                motion.popSpring(),
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(1f, 0f)
            ),
            exit = fadeOut(motion.tweenFast()) + scaleOut(motion.tweenFast(), targetScale = 0.9f)
        ) {
            GlassPanel(
                modifier = Modifier
                    .widthIn(min = 252.dp)
                    .padding(top = 4.dp, end = 4.dp),
                shape = AuralisTheme.shapes.card
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = strings.sortOrder.uppercase(),
                        style = AuralisType.overline,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(1.dp)
                            .background(colors.outline)
                    )
                    options.forEach { (order, label) ->
                        val selected = order == current
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(AuralisTheme.shapes.small)
                                .then(
                                    if (selected) {
                                        Modifier.background(colors.accentSoft)
                                    } else {
                                        Modifier
                                    }
                                )
                                .combinedClickableCompat { onSelect(order) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Icon tile: gives every option a consistent optical
                            // weight and makes the active row obvious at a glance.
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) colors.accent else colors.surfaceMuted
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = sortIcon(order),
                                    contentDescription = null,
                                    tint = if (selected) colors.onAccent else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = label,
                                style = AuralisType.body,
                                color = if (selected) colors.accent else colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            sortDirection(order)?.let { arrow ->
                                Icon(
                                    imageVector = arrow,
                                    contentDescription = null,
                                    tint = if (selected) colors.accent else colors.textTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
