package io.github.dimitrysaf.provenio.navigation

/**
 * Navigation routes.
 *
 * [Home] is the only top-level destination: it owns the navigation rail/bar and the tab
 * set. Everything else is a sub-destination that covers the whole window, so opening one
 * never resizes the tab layer underneath it.
 *
 * Settings is a single destination: its categories are panes within it, not destinations
 * of their own, so they can sit side by side on a wide window.
 *
 * Routes carrying data are typed instead, see [DetailRoute] and [PlayerRoute].
 */
object Routes {
    const val Home = "home"

    const val Search = "search"
    const val SearchMovies = "search/movies"
    const val SearchTv = "search/tv"

    const val Settings = "settings"

    /**
     * Opens settings already on Add-ons. A plain second route rather than a navigation
     * argument, because arguments arrive as SavedState here and are not worth the reach
     * for one deep link.
     */
    const val SettingsAddons = "settings/addons"
}
