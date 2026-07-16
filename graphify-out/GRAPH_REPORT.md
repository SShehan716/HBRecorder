# Graph Report - Misty-Screen-Recoder(lite)  (2026-07-16)

## Corpus Check
- 49 files · ~32,913 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 680 nodes · 1464 edges · 49 communities (37 shown, 12 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 158 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `54ddb47f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainActivity Core Recording Flow
- Trim Utilities & Video Editing
- File Naming & Output Watching
- Recordings Library UI
- Recording Settings & Format Config
- App Init & PIN Security
- Vault Encryption & Storage
- Audio Source Guide & Project Docs
- Advanced Settings Fragment
- Trim Activity Playback
- Quick Settings Fragment
- App Launcher Icons (hdpi/xhdpi/xxhdpi)
- Settings Activity & Preferences
- Floating Dock Service
- Recording Countdown Timer
- ViewPager Adapter
- Adaptive Icon Foreground Branding
- Dock Command Notification Receiver
- Log Utilities
- Instrumented Test
- Instrumented Test (duplicate)
- Unit Test
- Vault Subsystem (docs hyperedge)
- GitHub Issue Triage Workflow
- Gradle Wrapper Script
- Unit Test (duplicate)
- Round Launcher Icon (xxhdpi)
- Manifest & Launcher Icon (mdpi)
- Recordings Pipeline (docs hyperedge)
- Play Store Icon & Brand Identity
- Foreground Icon Recording Dot
- Trim Pipeline (docs hyperedge)
- HBRecorder Error Constants
- VideoPlayerActivity (docs)
- .onCreate
- Architecture
- Implementation Plan - Resolve Advertising ID Declaration Conflict
- Walkthrough - Privacy Policy Alignment
- HBRecorderListener
- .startService
- CLAUDE.md

## God Nodes (most connected - your core abstractions)
1. `MainActivity` - 96 edges
2. `HBRecorder` - 54 edges
3. `HBRecorderCodecInfo` - 39 edges
4. `ScreenRecordService` - 28 edges
5. `AdvancedSettingsFragment` - 27 edges
6. `RecordingItem` - 24 edges
7. `RecordingsActivity` - 24 edges
8. `VaultActivity` - 22 edges
9. `VaultManager` - 21 edges
10. `RecordingsAdapter` - 19 edges

## Surprising Connections (you probably didn't know these)
- `HBRecorder Custom Settings API` --conceptually_related_to--> `Audio Source Configuration Guide`  [INFERRED]
  README.md → AUDIO_SOURCE_GUIDE.md
- `AndroidManifest.xml (app module)` --references_as_roundIcon--> `ic_launcher_round.png (mdpi)`  [AMBIGUOUS]
  app/src/main/AndroidManifest.xml → app/src/main/res/mipmap-mdpi/ic_launcher_round.png
- `AndroidManifest.xml (app module)` --declares_round_launcher_icon--> `ic_launcher_round.png (xxxhdpi)`  [INFERRED]
  app/src/main/AndroidManifest.xml → app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
- `AdvancedSettingsFragment` --references--> `HBRecorder`  [EXTRACTED]
  app/src/main/java/com/ss/Misty_Screen_Recoder_lite/AdvancedSettingsFragment.java → hbrecorder/src/main/java/com/hbisoft/hbrecorder/HBRecorder.java
- `MainActivity` --references--> `HBRecorder`  [EXTRACTED]
  app/src/main/java/com/ss/Misty_Screen_Recoder_lite/MainActivity.java → hbrecorder/src/main/java/com/hbisoft/hbrecorder/HBRecorder.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **GitHub Issue Triage Workflow** — github_issue_template_bug_report, github_issue_template_feature_request, github_issue_template_question, github_stale [INFERRED 0.85]

## Communities (49 total, 12 thin omitted)

### Community 0 - "MainActivity Core Recording Flow"
Cohesion: 0.14
Nodes (12): AutoCompleteTextView, EditText, RequiresApi, Uri, MainActivity, Button, ConstraintLayout, SuppressWarnings (+4 more)

### Community 1 - "Trim Utilities & Video Editing"
Cohesion: 0.06
Nodes (30): Context, Uri, TrimUtils, AudioRecord, BufferInfo, ContentResolver, FileDescriptor, AvMuxer (+22 more)

### Community 2 - "File Naming & Output Watching"
Cohesion: 0.12
Nodes (4): HBRecorder, Context, RequiresApi, Uri

### Community 3 - "Recordings Library UI"
Cohesion: 0.06
Nodes (32): Adapter, AlertDialog, Uri, RecordingItem, Bundle, Intent, Menu, MenuItem (+24 more)

### Community 4 - "Recording Settings & Format Config"
Cohesion: 0.11
Nodes (5): HBRecorderCodecInfo, Context, RequiresApi, RecordingInfo, MediaCodecInfo

### Community 5 - "App Init & PIN Security"
Cohesion: 0.19
Nodes (6): Override, MyApplication, Context, PinManager, Application, SharedPreferences

### Community 6 - "Vault Encryption & Storage"
Cohesion: 0.11
Nodes (10): VaultCrypto, Callback, FileCallback, Context, Handler, Uri, VaultItem, VaultManager (+2 more)

### Community 7 - "Audio Source Guide & Project Docs"
Cohesion: 0.18
Nodes (12): Audio Source Configuration Guide, Android Version Detection for Internal Audio, Automatic Fallback on Audio Source Failure, Default Audio Source (DEFAULT), Internal Audio Source (REMOTE_SUBMIX), Microphone Audio Source (MIC), Voice Call Audio Source (VOICE_CALL), JitPack Build Configuration (+4 more)

### Community 8 - "Advanced Settings Fragment"
Cohesion: 0.11
Nodes (10): AdvancedSettingsFragment, AutoCompleteTextView, Bundle, CheckBox, LayoutInflater, Nullable, Override, View (+2 more)

### Community 9 - "Trim Activity Playback"
Cohesion: 0.14
Nodes (15): Bundle, ExoPlayer, Override, TextView, Uri, TrimActivity, Bundle, ExoPlayer (+7 more)

### Community 10 - "Quick Settings Fragment"
Cohesion: 0.15
Nodes (12): Bundle, CheckBox, LayoutInflater, Nullable, Override, View, ViewGroup, OnSettingsChangedListener (+4 more)

### Community 11 - "App Launcher Icons (hdpi/xhdpi/xxhdpi)"
Cohesion: 0.11
Nodes (22): Misty Screen Recorder (lite) Android app, AndroidManifest.xml (app module), icon.png (App Icon), App Launcher Icon (hdpi), ic_launcher_round.png (mipmap-hdpi): round Android launcher icon, dark indigo/purple filled circle background with a centered white ring/circle outline containing a solid red dot, evoking a camcorder/record-button glyph representing the screen recorder app, ic_launcher_round.png (mdpi), ic_launcher.png (xhdpi launcher icon), ic_launcher_foreground.png (xhdpi) (+14 more)

### Community 12 - "Settings Activity & Preferences"
Cohesion: 0.19
Nodes (11): Bundle, MenuItem, Override, MainPreferenceFragment, SettingsActivity, ListPreference, OnPreferenceChangeListener, OnPreferenceClickListener (+3 more)

### Community 13 - "Floating Dock Service"
Cohesion: 0.25
Nodes (10): FloatingDockService, Handler, IBinder, Intent, Nullable, Override, View, LayoutParams (+2 more)

### Community 14 - "Recording Countdown Timer"
Cohesion: 0.22
Nodes (3): Countdown, Timer, TimerTask

### Community 15 - "ViewPager Adapter"
Cohesion: 0.27
Nodes (6): Fragment, NonNull, Override, ViewPagerAdapter, FragmentActivity, FragmentStateAdapter

### Community 16 - "Adaptive Icon Foreground Branding"
Cohesion: 0.25
Nodes (9): Misty Screen Recorder (lite) app (com.ss.Misty_Screen_Recoder_lite), ic_launcher_foreground.png (mipmap-hdpi), ic_launcher_foreground.png (mdpi), ic_launcher_foreground.png (xxhdpi) - adaptive icon foreground layer, Android Adaptive Icon (foreground/background layer system), Camcorder/video-camera icon motif (white body, red record circle, triangular lens flap), Japan flag (Nisshoki) visual motif — white field with centered red disc, Misty Screen Recorder (lite) app branding/identity (+1 more)

### Community 17 - "Dock Command Notification Receiver"
Cohesion: 0.43
Nodes (5): BroadcastReceiver, Context, Intent, Override, NotificationReceiver

### Community 19 - "Instrumented Test"
Cohesion: 0.60
Nodes (3): ExampleInstrumentedTest, RunWith, Test

### Community 20 - "Instrumented Test (duplicate)"
Cohesion: 0.60
Nodes (3): ExampleInstrumentedTest, RunWith, Test

### Community 22 - "Vault Subsystem (docs hyperedge)"
Cohesion: 0.18
Nodes (5): FileObserver, Override, SingleFileObserver, Override, MyListener

### Community 23 - "GitHub Issue Triage Workflow"
Cohesion: 0.50
Nodes (4): Bug Report Issue Template, Feature Request Issue Template, Question Issue Template, Stale Bot Configuration (probot-stale)

### Community 24 - "Gradle Wrapper Script"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 26 - "Round Launcher Icon (xxhdpi)"
Cohesion: 0.67
Nodes (3): Round adaptive launcher icon (concept), ic_launcher_round.png (xxhdpi), Misty Screen Recorder (lite) app

### Community 27 - "Manifest & Launcher Icon (mdpi)"
Cohesion: 0.67
Nodes (3): AndroidManifest.xml (app module), ic_launcher.png (mdpi launcher icon), Video camera launcher icon branding concept (red circular camera glyph on dark purple rounded-square background)

### Community 34 - "VideoPlayerActivity (docs)"
Cohesion: 0.17
Nodes (3): Menu, MenuItem, Override

### Community 37 - "Architecture"
Cohesion: 0.20
Nodes (8): Architecture, Floating dock, Logging, Project overview, Recording pipeline, Recordings library & playback, Signing / release, Vault (hidden/encrypted storage)

### Community 38 - "Implementation Plan - Resolve Advertising ID Declaration Conflict"
Cohesion: 0.22
Nodes (8): Documentation & Compliance, Implementation Plan - Resolve Advertising ID Declaration Conflict, Manual Verification, [MODIFY] [privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html), [MODIFY] [strings.xml](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/app/src/main/res/values/strings.xml), Proposed Changes, User Review Required, Verification Plan

### Community 41 - "Walkthrough - Privacy Policy Alignment"
Cohesion: 0.33
Nodes (5): Changes Made, Documentation, Next Steps for You, [privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html), Walkthrough - Privacy Policy Alignment

## Ambiguous Edges - Review These
- `ic_launcher_round.png (mipmap-hdpi): round Android launcher icon, dark indigo/purple filled circle background with a centered white ring/circle outline containing a solid red dot, evoking a camcorder/record-button glyph representing the screen recorder app` → `ic_launcher_round.png (xxxhdpi)`  [AMBIGUOUS]
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png · relation: same_asset_different_density_as
- `ic_launcher_round.png (mdpi)` → `AndroidManifest.xml (app module)`  [AMBIGUOUS]
  app/src/main/res/mipmap-mdpi/ic_launcher_round.png · relation: references_as_roundIcon
- `ic_launcher_round.png (xhdpi round launcher icon)` → `ic_launcher_round.png (xxxhdpi)`  [AMBIGUOUS]
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png · relation: same_asset_different_density_as

## Knowledge Gaps
- **41 isolated node(s):** `User Review Required`, `[MODIFY] [privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html)`, `[MODIFY] [strings.xml](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/app/src/main/res/values/strings.xml)`, `Manual Verification`, `[privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html)` (+36 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `ic_launcher_round.png (mipmap-hdpi): round Android launcher icon, dark indigo/purple filled circle background with a centered white ring/circle outline containing a solid red dot, evoking a camcorder/record-button glyph representing the screen recorder app` and `ic_launcher_round.png (xxxhdpi)`?**
  _Edge tagged AMBIGUOUS (relation: same_asset_different_density_as) - confidence is low._
- **What is the exact relationship between `ic_launcher_round.png (mdpi)` and `AndroidManifest.xml (app module)`?**
  _Edge tagged AMBIGUOUS (relation: references_as_roundIcon) - confidence is low._
- **What is the exact relationship between `ic_launcher_round.png (xhdpi round launcher icon)` and `ic_launcher_round.png (xxxhdpi)`?**
  _Edge tagged AMBIGUOUS (relation: same_asset_different_density_as) - confidence is low._
- **Why does `MainActivity` connect `MainActivity Core Recording Flow` to `Trim Utilities & Video Editing`, `VideoPlayerActivity (docs)`, `Recordings Library UI`, `.onCreate`, `File Naming & Output Watching`, `Vault Encryption & Storage`, `.setOnClickListeners`, `Advanced Settings Fragment`, `.setupDropdowns`, `Quick Settings Fragment`, `.showOverlayPermissionDialog`, `.setOutputPath`, `Trim Activity Playback`, `HBRecorderListener`, `Log Utilities`, `Recordings Pipeline (docs hyperedge)`, `Trim Pipeline (docs hyperedge)`?**
  _High betweenness centrality (0.213) - this node is a cross-community bridge._
- **Why does `HBRecorder` connect `File Naming & Output Watching` to `MainActivity Core Recording Flow`, `.onCreate`, `Recording Settings & Format Config`, `Advanced Settings Fragment`, `HBRecorderListener`, `.setOutputPath`, `.startService`, `Recording Countdown Timer`, `.getFilePath`, `Vault Subsystem (docs hyperedge)`, `Recordings Pipeline (docs hyperedge)`?**
  _High betweenness centrality (0.135) - this node is a cross-community bridge._
- **Why does `HBRecorderCodecInfo` connect `Recording Settings & Format Config` to `MainActivity Core Recording Flow`, `Trim Utilities & Video Editing`, `.onCreate`, `Advanced Settings Fragment`, `.setupDropdowns`, `Trim Pipeline (docs hyperedge)`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HBRecorderCodecInfo` (e.g. with `.getDefaultHeight()` and `.getDefaultWidth()`) actually correct?**
  _`HBRecorderCodecInfo` has 3 INFERRED edges - model-reasoned connections that need verification._