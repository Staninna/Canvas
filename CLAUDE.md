# Canvas (droid fork)

This is a droid app: a personal fork of an open-source Android app, released as signed APKs from
GitHub. Use the `/droid` skill for branch, sync, signing and release work.

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

## UI architecture (fork)

- `ui/CanvasApp.kt`: the main screen; holds tool state and wires the canvas to the chrome.
- `ui/screens/DrawingCanvasScreen.kt`: the drawing surface. One gesture detector handles every
  touch: one finger or stylus uses the tool, two fingers pinch-zoom/pan, two/three-finger taps
  undo/redo, a resting palm is ignored while the stylus draws.
- `ui/state/CanvasViewportState.kt`: zoom/pan camera (screen = world * scale + offset).
- `ui/components/chrome/`: `ToolDock`, `CanvasTopBar`, `ToolOptionsPanel` (style, size, palette).
- `SettingsRepository`: every setting is a `StateFlow`; collect it, never poll.
- New user-visible text goes in `res/values/strings.xml`.
