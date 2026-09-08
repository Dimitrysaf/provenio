package io.github.dimitrysaf.provenio.navigation

/**
 * Navigation routes.
 *
 * [Home] is the only top-level destination: it owns the navigation rail/bar and the tab
 * set. Everything else is a sub-destination that covers the whole window, so opening one
 * never resizes the tab layer underneath it.
 *
 * Settings is a nested graph rather than a flat route — its categories are sub-pages of
 * Settings, so back walks Appearance → Settings → Home one step at a time.
 */
object Routes {
    const val Home = "home"
    const val Search = "search"

    const val SettingsGraph = "settings"
    const val SettingsRoot = "settings/root"
    const val SettingsAppearance = "settings/appearance"
}
