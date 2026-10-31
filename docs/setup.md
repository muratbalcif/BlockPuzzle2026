# Local setup

## Android
Install Android Studio with JDK 17 and Android SDK 35. Open the repository root.
Until the Gradle wrapper is committed, install Gradle 8.10.2 and run:
```sh
gradle :game-engine:allTests :composeApp:assembleDebug
```

## iOS
Requires macOS, Xcode, XcodeGen, and a generated Gradle wrapper.
```sh
gradle wrapper --gradle-version 8.10.2
chmod +x gradlew
cd iosApp && xcodegen generate
open BlockMaster.xcodeproj
```
The Xcode pre-build script invokes `embedAndSignAppleFrameworkForXcode`.
The framework integration and iOS build are not yet verified.

## MVP interaction
Select one of the numbered pieces and tap the board to place it. Drag-and-drop is a future milestone.
