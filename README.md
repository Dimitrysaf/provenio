<div align="center">

  <h1>Provenio</h1>

  <p>
    A free, open-source media app for your phone, your desktop, and the TV you already own.
    <br />
    Bring your own sources. Provenio turns them into a library with artwork, ratings, subtitles, and your place saved on every screen.
  </p>

  [GitHub releases](https://github.com/Dimitrysaf/provenio/releases/latest)

</div>

## Get Provenio

- [Android APK, Windows MSI and Linux Flatpak](https://github.com/Dimitrysaf/provenio/releases/latest)

## Build from source

```bash
git clone --recurse-submodules https://github.com/Dimitrysaf/provenio.git
cd provenio
```

### Android

Android development requires Android Studio and the Android SDK.

```bash
./gradlew :androidApp:assembleFullDebug
```

### iOS

iOS development requires macOS and Xcode.

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

The shared app is built with Kotlin Multiplatform and Compose Multiplatform.

## Privacy

Provenio has no accounts and no servers of its own. Your library, progress and settings stay on your devices, and Local sync moves them directly between your devices on your network. The app only talks to the services you connect, such as TMDB, Trakt, Simkl, debrid providers and your addons, under their own privacy policies.

## License

[GNU General Public License v3.0](./LICENSE)

Provenio is based on [Nuvio Mobile](https://github.com/NuvioMedia/NuvioMobile), also licensed under the GPL-3.0.
