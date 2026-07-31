# Auralis Player — Progress & Recovery File

Source of truth: the `auralis-player-2.2-source.zip` the user uploaded in chat
(versionName 2.2.0). The `main` branch of this repository is an **older 1.0.0**
tree and must be ignored.

Audit of the uploaded zip shows a large part of the work is already inside it
(it is the output of an earlier session), so the remaining work is the list
below.

## Confirmed scope changes

- Android Auto: dropped.
- Chromecast: dropped, replaced by an app-exclusive device-to-device transfer
  (sender / receiver, single song, whole playlist or all favorites).
- Lyrics translation: bilingual auto-detect **and** manual import.
- Vazir standard (Latin digits).

## Status

### Already present in the uploaded source (verified by reading the code)
- 1. Splash held until DataStore settings load; start screen resolved before the NavHost (`MainActivity`, `themes.xml`, `AuralisAppScaffold`).
- 2. Playback speed 0.25x–4x with presets (`ControlSheets.kt`).
- 3. Dual lyrics data model (`LyricLine.translation`, `Lyrics.hasTranslation`).
- 5. Vazir + RTL detection (`ui/theme/Script.kt`, `AppFonts.kt`); fonts in `res/font/`.
- 6 / 20. Lyrics fetch states incl. offline (`LyricsFetchStatus`, `LyricsDownloadState`).
- 9. Touch blocker over the lyrics overlay (`PointerEventPass.Final` in `NowPlayingHost.kt`).
- 14. Monotonic-clock listening accounting (`PlaybackService`, `HistoryDao.addPlayedMs`).
- 18 / 19. Long-press seek on next/previous + extended notification controls.
- 25. All new settings persisted (`SettingsRepository`).

### Done in this session
- 15. Statistics rows now play the tapped track through the shared queue.
- 16. Fourteen-day chart replaced by a proper empty panel until real data exists; zero-height bars no longer faked with a 2% floor.

### Remaining
- 4. Advanced lyrics settings sheet with live preview.
- 7. Richer lyrics highlight animation (verify current quality first).
- 8. Floating lyrics on the player + album-cover tap cross-fade (`lyricsOnPlayer` setting exists but no UI consumes it yet).
- 10. Playlist pin/unpin UI + prioritized ordering (repository + view model exist, UI does not).
- 17. Bespoke animated sorting menu (still a Material `DropdownMenu` in `LibraryScreen.kt`).
- 22. Premium search / playlist-name inputs.
- 23. Bespoke animated delete-playlist dialog (still `AlertDialog` in `PlaylistsScreen.kt`).
- 12b. Device-to-device transfer.
- 21 / 24 / 26 / 27. Consistency, performance, RTL passes and final review.

## Recovery

If the agent workspace is wiped: ask the user to re-upload the latest zip,
extract to `/data/proj/auralis`, then continue from the "Remaining" list.
The agent sandbox has no network and no Android SDK, so compiling is only
possible through this repository's GitHub Actions workflow.
