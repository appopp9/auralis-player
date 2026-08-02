package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.auralis.player.ui.theme.localizedStyle

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
    isPinned: Boolean = false,
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
            // The playing row gets a directional accent wash that fades out
            // towards the trailing edge, so it reads as "lit up" instead of a
            // flat coloured block.
            .then(
                when {
                    isPlaying -> Modifier.background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.accentSoft,
                                colors.accentSoft.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        )
                    )
                    pressed -> Modifier.background(colors.surfaceMuted)
                    else -> Modifier
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
            NowPlayingBar(color = colors.accent, animate = motion.enabled)
        }
        if (showArtwork) {
            Box(contentAlignment = Alignment.Center) {
                SongArtwork(
                    songId = song.id,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentDescription = null,
                    maxDecodeSize = 160
                )
                if (isPlaying) {
                    // Dim the cover and float the equaliser bars on top of it —
                    // the signature "this is the track you're hearing" cue.
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.46f)),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayingIndicator(
                            isPlaying = true,
                            color = Color.White,
                            barWidth = 3.dp,
                            maxHeight = 18.dp,
                            minHeight = 4.dp
                        )
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        tint = colors.accent,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(13.dp)
                    )
                }
                Text(
                    text = song.title,
                    style = localizedStyle(AuralisType.body, song.title),
                    color = if (isPlaying) colors.accent else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${song.displayArtist} • ${Formatters.duration(song.durationMs)}",
                style = localizedStyle(AuralisType.bodySmall, song.displayArtist),
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

/**
 * Slim accent rail on the leading edge of the currently-playing row. It breathes
 * between two heights so the row feels alive even when the artwork is dark.
 * Only ever composed for the single playing row, so the infinite transition is
 * never allocated for the rest of the list.
 */
@Composable
private fun NowPlayingBar(color: Color, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "nowPlayingBar")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nowPlayingBarPulse"
    )
    val fraction = if (animate) pulse else 1f
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(12.dp + 18.dp * fraction)
            .clip(CircleShape)
            .background(color)
    )
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
                style = localizedStyle(AuralisType.body, song.title),
                color = if (isPlaying) colors.accent else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = spacing.sm)
            )
            Text(
                text = song.displayArtist,
                style = localizedStyle(AuralisType.bodySmall, song.displayArtist),
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
                style = localizedStyle(AuralisType.body, title),
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
