package io.github.dimitrysaf.provenio.core.build

actual object AppFeaturePolicy {
    actual val pluginsEnabled: Boolean = false
    actual val personalMediaAddonCopyEnabled: Boolean = true
    actual val p2pEnabled: Boolean = false
    actual val trailerPlaybackMode: TrailerPlaybackMode = TrailerPlaybackMode.EXTERNAL
    actual val heroTrailerPlaybackSupported: Boolean = false
    actual val inAppUpdaterEnabled: Boolean = false
    actual val imdbRatingLogoEnabled: Boolean = false
    actual val mediaPlaybackForegroundServiceEnabled: Boolean = false
    actual val downloadForegroundServiceEnabled: Boolean = false
}
