# Canvas (droid fork)

This is a droid app: a long-lived personal fork of an open-source Android app, released as signed
APKs from GitHub. Use the `/droid` skill for branch, sync, signing and release work. It stays a
fork: upstream is synced regularly, so keep the fork's footprint in upstream files minimal.

- **Upstream:** https://github.com/sameerasw/canvas (default branch `main`, kept as a clean mirror)
- **Fork:** https://github.com/Staninna/Canvas, all work on `my-main` (every push to it releases)
- **Install mode:** replace (keeps upstream's application ID `com.sameerasw.canvas`; uninstall the
  original first)
- **Release build:** `:app:assembleRelease` → `app/build/outputs/apk/release/app-release-unsigned.apk`,
  signed in CI by `.github/workflows/release.yml`

## Build

    ./gradlew assembleDebug        # debug APK
    ./gradlew testDebugUnitTest    # unit tests
    ./gradlew assembleRelease      # minified (R8) release, run before pushing to my-main

## How the fork is laid out (sync-friendly)

The redesigned UI lives entirely in **`com.sameerasw.canvas.fork`**. Upstream files are left
byte-for-byte unchanged, except for these hooks:

| Upstream file | Fork change |
|---|---|
| `MainActivity.kt` | `setContent { ... }` calls `fork.ForkCanvasRoot(...)` instead of upstream's `CanvasApp` |
| `AndroidManifest.xml` | registers `.fork.settings.ForkSettingsActivity` |
| `.gitignore` | un-ignores `gradle/wrapper/gradle-wrapper.jar` (needed by CI) |

Rules for fork work:
- New fork code goes under `fork/`, new fork strings in `res/values/strings_fork.xml`, new icons as
  new drawable files. Don't edit upstream files unless there's no other way, and if you must,
  keep it to a small, commented (`// fork:`) hunk and add it to the table above.
- Use only the public API of upstream classes (`CanvasViewModel`, `SettingsRepository`, models,
  `StrokeDrawer`, `BackgroundDrawer`, `TextDrawer`, export helpers). Don't change their signatures.
- Don't delete upstream files that the fork no longer uses: leaving them avoids modify/delete
  conflicts. They are dead code in the fork (R8 strips them from the release).
- Fork-only settings go in `ForkSettings` (own prefs file `canvas_fork_prefs`). Upstream settings
  are read from upstream's `canvas_prefs` through a change listener and written through
  `SettingsRepository`.

### Fork packages

- `fork/ForkCanvasRoot.kt`: entry point from MainActivity (theme + `CanvasApp`); owns the
  background picker.
- `fork/BackgroundStore.kt`: copies picked backgrounds into `filesDir/backgrounds/` so backups
  carry them, migrates old `content://` backgrounds, removes unreferenced files once per process.
- `fork/ForkSettings.kt`: settings as `StateFlow`s.
- `fork/ui/CanvasApp.kt`: main screen; tool state, dialogs, wiring to the ViewModel.
- `fork/ui/CanvasSurface.kt`: drawing surface. One gesture detector: one finger/stylus uses the
  tool, two fingers pinch-zoom/pan, two/three-finger taps undo/redo, palm ignored while a stylus
  draws, eraser swipe = one undo step.
- `fork/ui/CanvasViewportState.kt`: zoom/pan camera (screen = world * scale + offset).
- `fork/ui/chrome/`: `ToolDock`, `CanvasTopBar`, `ToolOptionsPanel` (style, size, palette).
- `fork/ui/dialogs/`, `fork/ui/theme/` (`ForkTheme`, Material 3 Expressive), `fork/settings/`.

## Sync checklist (after merging upstream into my-main)

Upstream changes to the files the fork replaced merge cleanly but **do not show up in the app**,
because the fork uses its own versions. After every sync, review upstream's diff for these files
and port anything relevant to the fork equivalent:

| Upstream file(s) | Fork equivalent |
|---|---|
| `MainActivity.kt` (`CanvasApp` composable, menu actions, share flow) | `fork/ui/CanvasApp.kt` |
| `ui/screens/DrawingCanvasScreen.kt` (gestures, rendering, new tools) | `fork/ui/CanvasSurface.kt` |
| `ui/components/bottomtoolbar/`, `toptoolbar/`, `panels/`, `ColorPicker.kt` | `fork/ui/chrome/` |
| `ui/components/dialogs/` | `fork/ui/dialogs/` |
| `SettingsActivity.kt` (new settings) | `fork/settings/ForkSettingsActivity.kt` (+ `ForkSettings` if the key is new) |
| `ui/theme/` | `fork/ui/theme/` |
| `model/ToolType.kt` (new tool or shape) | `DockTools`, `toolIcon`, `toolLabel` in `ToolDock.kt`, a section in `ToolOptionsPanel.kt`, handling in `CanvasSurface.kt` |
| `SettingsRepository.kt` (new/renamed keys) | key constants in `ForkSettings.kt` must match |
| `CanvasViewModel.kt` (public API changes) | callers in `CanvasApp.kt` (eraser uses `setStrokes`/`importCanvasState`, text move uses `updateText`) |

Then build debug and release and run the unit tests before pushing `my-main`.
