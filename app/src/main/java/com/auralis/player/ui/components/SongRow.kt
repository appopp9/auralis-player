package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auralis.player.core.Formatters
import com.auralis.player.domain.model.Song
import com.auralis.player.ui.i18n.LocalStrings
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

/**
 * Song row tuned for scrolling: fixed height (no re-measure), no per-row colour
 * animations, no ripple, no subcomposition. Life comes from the entrance
 * animation and the press state, both single-layer.
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    showArtwork: Boolean = true,
    trailingText: String? = null,
    index: Int = 0,
    onToggleFavorite: ((Song) -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    val motion = AuralisTheme.motion
    val strings = LocalStrings.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && motion.enabled) 0.985f else 1f,
        animationSpec = motion.popSpring(),
        label = "rowPress"
    )
    val haptic = LocalHapticFeedback.current
    val fallbackToggle = LocalFavoriteToggle.current
    val toggle = onToggleFavorite ?: fallbackToggle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .appear(index)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AuralisTheme.shapes.small)
            .background(
                when {
                    isPlaying -> colors.accentSoft
                    pressed -> colors.surfaceMuted
                    else -> Color.Transparent
                }
            )
            .combinedClickableCompat(
                interactionSource = interaction,
                onLongClick = onMenu,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(start = spacing.sm, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
            )
        }
        if (showArtwork) {
            SongArtwork(
                songId = song.id,
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(10.dp),
                contentDescription = null,
                maxDecodeSize = 160
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = AuralisType.body,
                color = if (isPlaying) colors.accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.displayArtist} • ${Formatters.duration(song.durationMs)}",
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingText != null) {
            Text(text = trailingText, style = AuralisType.numeric, color = colors.textTertiary)
        }
        if (toggle != null) {
            FavoriteButton(
                favorite = song.isFavorite,
                onToggle = { toggle(song) },
                size = 40.dp
            )
        }
        AccentIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "${strings.more} — ${song.title}",
            onClick = onMenu,
            size = 38.dp,
            tint = colors.textSecondary
        )
    }
}

/** Heart button with an instant, morphing state change. */
@Composable
fun FavoriteButton(
    favorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val strings = LocalStrings.current
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .combinedClickableCompat(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggle()
                }
            )
            .semantics {
                contentDescription = if (favorite) strings.removeFavorite else strings.addFavorite
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = favorite,
            transitionSpec = {
                (scaleIn(motion.popSpring(), initialScale = 0.5f) + fadeIn(motion.tweenFast())) togetherWith
                    (scaleOut(motion.tweenFast(), targetScale = 0.6f) + fadeOut(motion.tweenFast()))
            },
            label = "favorite"
        ) { isFavorite ->
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFavorite) colors.accent else colors.textSecondary,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

@Composable
fun SongGridCard(
    song: Song,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    index: Int = 0
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    PressableSurface(
        onClick = onClick,
        onLongClick = onMenu,
        modifier = modifier.appear(index)
    ) {
        Column(modifier = Modifier.padding(spacing.xs)) {
            SongArtwork(
                songId = song.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = AuralisTheme.shapes.card,
                fallbackIconSize = 36.dp,
                maxDecodeSize = 384
            )
            Text(
                text = song.title,
                style = AuralisType.body,
                color = if (isPlaying) colors.accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = spacing.sm)
            )
            Text(
                text = song.displayArtist,
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CollectionCard(
    title: String,
    subtitle: String,
    artworkSongId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    circular: Boolean = false,
    index: Int = 0
) {
    val colors = AuralisTheme.colors
    val spacing = AuralisTheme.spacing
    PressableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .appear(index)
            .semantics { contentDescription = "$title, $subtitle" }
    ) {
        Column(modifier = Modifier.padding(spacing.xs)) {
            SongArtwork(
                songId = artworkSongId,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .then(if (circular) Modifier.clip(CircleShape) else Modifier),
                shape = if (circular) CircleShape else AuralisTheme.shapes.card,
                fallbackIconSize = 34.dp,
                maxDecodeSize = 384
            )
            Text(
                text = title,
                style = AuralisType.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = spacing.sm)
            )
            Text(
                text = subtitle,
                style = AuralisType.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ListDivider() {
    val colors = AuralisTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AuralisTheme.spacing.screen)
            .height(1.dp)
            .background(colors.outline.copy(alpha = 0.3f))
    )
}
