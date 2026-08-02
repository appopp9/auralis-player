package com.auralis.player.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

/**
 * Session-level memory of list scroll positions, keyed by screen. It survives
 * navigation re-creation (tab switches, opening a sub-screen and coming back),
 * so returning to a page restores its exact scroll offset instead of jumping
 * back to the top.
 */
private object ScrollMemory {
    private val positions = HashMap<String, Pair<Int, Int>>()
    fun get(key: String): Pair<Int, Int>? = positions[key]
    fun put(key: String, index: Int, offset: Int) {
        positions[key] = index to offset
    }
}

/**
 * A [LazyListState] that restores the saved scroll offset for [key] on create
 * and writes it back on dispose. Drop-in replacement for [rememberLazyListState]
 * for any page whose scroll should persist across navigation.
 */
@Composable
fun rememberPersistentListState(key: String): LazyListState {
    val saved = ScrollMemory.get(key)
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = saved?.first ?: 0,
        initialFirstVisibleItemScrollOffset = saved?.second ?: 0
    )
    DisposableEffect(key) {
        onDispose {
            ScrollMemory.put(key, state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }
    }
    return state
}
