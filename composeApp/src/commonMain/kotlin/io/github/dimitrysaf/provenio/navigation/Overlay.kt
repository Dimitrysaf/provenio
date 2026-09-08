package io.github.dimitrysaf.provenio.navigation

/**
 * A page stacked on top of the tab layer. Overlays cover the navigation bar rather than
 * asking it to hide, so opening one never resizes the layer underneath.
 */
sealed interface Overlay {
    data object Settings : Overlay
    data object Search : Overlay
}
