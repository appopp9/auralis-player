package com.auralis.player.data.db

import com.auralis.player.domain.model.AbLoop
import com.auralis.player.domain.model.EqualizerPreset
import com.auralis.player.domain.model.Song

fun SongEntity.toDomain(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    albumArtist = albumArtist,
    genre = genre,
    composer = composer,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    durationMs = durationMs,
    path = path,
    folderPath = folderPath,
    folderName = folderName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    isFavorite = isFavorite,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    mood = mood,
    lyrics = lyrics
)

fun AbLoopEntity.toDomain(): AbLoop = AbLoop(id, songId, label, startMs, endMs)

fun EqPresetEntity.toDomain(): EqualizerPreset = EqualizerPreset(
    id = id,
    name = name,
    bandLevels = levels.split(",").mapNotNull { it.trim().toIntOrNull() },
    isBuiltIn = false
)
