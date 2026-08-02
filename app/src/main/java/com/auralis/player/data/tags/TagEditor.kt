package com.auralis.player.data.tags

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.FileProvider
import com.auralis.player.data.artwork.ArtworkLoader
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TagUpdate(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val composer: String,
    val year: Int,
    val track: Int,
    val disc: Int,
    val lyrics: String?
)

sealed interface TagWriteResult {
    data object Success : TagWriteResult
    /** The system needs the user to approve the write; launch [intentSender]. */
    data class NeedsPermission(val intentSender: android.content.IntentSender) : TagWriteResult
    data class LocalOnly(val reason: String) : TagWriteResult
}

/**
 * Writes tags through MediaStore where the platform allows it, and always keeps
 * a local override so the library reflects the user's edit either way.
 */
@Singleton
class TagEditor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val artworkLoader: ArtworkLoader
) {

    suspend fun apply(song: Song, update: TagUpdate): TagWriteResult = withContext(Dispatchers.IO) {
        musicRepository.updateTags(
            songId = song.id,
            title = update.title,
            artist = update.artist,
            album = update.album,
            albumArtist = update.albumArtist,
            genre = update.genre,
            composer = update.composer,
            year = update.year,
            track = update.track,
            disc = update.disc,
            lyrics = update.lyrics
        )
        artworkLoader.invalidate(song.id)

        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, update.title)
            put(MediaStore.Audio.Media.ARTIST, update.artist)
            put(MediaStore.Audio.Media.ALBUM, update.album)
            put(MediaStore.Audio.Media.COMPOSER, update.composer)
            if (update.year > 0) put(MediaStore.Audio.Media.YEAR, update.year)
            if (update.track > 0) put(MediaStore.Audio.Media.TRACK, update.disc * 1000 + update.track)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put(MediaStore.Audio.Media.ALBUM_ARTIST, update.albumArtist)
                put(MediaStore.Audio.Media.GENRE, update.genre)
            }
        }

        try {
            context.contentResolver.update(uri, values, null, null)
            TagWriteResult.Success
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val request = MediaStore.createWriteRequest(context.contentResolver, listOf(uri))
                TagWriteResult.NeedsPermission(request.intentSender)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                security is RecoverableSecurityException
            ) {
                TagWriteResult.NeedsPermission(security.userAction.actionIntent.intentSender)
            } else {
                TagWriteResult.LocalOnly("Saved in Auralis only — the file is read-only")
            }
        } catch (t: Throwable) {
            TagWriteResult.LocalOnly("Saved in Auralis only")
        }
    }

    suspend fun delete(song: Song): TagWriteResult = withContext(Dispatchers.IO) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        try {
            context.contentResolver.delete(uri, null, null)
            musicRepository.removeSongsFromLibrary(listOf(song.id))
            TagWriteResult.Success
        } catch (security: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val request = MediaStore.createDeleteRequest(context.contentResolver, listOf(uri))
                TagWriteResult.NeedsPermission(request.intentSender)
            } else {
                TagWriteResult.LocalOnly("Unable to delete this file")
            }
        } catch (t: Throwable) {
            TagWriteResult.LocalOnly("Unable to delete this file")
        }
    }

    fun shareIntent(song: Song): Intent {
        val uri = resolveShareUri(song)
        return Intent(Intent.ACTION_SEND).apply {
            type = song.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} — ${song.displayArtist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun resolveShareUri(song: Song): Uri = runCatching {
        if (song.path.startsWith("/")) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(song.path))
        } else {
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
        }
    }.getOrElse {
        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
    }

    fun canWriteSystemSettings(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(context)

    fun writeSettingsIntent(): Intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Sets the track as the device ringtone. Returns false when not permitted. */
    fun setAsRingtone(song: Song): Boolean {
        if (!canWriteSystemSettings()) return false
        return runCatching {
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.id)
            runCatching {
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Audio.Media.IS_RINGTONE, true) },
                    null,
                    null
                )
            }
            android.media.RingtoneManager.setActualDefaultRingtoneUri(
                context,
                android.media.RingtoneManager.TYPE_RINGTONE,
                uri
            )
            true
        }.getOrDefault(false)
    }

    companion object {
        fun activityOf(context: Context): Activity? {
            var current = context
            while (current is android.content.ContextWrapper) {
                if (current is Activity) return current
                current = current.baseContext
            }
            return null
        }
    }
}
