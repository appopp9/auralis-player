package com.auralis.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.auralis.player.data.artwork.ArtworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AuralisApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var artworkFetcherFactory: ArtworkFetcherFactory

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
