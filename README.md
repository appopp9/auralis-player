# Auralis — Premium Offline Music Player for Android

Auralis is a fully offline, production-ready music player built with Kotlin, Jetpack Compose and
AndroidX Media3. It ships with its own design system (no stock Material look), a real-time audio
visualizer, waveform seeking, A-B looping, a tag editor, home-screen widgets and listening stats.

> `Auralis` is a placeholder product name — rename `app/src/main/res/values/strings.xml` and the
> `com.auralis.player` package if you want your own branding.

## Highlights

| Area | What you get |
| --- | --- |
| Playback | Media3 / ExoPlayer foreground service, background playback, lock-screen & Bluetooth controls, audio focus, becoming-noisy handling, gapless, crossfade, playback speed |
| Library | Songs, Artists, Album artists, Albums, Genres, Folders, Moods, Playlists, Favorites, fast alphabet scroll, grid/list, 9 sort orders |
| Scanner | Background MediaStore + metadata scan (MP3, FLAC, WAV, AAC, OGG, M4A, OPUS, AIFF, WMA), progress reporting, deleted/duplicate handling, manual rescan, excluded folders |
| Now Playing | Large artwork with fade/scale morphing, waveform seekbar with drag seek, gestures (swipe next/prev/lyrics/dismiss), queue, sleep timer, equalizer, favorite, shuffle & repeat |
| Lyrics | Embedded lyrics, synchronized highlighting with auto-scroll, optional online lookup (opt-in only) |
| Visualizer | Real-time capture with Bars, Wave, Circular, Spectrum, Particle, Minimal and Aurora modes plus sensitivity, smoothing, bar count, speed and intensity controls |
| Audio FX | 5/10-band equalizer, 10 built-in presets, custom presets, bass boost, treble, virtualizer, loudness, balance, volume boost |
| A-B loop | Mark start/end on the waveform, toggle looping, save and manage loops per track |
| Playlists | Create / rename / delete, add & remove tracks, reorder, smart playlists (recently played, most played, recently added, favorites, never played) |
| Queue | Play next, add to queue, remove, clear, reorder, restored after restart |
| Tag editor | Title, artist, album, album artist, genre, composer, year, track, disc, lyrics — written back to the file with scoped-storage consent |
| Widgets | Compact, Medium and Large home-screen widgets themed with the app accent |
| Stats | Total tracks, listening time, top songs/artists/albums/genres, daily chart, history |
| Theming | Light / Dark / AMOLED / System, 8 accents + custom color, dynamic accent extracted from artwork |
| Privacy | 100% offline first, no account, no analytics, network only used if you enable online lyrics |

## Architecture

```
app/src/main/java/com/auralis/player/
├── core/          formatting + fuzzy matching helpers
├── data/
│   ├── artwork/   Coil fetcher + artwork cache
│   ├── db/        Room entities, DAOs, database, mappers
│   ├── lyrics/    embedded + optional online lyrics
│   ├── prefs/     DataStore settings repository
│   ├── repository/ music, playlist and stats repositories
│   ├── scanner/   MediaStore + MediaMetadataRetriever scanner
│   └── tags/      tag writer (scoped storage aware)
├── di/            Hilt modules
├── domain/model/  pure Kotlin models and enums
├── playback/      ExoPlayer service, player connection, effects, visualizer, sleep timer, A-B loop
├── presentation/  ViewModels (MVVM, no business logic in composables)
├── ui/            design system, components, screens, navigation
└── widget/        AppWidget providers and updater
```

- **UI** is pure Compose and only renders state + emits events.
- **Presentation** exposes immutable `StateFlow` state and suspend actions.
- **Domain** holds framework-free models.
- **Data** owns Room, DataStore, MediaStore and file access.
- **Playback** wraps Media3 behind a `PlayerConnection` façade.

## Requirements

- Android Studio Koala or newer
- JDK 17
- Android Gradle Plugin 8.5.2 / Kotlin 1.9.24
- minSdk 26 (Android 8.0), targetSdk / compileSdk 34

## Build

```bash
./gradlew assembleDebug      # debug APK -> app/build/outputs/apk/debug/
./gradlew assembleRelease    # release APK -> app/build/outputs/apk/release/
```

CI is configured in `.github/workflows/android.yml`: every push to `main` builds both variants and
uploads the APKs as workflow artifacts.

## Permissions

| Permission | Why |
| --- | --- |
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | scan and play local audio |
| `POST_NOTIFICATIONS` | media notification on Android 13+ |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | background playback |
| `RECORD_AUDIO` | audio session capture for the visualizer (optional; deny it and the visualizer simply stays idle) |
| `WAKE_LOCK` | uninterrupted playback with the screen off |
| `WRITE_SETTINGS` | optional "set as ringtone" action |

Nothing is uploaded anywhere. Online lyrics are the only network feature and are disabled by default.

## License

MIT — see `LICENSE`.
