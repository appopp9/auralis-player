package com.auralis.player.core

import java.util.Locale
import java.util.concurrent.TimeUnit

object Formatters {

    fun duration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun longDuration(ms: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(ms)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${TimeUnit.MILLISECONDS.toSeconds(ms)}s"
        }
    }

    fun fileSize(bytes: Long): String {
        if (bytes <= 0) return "—"
        val mb = bytes / 1024.0 / 1024.0
        return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
        else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }

    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night listening"
    }
}
