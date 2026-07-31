# Auralis Player — Improvement Work: Progress & Recovery File

This file is the single source of truth for the agent's work on the Auralis
music player improvements. It exists because the agent's temporary workspace
has been wiped twice mid-task; if it happens again, recovery starts here.

- Branch: `agent-wip`
- Baseline source: `auralis-player-2.2-source.zip` (user-provided), versionName `2.2.0`, versionCode `4`
- Working copy layout: project root `app/`, source root `app/src/main/java/com/auralis/player/`

> Note: this branch's `main` ancestor does **not** byte-match the user's zip
> (`app/build.gradle.kts` differs), so all patches in `patches/` are expressed
> against the **zip baseline**, not against `main`.

---

## Recovery procedure (if the workspace is wiped again)

1. Ask the user to re-upload `auralis-player-2.2-source.zip` and the Vazir font zip.
2. Extract to `/data/proj/auralis`, `git init`, commit as the pristine baseline.
3. Fetch every file under `patches/` from the `agent-wip` branch, in numeric order.
4. Apply them: `git apply patches/01-*.patch` … then continue at the first
   unchecked item in the checklist below.
5. Re-copy the Vazir TTFs into `app/src/main/res/font/` (binary files are not
   stored in the patches).

---

## Scope decisions confirmed by the user

| Topic | Decision |
| --- | --- |
| Android Auto | **Dropped** — skipped for now. |
| Chromecast / Cast | **Replaced** by an app-exclusive device-to-device transfer feature (sender / receiver roles; must support sending a whole playlist or the whole Favorites collection). |
| Lyrics translation source | Both: bilingual auto-detect **and** manual import. |
| Vazir variant | Standard Vazir (Latin digits). |

## Known environment limits

- The agent's sandbox has **no Android SDK, no Gradle, no Kotlin compiler and no network**, so `./gradlew build` cannot be run there. Verification is static only.
- The repository does have a working GitHub Actions workflow (`.github/workflows/android.yml`) that builds debug + release APKs, which is the only realistic way to get a true compile check.

---

## Requirement checklist

Status: `[ ]` not started · `[~]` in progress · `[x]` done (static review only)

### Phase A — startup, fonts, settings storage
- [ ] 1. Startup flash: hold a splash until persisted settings load; resolve the start screen **before** the NavHost is built.
- [ ] 5. Vazir font for Persian lyrics, titles, artists and the alphabetical index.
- [ ] 25. Persist all new settings in `SettingsRepository`.

### Phase B — playback
- [ ] 2. Playback speed 0.25x–4x with standard presets.
- [ ] 13. Animated repeat & shuffle buttons.
- [ ] 14. Accurate listening time (monotonic clock segments).
- [ ] 18. Long press on next / previous seeks; step configurable in Settings.
- [ ] 19. Richer notification controls.

### Phase C — lyrics
- [ ] 3. Dual lyrics: original + translation.
- [ ] 4. Advanced lyrics settings with live preview.
- [ ] 6. Lyrics download with real saved / already-saved / offline / failed states.
- [ ] 7. Premium highlight animation and synced scrolling.
- [ ] 8. Floating lyrics on the Now Playing screen + tap-the-cover cross-fade.
- [ ] 9. No touch pass-through under the lyrics overlay.
- [ ] 20. Proper offline state; cached lyrics still shown.

### Phase D — UI polish
- [ ] 10. Playlist pinning.
- [ ] 15. Songs in Statistics are playable.
- [ ] 16. Last-14-days chart: real empty state.
- [ ] 17. Bespoke animated sorting menu.
- [ ] 22. Premium search input and playlist-name input.
- [ ] 23. Bespoke animated delete-playlist dialog.
- [ ] 21 / 24 / 26. Consistency, performance, RTL & accessibility passes.

### Phase E — device-to-device transfer
- [ ] 12b. App-exclusive song / playlist / favorites transfer over the local network.

### Final
- [ ] 27. Full static re-review, then a real compile via GitHub Actions.

---

## Change log

| # | Patch | Contents |
| --- | --- | --- |
| — | — | Repository prepared, `agent-wip` branch created, progress file added. |
