package com.auralis.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.auralis.player.data.prefs.AppSettings
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.scanner.MediaScanner
import com.auralis.player.ui.library.LibraryScreen
import com.auralis.player.ui.theme.AuralisTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity entry point. Asks for audio permission on first launch,
 * kicks off a media scan, then mounts the Compose tree under [AuralisTheme].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var mediaScanner: MediaScanner

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        lifecycleScope.launch { mediaScanner.scan() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            AuralisTheme(
                themeMode = settings.themeMode,
                accent = settings.accent,
                customAccent = settings.customAccent,
                dynamicColor = settings.dynamicArtworkColor
            ) {
                LibraryScreen()
            }
        }
        requestAudioPermissionIfNeeded()
    }

    private fun requestAudioPermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        lifecycleScope.launch {
            val alreadyGranted = permission.any { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
            if (!alreadyGranted) {
                permissionLauncher.launch(permission)
            } else if (!settingsRepository.settings.first().firstScanDone) {
                mediaScanner.scan()
            }
        }
    }
}
