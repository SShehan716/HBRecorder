# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Misty Screen Recorder (lite) is a native Android screen recorder app. It's a two-module Gradle project:

- **`app`** (`com.ss.Misty_Screen_Recoder_lite`) — the actual application: UI, recordings library, video trimming, an encrypted "vault" for hiding videos, and a floating recording-control dock.
- **`hbrecorder`** (`com.hbisoft.hbrecorder`) — a vendored/forked copy of the HBRecorder screen-recording library (originally from `HBiSoft/HBRecorder`, distributed via JitPack). It's included as a local module (`project(path: ':hbrecorder')`), not a remote dependency, so changes to the recording engine itself happen here.

Note: this repo currently ships only the default `ExampleUnitTest`/`ExampleInstrumentedTest` placeholders in both modules — there is no real unit test suite yet.

This project has a knowledge graph at graphify-out/ with god nodes,
community structure, and cross-file relationships.

Rules:

- For codebase questions, first run `graphify query "<question>"`
  when graphify-out/graph.json exists.
- Use `graphify path "<A>" "<B>"` for relationships and
  `graphify explain "<concept>"` for focused concepts.
- After modifying code, run `graphify update .` to keep the graph current.

## Architecture

### Recording pipeline

- `MainActivity` (`app/`) owns the single `HBRecorder` instance and implements `HBRecorderListener`. It drives the full recording lifecycle: requesting permissions (`RECORD_AUDIO`, notifications, `SYSTEM_ALERT_WINDOW` for the overlay), building a `MediaProjectionManager` capture intent, configuring quality/custom settings, starting/stopping/pausing recording, and reacting to `HBRecorderOnStart/OnComplete/OnError/OnPause/OnResume` callbacks.
- Two fragments feed settings into `MainActivity` via a `ViewPager2`/`TabLayout`: `QuickSettingsFragment` (HD toggle, audio on/off) and `AdvancedSettingsFragment` (encoder, resolution, framerate, bitrate, output format, audio source dropdowns, max file size). Settings persist to `SharedPreferences` (`ScreenRecorderSettings`).
- Actual capture happens inside `hbrecorder`'s `ScreenRecordService` (a foreground service, `foregroundServiceType="mediaProjection|microphone"`), which uses `MediaProjection` + `MediaRecorder`/`MediaCodec`, muxes output via `AvMuxer`, and — on Android 10+ — captures system/internal audio through `InternalAudioCapture` (`AudioPlaybackCaptureConfiguration` / `REMOTE_SUBMIX`). See `AUDIO_SOURCE_GUIDE.md` for the audio-source semantics: "Microphone" = `MIC`, "Internal Audio" = `REMOTE_SUBMIX` (Android 10+ only, capture can fail per-app/device — code must fall back gracefully), "Voice Call" = `VOICE_CALL`, "Default" = system default.
- `Constants` (hbrecorder) defines error codes such as `MAX_FILE_SIZE_REACHED_ERROR` and `SETTINGS_ERROR` that `MainActivity.HBRecorderOnError` handles with dedicated recovery paths (`handleMaxFileSizeError`, `handleSettingsError`, `applyFallbackSettings`).

### Floating dock

`FloatingDockService` draws a draggable overlay (via `SYSTEM_ALERT_WINDOW` / `WindowManager`) with pause/resume/stop controls while recording. It does **not** own the recorder — it broadcasts `ACTION_DOCK_STOP` / `ACTION_DOCK_PAUSE` / `ACTION_DOCK_RESUME` intents that `MainActivity`'s `dockCommandReceiver` picks up, since `MainActivity` is the sole owner of the `HBRecorder` instance. When touching dock behavior, changes usually need to happen in both `FloatingDockService` (UI/gesture) and `MainActivity` (`registerDockCommandReceiver`, the actual recorder action).

### Recordings library & playback

`RecordingsActivity` + `RecordingsAdapter` + `RecordingItem` list saved recordings (queried from `MediaStore`/gallery). `VideoPlayerActivity` plays a recording back using Media3 ExoPlayer. `TrimActivity` + `TrimUtils` handle in-app trimming of a recording.

### Vault (hidden/encrypted storage)

- `VaultManager` is a singleton that moves videos out of the public gallery into app-private storage (`vault/` dir + a `vault_index.json` metadata index tracking `originalName`, `encrypted`, `dateAdded`, `sizeBytes` per item), and can restore them back to the gallery. All file I/O runs on a single-thread `ExecutorService`, with callbacks posted back to the main thread via a `Handler`.
- `VaultCrypto` optionally encrypts vault files with AES-256-GCM using a non-exportable key from the Android Keystore. Files are encrypted in independent 4MB chunks (format: `MAGIC("MSTYENC1")` + repeated `[ivLen][iv][ciphertextLen][ciphertext+tag]` chunks) so multi-gigabyte videos never need to fit in memory. `VaultCrypto.isEncryptedFile()` sniffs the magic bytes to detect encrypted files.
- `PinManager` gates vault access with a PIN, stored only as a salted PBKDF2-HmacSHA1 hash (never the raw PIN) in `SharedPreferences`, verified with constant-time comparison.
- `VaultActivity` is the UI tying these three together.

### Logging

Use `LogUtils` (not `android.util.Log` directly) for all app-module logging — it's a thin wrapper gated by `BuildConfig.DEBUG` so debug logs are compiled out of release builds. `buildFeatures { buildConfig true }` in `app/build.gradle` exists specifically to support this.

### Signing / release

`app/build.gradle` release build type has `minifyEnabled true` + `shrinkResources true` with ProGuard rules in `app/proguard-rules.pro`; that file currently blanket-keeps both `com.ss.Misty_Screen_Recoder_lite.**` and `com.hbisoft.hbrecorder.**`, so R8 shrinking of app/library code is effectively disabled — only third-party deps get shrunk.
