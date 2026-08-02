package com.auralis.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.viewModels
import com.auralis.player.core.AppShortcuts
import com.auralis.player.domain.model.VisualizerMode
import com.auralis.player.presentation.MainViewModel
import com.auralis.player.ui.AuralisAppScaffold
import com.auralis.player.ui.theme.AuralisTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val audioGranted = grants.entries.any { (permission, granted) ->
            granted && permission in audioPermissions
        }
        viewModel.onPermissionResult(audioGranted || hasAudioPermission())
    }

    /**
     * The visualiser reads the output mix through Visualizer, which needs
     * RECORD_AUDIO. It is requested lazily — only once, and only when the user
     * actually has a visualiser mode switched on — so a first launch is never
     * greeted by a microphone prompt.
     */
    private var visualizerPermissionAsked = false

    private val visualizerPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The visualiser re-checks the permission before it starts. */ }

    private val intentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.consumeIntentSender()
        viewModel.rescan()
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(viewModel::exportBackup) }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importBackup) }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Hold the system splash until the saved theme / start screen have
        // loaded, so the default theme and Home page never flash on launch.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { !viewModel.uiReady.value }

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val artworkColors by viewModel.artworkColors.collectAsStateWithLifecycle()
            val pendingSender by viewModel.pendingIntentSender.collectAsStateWithLifecycle()
            val uiReady by viewModel.uiReady.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            var openNowPlayingSignal by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
                    openNowPlayingSignal++
                }
                requestAudioPermissions()
                consumeShortcutIntent()
            }

            LaunchedEffect(settings.visualizerMode, uiReady) {
                if (uiReady) maybeRequestVisualizerPermission(settings.visualizerMode)
            }

            LaunchedEffect(pendingSender) {
                pendingSender?.let { sender ->
                    runCatching {
                        intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }.onFailure { viewModel.consumeIntentSender() }
                }
            }

            AuralisTheme(
                appTheme = settings.appTheme,
                themeMode = settings.themeMode,
                accentPalette = settings.accent,
                customAccentArgb = settings.customAccent,
                // Artwork colours only feed the DYNAMIC theme's pre-Android-12
                // fallback; explicit accent picks always win elsewhere.
                dynamicAccent = artworkColors?.primary,
                animationsEnabled = settings.animationsEnabled
            ) {
                // Nothing composes until the first persisted settings snapshot
                // is ready, so the user only ever sees their real theme + screen.
                if (uiReady) {
                    AuralisAppScaffold(
                        mainViewModel = viewModel,
                        wideLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
                        openNowPlayingSignal = openNowPlayingSignal,
                        startRoute = settings.startScreen,
                        onExportBackup = { exportBackupLauncher.launch(viewModel.backupFileName()) },
                        onImportBackup = { importBackupLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShortcutIntent()
    }

    /** D23 — acts on a launcher shortcut, then clears it so it fires once. */
    private fun consumeShortcutIntent() {
        val current = intent ?: return
        val action = current.getStringExtra(AppShortcuts.EXTRA_ACTION) ?: return
        val songId = current.getLongExtra(AppShortcuts.EXTRA_SONG_ID, 0L)
        current.removeExtra(AppShortcuts.EXTRA_ACTION)
        viewModel.handleShortcut(action, songId)
    }

    private fun requestAudioPermissions() {
        if (hasAudioPermission()) {
            viewModel.onPermissionResult(true)
            return
        }
        permissionLauncher.launch(audioPermissions.toTypedArray())
    }

    private fun maybeRequestVisualizerPermission(mode: VisualizerMode) {
        if (mode == VisualizerMode.OFF || visualizerPermissionAsked) return
        if (!hasAudioPermission()) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        visualizerPermissionAsked = true
        runCatching { visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }

    private fun hasAudioPermission(): Boolean = audioPermissions.any { permission ->
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private val audioPermissions: List<String>
        get() = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "com.auralis.player.OPEN_NOW_PLAYING"
    }
}
