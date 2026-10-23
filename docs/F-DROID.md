# Getting Provenio into F-Droid

F-Droid builds every app from source on its own servers and rejects binaries committed to the repository. This file lists what stood in the way, what is done, and how the F-Droid build works.

## Done

- **No proprietary libraries.** Sentry crash reporting and Google's ML Kit code scanner are gone. Local sync scans pairing codes with its own scanner, built on CameraX and ZXing, both open source.
- **No Play Store build.** There is a single Android build with no product flavours.
- **No self-updates from F-Droid.** The Android updater only appears when Provenio was installed by hand. When F-Droid (or another store or update manager) installed it, the update button and banner stay hidden, because `AndroidAppUpdaterPlatform.isInstalledByHand()` checks the installing package.
- **No plugins.** Plugins downloaded JavaScript scrapers from third-party repositories and ran them inside the app, which F-Droid only allows behind an explicit opt-in. They are gone, along with their QuickJS runtime and its prebuilt `quickjs-kt-android-1.0.5-nuvio.aar`. Streams come only from addons and the app's own sources.
- **No piracy addon in onboarding.** Onboarding no longer offers Torrentio, and installing a recommended addon asks first.
- **No MPEG-H decoder.** The Fraunhofer MPEG-H library is under a non-free licence, so `lib-decoder-mpegh-release.aar` was removed. MPEG-H tracks still get a name in the track list, but are not decoded.

## Prebuilt binaries in `composeApp/libs/`

`composeApp/build.gradle.kts` adds every `lib-*.aar` in that folder to the Android app. F-Droid deletes them and builds each from source; the draft recipe in [`fdroid/io.github.dimitrysaf.provenio.yml`](fdroid/io.github.dimitrysaf.provenio.yml) does all of it.

| File | Where it comes from | How F-Droid builds it |
| --- | --- | --- |
| `lib-engine-android-0.1.1.aar` | The torrent engine in `engine/` (libtorrent and OpenSSL, with a JNI bridge). `lib-engine-android.source` records the commit it was built from. | `-Pprovenio.engine.fromSource=true` includes `engine/platform/android/engine` as the `:engine` project, which builds it with NDK 29 and CMake. Its Gradle tasks download the pinned OpenSSL, Boost and libtorrent sources, checked by hash; reviewers may ask for those as `srclibs` instead. |
| `lib-exoplayer-release.aar`, `lib-ui-release.aar` | [moneytoo's media3 fork](https://github.com/moneytoo/media) at its `1.8.0` branch, commit `9a3eaf3`. Upstream Nuvio added them as "media3 1.8.0 jp libs", that is, Just Player's. The fork patches three ExoPlayer files (`DefaultRenderersFactory`, `MediaCodecUtil`, `MediaCodecVideoRenderer`) and the UI's controls and subtitle drawing, which adds `PaddingBackgroundColorSpan`. Their classes match the fork's sources one for one. | The `media-moneytoo` srclib, which F-Droid already builds for [Just Player](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.brouken.player.yml). |
| `lib-decoder-ffmpeg-release.aar` | media3's own FFmpeg extension (not published to Maven), built against FFmpeg 6.0. | The same srclib, with the `FFmpeg@release/6.0` srclib and `build_ffmpeg.sh`. |
| `lib-decoder-av1-release.aar` | media3's own AV1 extension, using libgav1. | The same srclib, with the `cpu_features`, `libgav1` and `abseil-cpp` srclibs, as Just Player's media3 1.6 builds did. |

## Maven dependencies with native code

These come from Maven Central and contain prebuilt native libraries. F-Droid's scanner allows FOSS Maven dependencies, but check them during review:

- `io.github.abdallahmehiz:mpv-android-lib` (libmpv and its FFmpeg)
- `io.github.peerless2012:ass-media` (libass)

## Anti-features to expect

- **NonFreeNet**: TMDB, Trakt, Simkl, MDBList, the YouTube trailer resolver, debrid providers, and the parental guide from `api.tiffara.com` are services that are not free software.

## API keys

F-Droid builds without `local.properties` and does not sign up for API keys, so the keys the CI workflow passes in from its secrets would be missing. `app-keys.properties` holds the public ones every build falls back to: the Trakt, Simkl and Premiumize client IDs, which the app already sends in its requests (Trakt and Simkl sign in with PKCE, so there is no secret), and the TMDB key. A key set in `local.properties` still takes their place.

## The F-Droid recipe

[`fdroid/io.github.dimitrysaf.provenio.yml`](fdroid/io.github.dimitrysaf.provenio.yml) is the draft metadata file. It deletes `composeApp/libs/`, builds the four media3 AARs from their srclibs into that folder, then builds the app with the engine from source. To submit it:

1. Fill in `app-keys.properties` and push it.
2. Pick the commit to publish, and set it, its build number, `versionName` and `versionCode` where the file says CHANGE. The build number is passed as `-Pprovenio.buildNumber`, so the version matches the GitHub release built from that commit.
3. Open a merge request to [fdroiddata](https://gitlab.com/fdroid/fdroiddata) adding it as `metadata/io.github.dimitrysaf.provenio.yml`. Its CI runs the build; reviewers check the rest.
