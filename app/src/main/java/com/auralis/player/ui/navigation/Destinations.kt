package com.auralis.player.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val PLAYLISTS = "playlists"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val SEARCH = "search"
    const val STATS = "stats"
    const val WRAPPED = "wrapped"
    const val EQUALIZER = "equalizer"
    const val FOLDER_BROWSER = "folder_browser"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{name}"
    const val GENRE = "genre/{name}"
    const val MOOD = "mood/{name}"
    const val FOLDER = "folder/{path}"
    const val PLAYLIST = "playlist/{playlistId}"
    const val TAG_EDITOR = "tags/{songId}"
    const val SMART_PLAYLIST = "smart/{kind}"
    const val SMART_CUSTOM = "smart_custom/{smartId}"
    const val SMART_EDITOR = "smart_editor/{smartId}"

    fun album(id: Long) = "album/$id"
    fun artist(name: String) = "artist/${encode(name)}"
    fun genre(name: String) = "genre/${encode(name)}"
    fun mood(name: String) = "mood/${encode(name)}"
    fun folder(path: String) = "folder/${encode(path)}"
    fun playlist(id: Long) = "playlist/$id"
    fun tagEditor(songId: Long) = "tags/$songId"
    fun smart(kind: String) = "smart/$kind"
    fun smartCustom(id: Long) = "smart_custom/$id"

    /** id 0 opens the editor on a brand new smart playlist. */
    fun smartEditor(id: Long) = "smart_editor/$id"

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
    fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}

enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    LIBRARY(Routes.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
    PLAYLISTS(Routes.PLAYLISTS, "Playlists", Icons.Rounded.QueueMusic),
    FAVORITES(Routes.FAVORITES, "Favorites", Icons.Rounded.Favorite),
    SETTINGS(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
}
