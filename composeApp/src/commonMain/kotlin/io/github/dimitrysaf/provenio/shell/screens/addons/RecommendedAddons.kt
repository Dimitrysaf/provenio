package io.github.dimitrysaf.provenio.shell.screens.addons

internal const val CinemetaName = "Cinemeta"
internal const val CinemetaManifestUrl = "https://v3-cinemeta.strem.io/manifest.json"
internal const val OpenSubtitlesName = "OpenSubtitles"
internal const val OpenSubtitlesManifestUrl = "https://opensubtitles-v3.strem.io/manifest.json"

internal fun String.manifestKey(): String = substringBefore("?").trimEnd('/').lowercase()
