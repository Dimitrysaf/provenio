package io.github.dimitrysaf.provenio.navigation

/**
 * A page stacked on top of the tab layer. Overlays cover the navigation bar rather than
 * asking it to hide, so opening one never resizes the layer underneath.
 *
 * They are held in a stack, so a page can open a deeper page and back pops one level at a
 * time rather than dropping straight back to the tabs.
 */
sealed interface Overlay {
    data object Settings : Overlay
    data object Appearance : Overlay
    data object Search : Overlay
}
