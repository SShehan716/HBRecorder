# Graph Report - .  (2026-07-16)

## Corpus Check
- Corpus is ~32,399 words - fits in a single context window. You may not need a graph.

## Summary
- 686 nodes · 1484 edges · 36 communities (28 shown, 8 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 167 edges (avg confidence: 0.81)
- Token cost: 116,928 input · 0 output

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
- `HBRecorderListener interface & callbacks` --semantically_similar_to--> `MainActivity`  [INFERRED] [semantically similar]
  README.md → CLAUDE.md
- `HBRecorder Library README` --semantically_similar_to--> `HBRecorder (recording engine class)`  [INFERRED] [semantically similar]
  README.md → CLAUDE.md
- `HBRecorder Custom Settings API` --conceptually_related_to--> `Audio Source Configuration Guide`  [INFERRED]
  README.md → AUDIO_SOURCE_GUIDE.md
- `MIT License (HBiSoft)` --conceptually_related_to--> `hbrecorder Module Notes (CLAUDE.md)`  [INFERRED]
  License.txt → hbrecorder/CLAUDE.md
- `MIT License (HBiSoft)` --conceptually_related_to--> `HBRecorder Library README`  [INFERRED]
  License.txt → README.md

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **GitHub Issue Triage Workflow** — github_issue_template_bug_report, github_issue_template_feature_request, github_issue_template_question, github_stale [INFERRED 0.85]
- **Vault (Hidden/Encrypted Storage) Subsystem** — claude_md_vaultmanager, claude_md_vaultcrypto, claude_md_pinmanager, claude_md_vaultactivity [EXTRACTED 1.00]
- **Recording Pipeline** — claude_md_mainactivity, claude_md_hbrecorder, claude_md_screenrecordservice, claude_md_quicksettingsfragment, claude_md_advancedsettingsfragment [EXTRACTED 1.00]

## Communities (36 total, 8 thin omitted)

### Community 0 - "MainActivity Core Recording Flow"
Cohesion: 0.06
Nodes (18): AutoCompleteTextView, BroadcastReceiver, Bundle, EditText, Intent, Menu, MenuItem, Override (+10 more)

### Community 1 - "Trim Utilities & Video Editing"
Cohesion: 0.06
Nodes (30): Context, Uri, TrimUtils, AudioRecord, BufferInfo, ContentResolver, ContentValues, FileDescriptor (+22 more)

### Community 2 - "File Naming & Output Watching"
Cohesion: 0.06
Nodes (12): FileObserver, Override, SingleFileObserver, HBRecorder, Context, Intent, Override, RequiresApi (+4 more)

### Community 3 - "Recordings Library UI"
Cohesion: 0.09
Nodes (24): Adapter, AlertDialog, Uri, RecordingItem, Bundle, Intent, Menu, MenuItem (+16 more)

### Community 4 - "Recording Settings & Format Config"
Cohesion: 0.08
Nodes (6): ResolutionPreset, HBRecorderCodecInfo, Context, RequiresApi, RecordingInfo, MediaCodecInfo

### Community 5 - "App Init & PIN Security"
Cohesion: 0.09
Nodes (15): Override, MyApplication, Context, PinManager, Bundle, EditText, Menu, MenuItem (+7 more)

### Community 6 - "Vault Encryption & Storage"
Cohesion: 0.11
Nodes (9): VaultCrypto, Callback, FileCallback, Context, Handler, Uri, VaultItem, VaultManager (+1 more)

### Community 7 - "Audio Source Guide & Project Docs"
Cohesion: 0.08
Nodes (36): Audio Source Configuration Guide, Android Version Detection for Internal Audio, Automatic Fallback on Audio Source Failure, Default Audio Source (DEFAULT), Internal Audio Source (REMOTE_SUBMIX), Microphone Audio Source (MIC), Voice Call Audio Source (VOICE_CALL), Project CLAUDE.md (root) (+28 more)

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
Cohesion: 0.22
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
Cohesion: 0.50
Nodes (4): PinManager, VaultActivity, VaultCrypto, VaultManager

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

### Community 28 - "Recordings Pipeline (docs hyperedge)"
Cohesion: 0.67
Nodes (3): RecordingItem, RecordingsActivity, RecordingsAdapter

## Ambiguous Edges - Review These
- `ic_launcher_round.png (mipmap-hdpi): round Android launcher icon, dark indigo/purple filled circle background with a centered white ring/circle outline containing a solid red dot, evoking a camcorder/record-button glyph representing the screen recorder app` → `ic_launcher_round.png (xxxhdpi)`  [AMBIGUOUS]
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png · relation: same_asset_different_density_as
- `ic_launcher_round.png (mdpi)` → `AndroidManifest.xml (app module)`  [AMBIGUOUS]
  app/src/main/res/mipmap-mdpi/ic_launcher_round.png · relation: references_as_roundIcon
- `ic_launcher_round.png (xhdpi round launcher icon)` → `ic_launcher_round.png (xxxhdpi)`  [AMBIGUOUS]
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png · relation: same_asset_different_density_as

## Knowledge Gaps
- **41 isolated node(s):** `Constants`, `Bug Report Issue Template`, `Feature Request Issue Template`, `Question Issue Template`, `JitPack Build Configuration` (+36 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `ic_launcher_round.png (mipmap-hdpi): round Android launcher icon, dark indigo/purple filled circle background with a centered white ring/circle outline containing a solid red dot, evoking a camcorder/record-button glyph representing the screen recorder app` and `ic_launcher_round.png (xxxhdpi)`?**
  _Edge tagged AMBIGUOUS (relation: same_asset_different_density_as) - confidence is low._
- **What is the exact relationship between `ic_launcher_round.png (mdpi)` and `AndroidManifest.xml (app module)`?**
  _Edge tagged AMBIGUOUS (relation: references_as_roundIcon) - confidence is low._
- **What is the exact relationship between `ic_launcher_round.png (xhdpi round launcher icon)` and `ic_launcher_round.png (xxxhdpi)`?**
  _Edge tagged AMBIGUOUS (relation: same_asset_different_density_as) - confidence is low._
- **Why does `MainActivity` connect `MainActivity Core Recording Flow` to `Trim Utilities & Video Editing`, `File Naming & Output Watching`, `Recordings Library UI`, `Recording Settings & Format Config`, `Advanced Settings Fragment`, `Trim Activity Playback`, `Quick Settings Fragment`, `Log Utilities`?**
  _High betweenness centrality (0.209) - this node is a cross-community bridge._
- **Why does `HBRecorder` connect `File Naming & Output Watching` to `Advanced Settings Fragment`, `MainActivity Core Recording Flow`, `Recording Settings & Format Config`, `Recording Countdown Timer`?**
  _High betweenness centrality (0.133) - this node is a cross-community bridge._
- **Why does `HBRecorderCodecInfo` connect `Recording Settings & Format Config` to `Advanced Settings Fragment`, `MainActivity Core Recording Flow`, `Trim Utilities & Video Editing`?**
  _High betweenness centrality (0.071) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `HBRecorderCodecInfo` (e.g. with `.getDefaultHeight()` and `.getDefaultWidth()`) actually correct?**
  _`HBRecorderCodecInfo` has 3 INFERRED edges - model-reasoned connections that need verification._