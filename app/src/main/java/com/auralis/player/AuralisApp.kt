package com.auralis.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.auralis.player.data.artwork.ArtworkFetcherFactory
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.playback.PlaybackController
import com.auralis.player.widget.WidgetRenderer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AuralisApp : Application(), ImageLoaderFactory {

    @Inject lateinit var artworkFetcherFactory: ArtworkFetcherFactory
    @Inject lateinit var musicRepository: MusicRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var artworkLoader: ArtworkLoader
    @Inject lateinit var playbackController: PlaybackController

    override fun onCreate() {
        super.onCreate()
        // Restore the last played queue (paused) so the notification & widgets
        // have something to show after a process death.
        playbackController.bootstrap()
        // Start pushing state into home-screen widgets.
        WidgetRenderer.start(this, musicRepository, settingsRepository, artworkLoader)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.22)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artwork_cache"))
                    .maxSizeBytes(120L * 1024 * 1024)
                    .build()
            }
            .components {
                add(artworkFetcherFactory)
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
