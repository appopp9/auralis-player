# Auralis Player 3.0

A local-first, premium Android music player. Kotlin · Jetpack Compose · Hilt · Room · Media3.

## What's new in 3.0 — stability & "every switch is real" pass

- **Audio effects completed** — target-loudness normalization (A3) and preset reverb (A6) are now implemented in `AudioEffectsController` (the service already called them, which previously broke the build).
- **Treble no longer fights the equaliser** — the treble tilt is merged on top of the user's curve instead of overwriting the top two bands.
- **Real audio session id** — published by the playback service through `PlaybackSessionInfo`, so the equaliser and visualiser attach to the actual session.
- **Widgets show artwork** — the three widget sizes use the synchronous `WidgetArtwork` cache.
- **Settings that did nothing now do something** — gapless, loudness normalization ("ReplayGain"), respect-other-apps audio focus, manual loudness gain, and the three notification switches are all applied by the service; the settings diff in `MainViewModel` no longer drops audio, Bluetooth and Drive Mode values.
- **Your data survives updates** — Room no longer destroys the database on every schema change; only the pre-3.0 development schemas may be rebuilt.
- **Clearing the queue sticks** — an empty queue is persisted instead of being resurrected on the next launch.
- **Microphone permission asked at the right time** — RECORD_AUDIO is requested only when a visualiser mode is switched on.
- **Release signing** — drop a `keystore.properties` next to the root build file and release builds are signed with your key (debug key otherwise).

## What's new in 2.0 — "Theme Studio" release

### Seven complete design systems (runtime-switchable, no restart)
| Theme | Identity |
|---|---|
| **Aurora** *(default)* | Vivid blue accent — pure white light mode, true black dark mode, floating glass chrome |
| **Luxury Gold** | Black marble + champagne hairlines, serif display, unhurried motion |
| **Scandinavian** | Paper & sage, flat outlined surfaces, airy typography |
| **Dynamic** | Material You — follows the device wallpaper (Android 12+), artwork fallback |
| **Soft UI** | Neumorphic extruded surfaces with dual-source shadows |
| **AMOLED** | Pure #000, hairline dividers, instant motion |
| **Experimental** | Deep-space glassmorphism, violet→cyan gradients, animated aurora backdrop |

Each theme carries its own colors, corner language, panel/nav-bar/backdrop construction,
display typography and motion personality (`ui/theme/ThemeSpecs.kt` + `AuralisStyle` knobs).
Switch instantly from **Settings → Theme Studio** with live preview cards; key colors
cross-fade over ~420 ms.

### Performance overhaul
- **Killed the 4 Hz whole-app recomposition**: play position now lives in its own
  `PlayerConnection.position` flow; only the seek bar / mini progress hairline repaint
  while music plays. Everything else is perfectly still.
- `Modifier.appear()` rebuilt as a zero-cost composable factory (no `composed {}`
  allocation per row) — fast flings carry no animation cost.
- Coil global crossfade off; per-request 90 ms crossfades, capped decode sizes,
  stable cache keys.
- Navigation transitions replaced: fast shared-axis (~260 ms emphasized) instead of the
  700 ms default cross-fade. Library tabs slide directionally with cascading row entrances.

### Fixes
- **Accent/theme not applying until restart**: artwork-derived color no longer overrides
  the picked accent; it only feeds the Dynamic theme + the Now Playing stage.
- Persian localization actually wired up (Settings → Language / زبان) with full RTL.
- Volume boost setting stored as float multiplier; stats screen aligned to the data model;
  dozens of component-contract compile errors resolved.

## Build

```bash
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleRelease
# → app/build/outputs/apk/release/app-release.apk (debug-signed, installable)
```

Requirements: JDK 17, Android SDK 34.
