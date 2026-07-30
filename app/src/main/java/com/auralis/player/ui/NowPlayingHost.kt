package com.auralis.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.player.domain.model.Song
import com.auralis.player.presentation.MainViewModel
import com.auralis.player.presentation.NowPlayingViewModel
import com.auralis.player.ui.screens.nowplaying.EqualizerSheet
import com.auralis.player.ui.screens.nowplaying.LyricsPane
import com.auralis.player.ui.screens.nowplaying.NowPlayingCallbacks
import com.auralis.player.ui.screens.nowplaying.NowPlayingScreen
import com.auralis.player.ui.screens.nowplaying.QueueSheet
import com.auralis.player.ui.screens.nowplaying.SleepTimerSheet

/**
 * Hosts the full-screen player together with its sheets. Keeps all playback
 * state handling inside view models — the composables stay declarative.
 */
@Composable
fun NowPlayingHost(
    mainViewModel: MainViewModel,
    onCollapse: () -> Unit,
    onSongMenu: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onOpenArtist: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: NowPlayingViewModel = hiltViewModel()

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val sleepState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val loopState by viewModel.loopState.collectAsStateWithLifecycle()
    val magnitudes by viewModel.magnitudes.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsLoading by viewModel.lyricsLoading.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val savedLoops by viewModel.savedLoops.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val artworkColors by mainViewModel.artworkColors.collectAsStateWithLifecycle()

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val isDark = com.auralis.player.ui.theme.AuralisTheme.colors.isDark
    LaunchedEffect(playerState.currentSong?.id, isDark) {
        playerState.currentSong?.let { mainViewModel.refreshArtworkColors(it.id, isDark) }
    }

    val callbacks = NowPlayingCallbacks(
        onCollapse = onCollapse,
        onPlayPause = { mainViewModel.player.togglePlayPause() },
        onNext = { mainViewModel.player.next() },
        onPrevious = { mainViewModel.player.previous() },
        onSeekFraction = { mainViewModel.player.seekToFraction(it) },
        onToggleShuffle = { mainViewModel.player.toggleShuffle() },
        onCycleRepeat = { mainViewModel.player.cycleRepeat() },
        onToggleFavorite = { viewModel.toggleFavoriteCurrent() },
        onOpenQueue = { showQueue = true },
        onOpenSleepTimer = { showSleep = true },
        onOpenEqualizer = { showEqualizer = true },
        onOpenLyrics = { showLyrics = true },
        onSongMenu = onSongMenu,
        onMarkLoopStart = { viewModel.markLoopStart() },
        onMarkLoopEnd = { viewModel.markLoopEnd() },
        onToggleLoop = { viewModel.toggleLoop(it) },
        onClearLoop = { viewModel.clearLoop() }
    )

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingScreen(
            state = playerState,
            isFavorite = isFavorite,
            magnitudes = magnitudes,
            visualizerMode = settings.visualizerMode,
            visualizerIntensity = settings.visualizerIntensity,
            visualizerSpeed = settings.visualizerSpeed,
            loop = loopState,
            sleepActive = sleepState.active || sleepState.stopAtTrackEnd,
            dynamicColor = if (settings.dynamicArtworkColor) artworkColors?.primary else null,
            callbacks = callbacks
        )

        AnimatedVisibility(
            visible = showLyrics,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.86f,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold
                )
            ) { it / 3 } + fadeIn(tween(180)),
            exit = slideOutVertically(tween(220)) { it / 3 } + fadeOut(tween(140))
        ) {
            LyricsPane(
                lyrics = lyrics,
                loading = lyricsLoading,
                activeIndex = viewModel.activeLyricIndex(playerState.positionMs),
                title = playerState.currentSong?.title.orEmpty(),
                artist = playerState.currentSong?.displayArtist.orEmpty(),
                onBack = { showLyrics = false },
                onSearchOnline = { viewModel.searchLyricsOnline() },
                onSeekToLine = { mainViewModel.player.seekTo(it) }
            )
        }
    }

    BackHandler(enabled = showLyrics) { showLyrics = false }

    if (showQueue) {
        QueueSheet(
            queue = playerState.queue,
            currentIndex = playerState.queueIndex,
            onDismiss = { showQueue = false },
            onSelect = { mainViewModel.player.skipToQueueIndex(it) },
            onRemove = { mainViewModel.player.removeFromQueue(it) },
            onMove = { from, to -> mainViewModel.player.moveQueueItem(from, to) },
            onClear = {
                mainViewModel.player.clearQueue()
                showQueue = false
            }
        )
    }

    if (showSleep) {
        SleepTimerSheet(
            state = sleepState,
            onDismiss = { showSleep = false },
            onStart = { viewModel.startSleepTimer(it) },
            onEndOfTrack = { viewModel.sleepAfterTrack() },
            onCancel = { viewModel.cancelSleepTimer() }
        )
    }

    if (showEqualizer) {
        EqualizerSheet(
            state = equalizerState,
            builtInPresets = viewModel.builtInPresets,
            customPresets = customPresets,
            loop = loopState,
            savedLoops = savedLoops,
            onDismiss = { showEqualizer = false },
            onToggleEnabled = { viewModel.setEqEnabled(it) },
            onBandChange = { index, level -> viewModel.setBand(index, level) },
            onApplyPreset = { viewModel.applyPreset(it) },
            onSavePreset = { viewModel.saveCurrentAsPreset(it) },
            onDeletePreset = { viewModel.deletePreset(it) },
            onBassBoost = { viewModel.setBassBoost(it) },
            onTreble = { viewModel.setTreble(it) },
            onVirtualizer = { viewModel.setVirtualizer(it) },
            onLoudness = { viewModel.setLoudness(it) },
            onApplyLoop = { viewModel.applySavedLoop(it) },
            onDeleteLoop = { viewModel.deleteSavedLoop(it) },
            onSaveLoop = { viewModel.saveLoop(it) },
            onClearLoop = { viewModel.clearLoop() }
        )
    }
}
