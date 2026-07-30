package com.auralis.player.ui.components

import androidx.compose.runtime.compositionLocalOf
import com.auralis.player.domain.model.Song

/**
 * Favourite toggle provided once by the app scaffold, so every row and the
 * player share one write path and update on the first tap.
 */
val LocalFavoriteToggle = compositionLocalOf<((Song) -> Unit)?> { null }
