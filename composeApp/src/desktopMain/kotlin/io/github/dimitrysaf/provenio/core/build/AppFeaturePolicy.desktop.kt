package io.github.dimitrysaf.provenio.core.build

// Desktop has no plugin runtime.

actual object AppFeaturePolicy {
    actual val pluginsEnabled: Boolean = false
    actual val personalMediaAddonCopyEnabled: Boolean = false
    actual val p2pEnabled: Boolean = true
    actual val trailerPlaybackMode: TrailerPlaybackMode = TrailerPlaybackMode.IN_APP
    actual val heroTrailerPlaybackSupported: Boolean = false
    actual val inAppUpdaterEnabled: Boolean = io.github.dimitrysaf.provenio.core.updater.AppUpdaterPlatform.isSupported
    actual val imdbRatingLogoEnabled: Boolean = false
    actual val mediaPlaybackForegroundServiceEnabled: Boolean = false
    actual val downloadForegroundServiceEnabled: Boolean = false
}
