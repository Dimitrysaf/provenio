# Provenio for iOS

**The iOS app is not maintained.** Provenio is developed and released for Android, Windows and Linux. The iOS code is kept in the repository so anyone who wants to bring it back has a starting point, but it is not built in CI, not tested and not released, and it may not compile against the current shared code.

If you want to work on it, contributions are welcome. Open an issue first so we can talk about what you plan to do.

## What is here

- `iosApp/` is the Xcode project: the SwiftUI shell, the tab bar, app icons, Info.plist and the downloads widget extension (`DownloadsWidgetExtension/`).
- The app itself is the shared Kotlin Multiplatform code in `composeApp/`, compiled into a framework the Xcode project embeds.
  - `composeApp/src/iosMain/` holds the iOS implementations of the platform APIs.
  - `composeApp/src/iosAppStore/` and `composeApp/src/iosFull/` hold the parts that differ between the two iOS builds.
- `Configuration/Version.xcconfig` holds the app version, which the Android and desktop builds also read.

## The two builds

The `PROVENIO_IOS_DISTRIBUTION` environment variable (or the same key in `local.properties`) picks one:

- `appstore` (the default) leaves out the torrent engine and the plugin runtime.
- `full` adds them. It links the torrent engine from `engine/`, so build its Apple XCFramework first, on macOS:

  ```bash
  engine/scripts/build-apple-xcframework.sh
  ```

  This writes `engine/platform/apple/Engine.xcframework`, which `composeApp/build.gradle.kts` checks for before compiling.

## Building

You need macOS, Xcode and JDK 17.

The player uses [MPVKit](https://github.com/mpvkit/MPVKit), which the Xcode project expects as a local Swift package at `MPVKit/` in the repository root. It is not included; clone it there first. Upstream Nuvio used its own fork, [NuvioMedia/MPVKit](https://github.com/NuvioMedia/MPVKit).

```bash
env PROVENIO_IOS_DISTRIBUTION=full xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath build/ios-derived-full-simulator \
  CODE_SIGNING_ALLOWED=NO \
  build
```

To run on a device, set `TEAM_ID` in `Configuration/Config.xcconfig` to your Apple developer team; the Xcode project signs with it.

## Known gaps

- Nothing on iOS has been checked since the app moved to the Android, Windows and Linux releases, so expect build errors and features that behave differently from the other platforms.
- The torrent engine's iOS build does not report per-torrent details yet.
