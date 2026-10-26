# Floatify

Android 10+ app that opens mini apps (file manager, calculator, browser, games, ...) in floating windows over other apps.

**Status:** two mini apps are real now — **Calculator** and **File Manager**. The rest are placeholder windows, and the settings screen still holds placeholder options.

## Built-in mini apps

### 🧮 Calculator
A working calculator with `+ - × ÷ %`, unary minus, live result preview and a `C` / `⌫` / `=` keypad. Backed by a small recursive-descent evaluator (`Calc` in `CalculatorView.kt`).

### 📁 File Manager
A floating file browser:

- **Browse** folders with tap-to-open, plus `🏠` (home), `↑` (up), `⟳` (refresh) and `↕` (cycle sort by name / size / date).
- **Open** a file with any app on the device (images, video, audio, PDF, text, APK) via a `FileProvider` URI.
- **Long-press** a file or folder for an inline panel: copy path, share, delete (with confirmation).
- Footer shows item count, total size and the active sort order.
- If Android's scoped storage blocks a folder, the panel explains why and can jump straight to the **All files access** settings screen.

Dialogs are drawn as an in-window panel, because a foreground service cannot show a real `AlertDialog` window.

> Granting "All files access" (`MANAGE_EXTERNAL_STORAGE`) is optional but needed to browse shared storage on Android 11+. Without it, app-private storage still works.

## Build
Push to GitHub: the `Android CI` workflow builds a debug APK and uploads it as an artifact.
Locally: open in Android Studio, or run `gradle assembleDebug` (JDK 17).
