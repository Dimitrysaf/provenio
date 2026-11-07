package io.github.dimitrysaf.provenio.shell.theme

/**
 * Keeps the theme's primary and background colours, as ARGB, for the next launch screen, which
 * the platform draws before the app's theme exists. A no-op where the launch screen is the app's own.
 */
internal expect fun saveLaunchColors(primary: Int, background: Int)
