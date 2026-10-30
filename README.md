# BlockPuzzle2026

Cross-platform block puzzle for Android and iOS, built with Kotlin Multiplatform
and Compose Multiplatform. F0 supplies the project foundation, F1 the deterministic
engine, F2 the playable shared UI, and F3 local saves/records/statistics.

## Modules

| Module | Responsibility |
| --- | --- |
| `engine` | Deterministic pure Kotlin engine, JVM/iOS targets, no UI dependency |
| `composeApp` | Shared Compose UI, Android application and static iOS framework |
| `iosApp` | SwiftUI host, checked-in Xcode project and iOS launch test |

Versions live in `gradle/libs.versions.toml`: Kotlin 2.2.20, Compose 1.9.1,
AGP 8.11.1. The Gradle 8.14.3 wrapper verifies its distribution SHA-256.

## Android development

Requires a full JDK 21 and Android SDK. Install Android Studio or Android command
line tools, accept SDK licenses, and install:

```sh
sdkmanager 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
```

Set `JAVA_HOME` to the JDK and `ANDROID_HOME` to the SDK, or set `sdk.dir` in an
untracked `local.properties`. Minimum Android version: API 24; target API: 36.

```sh
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:testDebugUnitTest :composeApp:lintDebug ktlintCheck
```

The JVM-hosted Robolectric tests exercise setup, raw tapping, drag/drop, zoom/pan,
large text, lifecycle pause, configuration recreation, game-over/replay and local
progress restoration without
an emulator. Native-graphics renders are exported under
`composeApp/build/reports/ui-snapshots/`. The APK is in `composeApp/build/outputs/apk/debug/`.
Install on an emulator/device with `adb install -r <apk-path>`.

The engine can be configured independently of the Android SDK:

```sh
./gradlew --configure-on-demand :engine:jvmTest
```

This runs the F1 common engine tests on JVM. See [engine/README.md](engine/README.md)
for configurable boards, difficulty/tempo rules, scoring and the engine API.

## iOS development

Requires macOS, Xcode 16+, its command line tools and an installed iOS simulator,
plus JDK 21. Open `iosApp/iosApp.xcodeproj`, select the shared `iosApp` scheme and
a simulator, then Run. Xcode's first build phase runs
`:composeApp:embedAndSignAppleFrameworkForXcode`; CocoaPods is not required.
The deployment target is iOS 15.0. Device builds require your signing team;
simulator builds require no Apple credentials.

```sh
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 16' \
  -derivedDataPath iosApp/DerivedData CODE_SIGNING_ALLOWED=NO
```

Choose an installed device from `xcrun simctl list devices available` instead of
assuming the example simulator exists. The iOS UI tests launch the app, check its title, and exercise setup/start and
pause/resume and save/relaunch recovery. Linux cannot compile or validate Apple
frameworks/Xcode apps.

## CI and quality

GitHub Actions runs on pull requests, pushes to `main`, and manual dispatch:

- Linux: wrapper validation (Gradle setup action), ktlint, engine JVM test task,
  Android UI/controller/geometry tests, Android lint (warnings fail), debug APK and reports.
- macOS: common engine tests on an available iOS simulator, unsigned simulator
  Debug app build and XCTest launch/play tests; reports and result bundle.

Use `./gradlew ktlintFormat` to apply Kotlin formatting. CI runs `ktlintCheck`
and never rewrites sources. No backend, API keys, ad IDs or signing credentials
are needed for F0.

## Game rules and next milestones

Place one of three available pieces on an 8×8 grid. Completed rows and columns
clear simultaneously. The game ends when none of the available pieces can fit.

F1 engine: implemented, including 8×8/16×16 boards, Easy/Medium/Hard piece
distributions and independent Relaxed/Slow/Medium/Fast tempos. Timed modes deduct
10 points per timeout and end on the third timeout.

F2 UI: implemented, including light theme, setup choices, pointer drag preview,
accessible tap placement, enlarged/pannable boards, line feedback, haptic/animation
switches and lifecycle-aware timers. Android ViewModel retains an in-memory game
through configuration recreation.

See [device validation notes](docs/manual/f2-device-checks.md) for controls and
unrun physical-device/iOS checks.

F3 local progress: implemented. Games checkpoint automatically and return paused;
Home offers Continue or a confirmed replacement. Records are separate per mode
and use final completed scores. See [local progress notes](docs/manual/f3-local-progress.md)
for storage, retry/recovery behavior and outstanding real-device checks.

F4: Cloudflare Workers/D1 leaderboard. F5: ads and premium.
F6: device testing, performance and store release preparation.

## Cloud workspace

The prepared cloud tools and caches live outside the checkout under
`/workspace/.tools`. Activate them in a new shell with:

```sh
. /workspace/.tools/activate-f0.sh
cd /workspace/BlockPuzzle2026
```

This activation file is cloud-local and contains tool paths, proxy host/port and
CA trust configuration. No SDK paths, proxy settings or credentials are committed.
Use the existing isolated checkout; another Git worktree is unnecessary.
