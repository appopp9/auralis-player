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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.viewModels
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

    private val intentSenderLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        viewModel.consumeIntentSender()
        viewModel.rescan()
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val artworkColors by viewModel.artworkColors.collectAsStateWithLifecycle()
            val pendingSender by viewModel.pendingIntentSender.collectAsStateWithLifecycle()
            val windowSizeClass = calculateWindowSizeClass(this)
            var openNowPlayingSignal by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
                    openNowPlayingSignal++
                }
                requestAudioPermissions()
            }

            LaunchedEffect(pendingSender) {
                pendingSender?.let { sender ->
                    runCatching {
                        intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }.onFailure { viewModel.consumeIntentSender() }
                }
            }

            AuralisTheme(
                themeMode = settings.themeMode,
                accentPalette = settings.accent,
                customAccentArgb = settings.customAccent,
                dynamicAccent = if (settings.dynamicArtworkColor) artworkColors?.primary else null,
                animationsEnabled = settings.animationsEnabled
            ) {
                AuralisAppScaffold(
                    mainViewModel = viewModel,
                    wideLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
                    openNowPlayingSignal = openNowPlayingSignal
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun requestAudioPermissions() {
        if (hasAudioPermission()) {
            viewModel.onPermissionResult(true)
            return
        }
        permissionLauncher.launch(audioPermissions.toTypedArray())
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
