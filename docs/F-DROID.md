# Getting Provenio into F-Droid

F-Droid builds every app from source on its own servers and rejects binaries committed to the repository. This file lists what still stands in the way and how to clear each part. It is a plan; the remaining items are not done yet.

## Done

- **No proprietary libraries.** Sentry crash reporting and Google's ML Kit code scanner are gone. Local sync scans pairing codes with its own scanner, built on CameraX and ZXing, both open source.
- **No Play Store build.** There is a single Android build with no product flavours.
- **No self-updates from F-Droid.** The Android updater only appears when Provenio was installed by hand. When F-Droid (or another store or update manager) installed it, the update button and banner stay hidden, because `AndroidAppUpdaterPlatform.isInstalledByHand()` checks the installing package.
- **No MPEG-H decoder.** The Fraunhofer MPEG-H library is under a non-free licence, so `lib-decoder-mpegh-release.aar` was removed. MPEG-H tracks still get a name in the track list, but are not decoded.

## Prebuilt binaries in `composeApp/libs/`

`composeApp/build.gradle.kts` adds every `lib-*.aar` in that folder to the Android app, plus the QuickJS AAR. Each needs a from-source replacement before F-Droid can build the app.

| File | What it is | How to build it from source |
| --- | --- | --- |
| `lib-engine-android-0.1.1.aar` | The torrent engine (libtorrent and OpenSSL, with a JNI bridge) | Already possible: build with `-Pprovenio.engine.fromSource=true`, which includes `engine/platform/android/engine` as the `:engine` project and builds it with the NDK and CMake. F-Droid's recipe would set that property and stop using the AAR. `lib-engine-android.source` records the engine commit the AAR was built from. |
| `lib-exoplayer-release.aar`, `lib-ui-release.aar` | Patched builds of media3's ExoPlayer and UI modules (the Maven media3 version is 1.8.0) | Find what the patches change (compare the classes with stock media3 1.8.0), put them in this repository as patch files, and build those two modules from the media3 source as an F-Droid `srclib`. If the patches turn out to be unneeded, switch to the stock `androidx.media3:media3-exoplayer` and `media3-ui` from Maven and delete the AARs. |
| `lib-decoder-ffmpeg-release.aar` | media3's FFmpeg audio decoder extension, with FFmpeg's native libraries | Build media3's `decoder_ffmpeg` module against FFmpeg from source, as media3's own instructions describe. Many F-Droid apps do this already (for example as an FFmpeg `srclib`). |
| `lib-decoder-av1-release.aar` | media3's AV1 decoder extension (libgav1) | Build media3's `decoder_av1` module with libgav1 from source, the same way. |
| `quickjs-kt-android-1.0.5-nuvio.aar` | quickjs-kt 1.0.5 with upstream Nuvio's patches, used by the plugin runtime | Find Nuvio's changes against `io.github.dokar3:quickjs-kt:1.0.5`, keep them as a patch, and build quickjs-kt from source as a `srclib`. If they are not needed, use the Maven artifact `libs.quickjs.kt`, which is already declared in `gradle/libs.versions.toml`. |

## Maven dependencies with native code

These come from Maven Central and contain prebuilt native libraries. F-Droid's scanner allows FOSS Maven dependencies, but check them during review:

- `io.github.abdallahmehiz:mpv-android-lib` (libmpv and its FFmpeg)
- `io.github.peerless2012:ass-media` (libass)

## Anti-features to expect

- **NonFreeNet**: TMDB, Trakt, Simkl, MDBList, the YouTube trailer resolver, debrid providers, and the parental guide from `api.tiffara.com` are services that are not free software.
- **Plugins**: plugins download and run JavaScript scrapers from third parties. Expect reviewers to ask about this, since F-Droid is wary of apps that download and run code.

## The F-Droid recipe

Once the binaries above can be built from source, the metadata file in `fdroiddata` will:

1. Delete `composeApp/libs/*.aar` in a `prebuild` step.
2. Build the engine with `-Pprovenio.engine.fromSource=true`.
3. Build the media3 modules and quickjs-kt as `srclibs`, and point Gradle at their outputs.
4. Run `./gradlew :androidApp:assembleRelease`.
