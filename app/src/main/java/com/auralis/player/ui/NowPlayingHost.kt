package com.auralis.player.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.auralis.player.ui.screens.nowplaying.AbLoopSheet
import com.auralis.player.ui.screens.nowplaying.DriveModeScreen
import com.auralis.player.ui.screens.nowplaying.EqualizerSheet
import com.auralis.player.ui.screens.nowplaying.LyricsEditorSheet
import com.auralis.player.ui.screens.nowplaying.LyricsSettingsActions
import com.auralis.player.ui.screens.nowplaying.LyricsSettingsSheet
import com.auralis.player.ui.screens.nowplaying.SpeedSheet
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
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsSettings by viewModel.lyricsSettings.collectAsStateWithLifecycle()
    val editorDraft by viewModel.editorDraft.collectAsStateWithLifecycle()
    val customPresets by viewModel.customPresets.collectAsStateWithLifecycle()
    val savedLoops by viewModel.savedLoops.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val artworkColors by mainViewModel.artworkColors.collectAsStateWithLifecycle()

    // File picker for importing a local .lrc/.txt lyrics file.
    val importLyricsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importLyrics(it) } }

    // Separate picker for a translation sidecar, so importing a translation
    // never overwrites the original lyrics.
    val importTranslationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importTranslation(it) } }

    var showQueue by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showLyricsSettings by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showAbLoop by remember { mutableStateOf(false) }
    var showInlineLyrics by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showDriveMode by remember { mutableStateOf(false) }

    val isDark = com.auralis.player.ui.theme.AuralisTheme.colors.isDark
    LaunchedEffect(playerState.currentSong?.id, isDark) {
        playerState.currentSong?.let { mainViewModel.refreshArtworkColors(it.id, isDark) }
    }

    // Reset the floating lyrics overlay whenever the track changes.
    LaunchedEffect(playerState.currentSong?.id) {
        showInlineLyrics = false
    }

    val seekIncrement = settings.seekIncrementMs.toLong()

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
        onOpenSpeed = { showSpeed = true },
        onOpenAbLoop = { showAbLoop = true },
        onSongMenu = onSongMenu,
        onMarkLoopStart = { viewModel.markLoopStart() },
        onMarkLoopEnd = { viewModel.markLoopEnd() },
        onToggleLoop = { viewModel.toggleLoop(it) },
        onClearLoop = { viewModel.clearLoop() },
        onToggleLyricsOverlay = { showInlineLyrics = !showInlineLyrics },
        onSeekForward = { mainViewModel.player.seekBy(seekIncrement) },
        onSeekBackward = { mainViewModel.player.seekBy(-seekIncrement) },
        onSeekBy = { delta -> mainViewModel.player.seekBy(delta) },
        onOpenCarMode = { showDriveMode = true }
    )

    Box(modifier = modifier.fillMaxSize()) {
        NowPlayingScreen(
            state = playerState,
            positionFlow = viewModel.position,
            isFavorite = isFavorite,
            magnitudes = magnitudes,
            visualizerMode = settings.visualizerMode,
            visualizerIntensity = settings.visualizerIntensity,
            visualizerSpeed = settings.visualizerSpeed,
            loop = loopState,
            sleepActive = sleepState.active || sleepState.stopAtTrackEnd,
            dynamicColor = artworkColors?.primary,
            callbacks = callbacks,
            inlineLyrics = lyrics,
            showInlineLyrics = showInlineLyrics,
            inlineLyricsSettings = lyricsSettings,
            seekStepMs = seekIncrement
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
            val position by viewModel.position.collectAsStateWithLifecycle()
            LyricsPane(
                lyrics = lyrics,
                loading = lyricsLoading,
                downloadState = downloadState,
                settings = lyricsSettings,
                activeIndex = viewModel.activeLyricIndex(position.positionMs),
                title = playerState.currentSong?.title.orEmpty(),
                artist = playerState.currentSong?.displayArtist.orEmpty(),
                currentSong = playerState.currentSong,
                onBack = { showLyrics = false },
                onDownload = { viewModel.downloadLyrics() },
                onOpenSettings = { showLyricsSettings = true },
                onSeekToLine = { mainViewModel.player.seekTo(it) },
                onConsumeFeedback = { viewModel.consumeDownloadFeedback() },
                lyricsOffsetMs = lyricsOffsetMs,
                onAdjustOffset = { viewModel.adjustLyricsOffset(it) },
                onImport = { importLyricsLauncher.launch(arrayOf("*/*")) },
                onEditLyrics = {
                    viewModel.openLyricsEditor()
                    showLyricsEditor = true
                }
            )
        }

        AnimatedVisibility(
            visible = showDriveMode,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(160))
        ) {
            val drivePosition by viewModel.position.collectAsStateWithLifecycle()
            val driveLine = lyrics?.let { l ->
                val index = viewModel.activeLyricIndex(drivePosition.positionMs)
                if (l.synchronized) l.lines.getOrNull(index)?.text else null
            }
            DriveModeScreen(
                song = playerState.currentSong,
                isPlaying = playerState.isPlaying,
                positionFlow = viewModel.position,
                lyricLine = driveLine,
                showLyricLine = settings.driveModeLyrics,
                keepScreenOn = settings.driveModeKeepScreenOn,
                swipeGestures = settings.driveModeSwipeGestures,
                seekSeconds = settings.driveModeSeekSeconds,
                isFavorite = isFavorite,
                onPlayPause = { mainViewModel.player.togglePlayPause() },
                onNext = { mainViewModel.player.next() },
                onPrevious = { mainViewModel.player.previous() },
                onSeekTo = { mainViewModel.player.seekTo(it) },
                onToggleFavorite = { viewModel.toggleFavoriteCurrent() },
                onExit = { showDriveMode = false }
            )
        }
    }

    // Collapse the full player on back ONLY when nothing more specific is
    // consuming it (lyrics pane or a sheet). Composed BEFORE the lyrics
    // handler so the lyrics pane keeps priority over collapsing the player.
    BackHandler(enabled = showDriveMode) { showDriveMode = false }

    BackHandler(
        enabled = !showLyrics && !showQueue && !showSleep && !showEqualizer &&
            !showLyricsSettings && !showSpeed && !showAbLoop && !showDriveMode &&
            !showLyricsEditor
    ) { onCollapse() }

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

    if (showSpeed) {
        SpeedSheet(
            currentSpeed = playerState.speed,
            onDismiss = { showSpeed = false },
            onSpeed = { mainViewModel.player.setSpeed(it) }
        )
    }

    if (showAbLoop) {
        AbLoopSheet(
            loop = loopState,
            positionFlow = viewModel.position,
            onDismiss = { showAbLoop = false },
            onMarkStart = { viewModel.markLoopStart() },
            onMarkEnd = { viewModel.markLoopEnd() },
            onToggle = { viewModel.toggleLoop(it) },
            onClear = { viewModel.clearLoop() }
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

    if (showLyricsEditor) {
        LyricsEditorSheet(
            initialText = editorDraft.text,
            initialTranslation = editorDraft.translation,
            loading = editorDraft.loading,
            persianFont = lyricsSettings.lyricsPersianFont,
            onImportLyrics = { importLyricsLauncher.launch(arrayOf("*/*")) },
            onImportTranslation = { importTranslationLauncher.launch(arrayOf("*/*")) },
            onTimestamp = { viewModel.currentTimestampTag() },
            onSave = { text, translation -> viewModel.saveLyricsEdit(text, translation) },
            onDismiss = { showLyricsEditor = false }
        )
    }

    if (showLyricsSettings) {
        LyricsSettingsSheet(
            settings = lyricsSettings,
            actions = LyricsSettingsActions(
                onFontSize = { v -> viewModel.updateLyricsSettings { setLyricsFontSize(v) } },
                onLineSpacing = { v -> viewModel.updateLyricsSettings { setLyricsLineSpacing(v) } },
                onAlign = { v -> viewModel.updateLyricsSettings { setLyricsAlign(v) } },
                onBoldActive = { v -> viewModel.updateLyricsSettings { setLyricsBoldActive(v) } },
                onShowTranslation = { v -> viewModel.updateLyricsSettings { setLyricsShowTranslation(v) } },
                onTranslationScale = { v -> viewModel.updateLyricsSettings { setLyricsTranslationScale(v) } },
                onTranslationGap = { v -> viewModel.updateLyricsSettings { setLyricsTranslationGap(v) } },
                onInactiveAlpha = { v -> viewModel.updateLyricsSettings { setLyricsInactiveAlpha(v) } },
                onPersianFont = { v -> viewModel.updateLyricsSettings { setLyricsPersianFont(v) } }
            ),
            onDismiss = { showLyricsSettings = false }
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
