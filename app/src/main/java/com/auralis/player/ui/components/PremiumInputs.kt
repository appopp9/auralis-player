package com.auralis.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.auralis.player.ui.theme.AuralisTheme
import com.auralis.player.ui.theme.AuralisType

/**
 * Premium search field, custom-drawn to match the app's design system: a soft
 * themed container, a leading icon that lights up on focus, an accent border
 * that fades in while focused, and an animated clear button. No stock Material
 * outline.
 */
@Composable
fun AuralisSearchField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onSearch: (() -> Unit)? = null
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = motion.tweenFast(),
        label = "searchBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused) 1.5.dp else 1.dp,
        animationSpec = motion.tweenFast(),
        label = "searchBorderW"
    )
    val iconTint by animateColorAsState(
        targetValue = if (focused) colors.accent else colors.textTertiary,
        animationSpec = motion.tweenFast(),
        label = "searchIcon"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(AuralisTheme.shapes.chip)
            .background(colors.surfaceMuted)
            .border(
                width = borderWidth,
                color = colors.accent.copy(alpha = 0.65f * borderAlpha),
                shape = AuralisTheme.shapes.chip
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (value.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = AuralisType.body,
                    color = colors.textTertiary
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                interactionSource = interaction,
                textStyle = AuralisType.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            )
        }

        AnimatedVisibility(
            visible = value.text.isNotEmpty(),
            enter = fadeIn(motion.tweenFast()) + scaleIn(motion.popSpring(), initialScale = 0.6f),
            exit = fadeOut(motion.tweenFast()) + scaleOut(motion.tweenFast(), targetScale = 0.6f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .combinedClickableCompat {
                        onValueChange(TextFieldValue("", TextRange(0)))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Premium single-line text field (e.g. playlist name). Custom container with a
 * focus glow, a subtle fill shift, validation-driven error state and helper /
 * error text. No stock Material outline.
 */
@Composable
fun AuralisTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    helperText: String? = null,
    focusRequester: FocusRequester? = null,
    singleLine: Boolean = true
) {
    val colors = AuralisTheme.colors
    val motion = AuralisTheme.motion
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> colors.danger
            focused -> colors.accent
            else -> colors.outline.copy(alpha = 0.5f)
        },
        animationSpec = motion.tweenFast(),
        label = "fieldBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused || isError) 1.5.dp else 1.dp,
        animationSpec = motion.tweenFast(),
        label = "fieldBorderW"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .clip(AuralisTheme.shapes.small)
                .background(if (focused) colors.surface else colors.surfaceMuted)
                .border(width = borderWidth, color = borderColor, shape = AuralisTheme.shapes.small)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(text = placeholder, style = AuralisType.body, color = colors.textTertiary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                interactionSource = interaction,
                textStyle = AuralisType.body.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(if (isError) colors.danger else colors.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            )
        }

        val message = if (isError) errorText else helperText
        if (!message.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(start = 6.dp, top = 6.dp)
            ) {
                if (isError) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = colors.danger,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = message,
                    style = AuralisType.bodySmall,
                    color = if (isError) colors.danger else colors.textTertiary
                )
            }
        }
    }
}
