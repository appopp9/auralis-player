package com.auralis.player.playback

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.domain.model.Song

object MediaItems {

    const val EXTRA_SONG_ID = "auralis.song_id"

    fun contentUri(songId: Long): Uri =
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

    fun from(song: Song): MediaItem {
        val extras = Bundle().apply { putLong(EXTRA_SONG_ID, song.id) }
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.displayArtist)
            .setAlbumTitle(song.displayAlbum)
            .setAlbumArtist(song.albumArtist.ifBlank { song.displayArtist })
            .setGenre(song.genre)
            .setTrackNumber(song.trackNumber.takeIf { it > 0 })
            .setDiscNumber(song.discNumber.takeIf { it > 0 })
            .setArtworkUri(ArtworkLoader.uriFor(song.id))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(if (song.path.startsWith("content://")) Uri.parse(song.path) else contentUri(song.id))
            .setMediaMetadata(metadata)
            .build()
    }

    fun songId(item: MediaItem?): Long =
        item?.mediaId?.toLongOrNull() ?: -1L
}
