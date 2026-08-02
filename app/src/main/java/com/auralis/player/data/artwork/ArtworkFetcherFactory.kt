package com.auralis.player.data.artwork

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Coil fetcher for `auralis://artwork/{songId}` URIs. */
@Singleton
class ArtworkFetcherFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artworkLoader: ArtworkLoader
) : Fetcher.Factory<Uri> {

    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (data.scheme != ArtworkLoader.SCHEME) return null
        val songId = ArtworkLoader.songIdOf(data) ?: return null
        return ArtworkFetcher(songId, options)
    }

    private inner class ArtworkFetcher(
        private val songId: Long,
        private val options: Options
    ) : Fetcher {
        override suspend fun fetch(): FetchResult? {
            val requested = when (val dim = options.size.width) {
                is Dimension.Pixels -> dim.px.coerceIn(96, 1024)
                else -> 512
            }
            val bitmap = artworkLoader.load(songId, requested) ?: return null
            return DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        }
    }
}
