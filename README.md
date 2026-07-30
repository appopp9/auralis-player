# Auralis Player

A local-first Android music player built with Kotlin, Jetpack Compose, Hilt, Room, and Media3.

## What was fixed in this revision

### Build infrastructure (was completely missing)
- Added root `build.gradle.kts` with plugin version catalog (AGP 8.5.0, Kotlin 1.9.24, KSP, Hilt 2.51.1)
- Added `settings.gradle.kts` with `pluginManagement` and `dependencyResolutionManagement`
- Added `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.7)
- Added `gradle/wrapper/gradle-wrapper.jar` (real binary, downloaded from official Gradle repo)
- Added `gradlew` / `gradlew.bat` shell scripts
- Added `.gitignore`

### Source code (referenced by manifest or imports but missing)
- `core/Fuzzy.kt` — fuzzy matching utility used by `MusicRepository.search()`
- `di/ApplicationScope.kt` + `di/IoDispatcher.kt` — Hilt qualifiers
- `di/AppModule.kt` — Hilt module providing Room DB, all DAOs, ApplicationScope, IoDispatcher
- `playback/PlaybackService.kt` — Media3 `MediaSessionService` (declared in manifest)
- `playback/PlaybackController.kt` — singleton holding the `ExoPlayer`, bridges to `MusicRepository`
- `widget/CompactWidgetProvider.kt`, `MediumWidgetProvider.kt`, `LargeWidgetProvider.kt` (declared in manifest)
- `widget/WidgetRenderer.kt` — shared RemoteViews renderer for all three widget sizes
- `MainActivity.kt` — Compose entry activity, handles permission + initial scan
- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` — Material3 theme
- `ui/library/LibraryViewModel.kt`, `LibraryScreen.kt` — minimal Compose library screen so the app launches

### Bugs fixed in existing files
- `StatsRepository.kt`: `daily` chart was using `takeLast(14)` on a DESC-ordered list (gave oldest 14 days instead of most recent 14). Switched to `take(14).reversed()`.
- `PlaybackService.onDestroy`: was releasing the singleton-owned `ExoPlayer`. Now only releases the `MediaSession` — the player's lifecycle is managed by Hilt's SingletonComponent.

### AuralisApp.kt
- Now also bootstraps `PlaybackController` (restores last queue) and starts `WidgetRenderer` on app startup.

## How to build

This project is a standard Android Gradle project. Open it in **Android Studio Iguana / Koala Feature Drop or newer** and let Gradle sync.

### Command-line build

```bash
# 1. Set SDK location (skip if ANDROID_HOME is already set system-wide)
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# 2. Build debug APK
./gradlew assembleDebug

# 3. Install on a connected device
./gradlew installDebug
# or
adb install app/build/outputs/apk/debug/app-debug.apk
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Requirements
- JDK 17 (the project sets `jvmTarget = "17"`)
- Android SDK with `compileSdk = 34` (Android 14) and Build Tools 34.x
- Android Studio Iguana+ (for the IDE; CLI works with just JDK 17 + Android SDK)

## Known limitations / next steps

The data layer (scanner, repositories, lyrics, tags, artwork) is complete. The UI is a minimal placeholder showing the song list — the original design clearly had much more (player screen, equalizer, queue, stats, settings). You'll want to add:

- `ui/player/` — full-screen now-playing view with seekbar, AB-loop, lyrics overlay
- `ui/settings/` — theme/accent picker, audio settings, excluded folders
- `ui/equalizer/` — EQ panel using `android.media.audiofx.Equalizer`
- `ui/stats/` — listening history charts
- A `Player.Listener` in `PlaybackController` to record play counts at the right time (currently fires once on `playQueue`, which is too early)
- A `MediaController` connection in the UI (recommended Media3 pattern) instead of talking to `player` directly

## Architecture

```
com.auralis.player/
├── AuralisApp.kt         — Application; Coil ImageLoader; bootstrap
├── MainActivity.kt       — single Activity; permission + scan trigger
├── core/                 — pure-Kotlin utilities (Fuzzy)
├── di/                   — Hilt modules & qualifiers
├── domain/model/         — plain data classes (Song, Album, …)
├── data/
│   ├── db/               — Room entities, DAOs, mappers, database
│   ├── scanner/          — MediaStore-based library scanner
│   ├── repository/       — MusicRepository, PlaylistRepository, StatsRepository
│   ├── prefs/            — DataStore-backed AppSettings
│   ├── lyrics/           — embedded / sidecar / LRCLIB lyrics
│   ├── tags/             — MediaStore tag editor + ringtone / share
│   └── artwork/          — Coil fetcher + LruCache
├── playback/             — ExoPlayer + MediaSessionService
├── widget/               — AppWidget providers + shared renderer
└── ui/                   — Compose UI (theme + library screen)
```
