package com.auralis.player.data.backup

import android.content.Context
import android.net.Uri
import com.auralis.player.data.prefs.SettingsRepository
import com.auralis.player.data.repository.MusicRepository
import com.auralis.player.data.repository.PlaylistRepository
import com.auralis.player.domain.model.AccentPalette
import com.auralis.player.domain.model.AppTheme
import com.auralis.player.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class RestoreSummary(
    val favorites: Int,
    val pinned: Int,
    val playlists: Int,
    val settingsApplied: Boolean
)

/**
 * Encrypted backup of everything personal: favorites, pinned songs, playlists
 * and the visual/theme settings.
 *
 * Format: `AURB1` magic + IV + AES-256/GCM ciphertext of a JSON payload.
 * The key is derived inside the app, so backup files are opaque blobs to any
 * other program — only Auralis can open them — while still restoring fine on
 * a fresh install or another device.
 *
 * Songs are referenced by absolute file path (MediaStore ids differ between
 * devices), so restores match tracks wherever the files live again.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val settingsRepository: SettingsRepository
) {

    // ------------------------------------------------------------------
    // Crypto
    // ------------------------------------------------------------------

    private val magic = "AURB1".toByteArray(Charsets.US_ASCII)

    private fun key(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("auralis.backup.v1::com.auralis.player::x7Q9mK2pL5vT8sW1".toByteArray())
        return SecretKeySpec(digest, "AES")
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return magic + byteArrayOf(iv.size.toByte()) + iv + ciphertext
    }

    private fun decrypt(data: ByteArray): ByteArray {
        require(data.size > magic.size + 14 && data.copyOfRange(0, magic.size).contentEquals(magic)) {
            "This file is not an Auralis backup"
        }
        val ivLen = data[magic.size].toInt()
        val ivStart = magic.size + 1
        val iv = data.copyOfRange(ivStart, ivStart + ivLen)
        val ciphertext = data.copyOfRange(ivStart + ivLen, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    // ------------------------------------------------------------------
    // Payload
    // ------------------------------------------------------------------

    private suspend fun buildPayload(): JSONObject {
        // Cold start: give the library a moment to load before snapshotting.
        withTimeoutOrNull(15_000) { musicRepository.songs.first { it.isNotEmpty() } }

        val settings = settingsRepository.settings.first()
        val songs = musicRepository.songs.value
        val songById = songs.associateBy { it.id }

        val favorites = JSONArray()
        songs.filter { it.isFavorite }.forEach { favorites.put(it.path) }

        val pinned = JSONArray()
        settings.pinnedSongs.mapNotNull { songById[it]?.path }.forEach { pinned.put(it) }

        val playlists = JSONArray()
        playlistRepository.playlists.value.forEach { playlist ->
            val members = playlistRepository.songsOf(playlist.id).first()
            val arr = JSONArray()
            members.forEach { arr.put(it.path) }
            playlists.put(
                JSONObject()
                    .put("name", playlist.name)
                    .put("songs", arr)
            )
        }

        val theme = JSONObject()
            .put("appTheme", settings.appTheme.name)
            .put("themeMode", settings.themeMode.name)
            .put("accent", settings.accent.name)
            .put("customAccent", settings.customAccent)
            .put("startScreen", settings.startScreen)
            .put("gridStyle", settings.gridStyle.name)

        return JSONObject()
            .put("format", "auralis-backup")
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("theme", theme)
            .put("favorites", favorites)
            .put("pinned", pinned)
            .put("playlists", playlists)
    }

    private suspend fun applyPayload(json: JSONObject): RestoreSummary {
        require(json.optString("format") == "auralis-backup") { "This file is not an Auralis backup" }

        // Wait for the library so path matching has something to match against.
        withTimeoutOrNull(15_000) { musicRepository.songs.first { it.isNotEmpty() } }
        val byPath = musicRepository.songs.value.associateBy { it.path }

        // --- theme / settings ---
        var settingsApplied = false
        json.optJSONObject("theme")?.let { theme ->
            runCatching { AppTheme.valueOf(theme.optString("appTheme")) }
                .getOrNull()?.let { settingsRepository.setAppTheme(it) }
            runCatching { ThemeMode.valueOf(theme.optString("themeMode")) }
                .getOrNull()?.let { settingsRepository.setThemeMode(it) }
            runCatching { AccentPalette.valueOf(theme.optString("accent")) }
                .getOrNull()?.let { settingsRepository.setAccent(it) }
            if (theme.has("customAccent")) settingsRepository.setCustomAccent(theme.optLong("customAccent"))
            theme.optString("startScreen").takeIf { it.isNotBlank() }
                ?.let { settingsRepository.setStartScreen(it) }
            runCatching { com.auralis.player.domain.model.GridStyle.valueOf(theme.optString("gridStyle")) }
                .getOrNull()?.let { settingsRepository.setGridStyle(it) }
            settingsApplied = true
        }

        // --- favorites ---
        var favoriteCount = 0
        json.optJSONArray("favorites")?.let { favorites ->
            for (i in 0 until favorites.length()) {
                byPath[favorites.optString(i)]?.let { song ->
                    musicRepository.setFavorite(song.id, true)
                    favoriteCount++
                }
            }
        }

        // --- pinned ---
        var pinnedCount = 0
        json.optJSONArray("pinned")?.let { pinned ->
            val ids = mutableSetOf<Long>()
            for (i in 0 until pinned.length()) {
                byPath[pinned.optString(i)]?.let { ids += it.id }
            }
            if (ids.isNotEmpty()) {
                settingsRepository.setPinnedSongs(ids)
                pinnedCount = ids.size
            }
        }

        // --- playlists (skip name collisions to avoid duplicates) ---
        var playlistCount = 0
        json.optJSONArray("playlists")?.let { playlists ->
            val existing = playlistRepository.playlists.value.map { it.name }.toSet()
            for (i in 0 until playlists.length()) {
                val entry = playlists.optJSONObject(i) ?: continue
                val name = entry.optString("name")
                if (name.isBlank() || name in existing) continue
                val songIds = mutableListOf<Long>()
                entry.optJSONArray("songs")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        byPath[arr.optString(j)]?.let { songIds += it.id }
                    }
                }
                playlistRepository.create(name, songIds)
                playlistCount++
            }
        }

        return RestoreSummary(favoriteCount, pinnedCount, playlistCount, settingsApplied)
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val bytes = encrypt(buildPayload().toString().toByteArray(Charsets.UTF_8))
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("Could not open the selected location")
    }

    suspend fun importFrom(uri: Uri): RestoreSummary = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not read the selected file")
        val json = JSONObject(String(decrypt(bytes), Charsets.UTF_8))
        applyPayload(json)
    }

    /** Runs at app start; writes a dated backup at most once per day. */
    suspend fun autoBackupIfDue() {
        val settings = settingsRepository.settings.first()
        if (!settings.autoBackupEnabled) return
        val dayMs = 20L * 60 * 60 * 1000 // ~daily, tolerant of open times
        if (System.currentTimeMillis() - settings.lastAutoBackupAt < dayMs) return

        withContext(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
            dir.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
            val file = File(dir, "Auralis-auto-$stamp.aur")
            file.writeBytes(encrypt(buildPayload().toString().toByteArray(Charsets.UTF_8)))
            // keep the 7 most recent auto backups
            dir.listFiles { f -> f.name.startsWith("Auralis-auto-") }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(7)
                ?.forEach { it.delete() }
        }
        settingsRepository.setLastAutoBackup(System.currentTimeMillis())
    }

    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
        return "Auralis-backup-$stamp.aur"
    }
}
