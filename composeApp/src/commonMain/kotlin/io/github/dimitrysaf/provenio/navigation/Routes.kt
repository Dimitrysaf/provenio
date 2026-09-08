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
 */
object Routes {
    const val Home = "home"
    const val Search = "search"

    const val Settings = "settings"

    /**
     * Opens settings already on Add-ons. A plain second route rather than a navigation
     * argument, because arguments arrive as SavedState here and are not worth the reach
     * for one deep link.
     */
    const val SettingsAddons = "settings/addons"

    const val Player = "player"

    /**
     * A public sample clip, used to prove the playback path end to end.
     *
     * The Google gtv-videos-bucket samples that every tutorial still links to now answer
     * 403. This one is W3C hosted and answers 206 with byte ranges, so seeking works.
     */
    const val SampleVideoUrl = "https://media.w3.org/2010/05/sintel/trailer.mp4"
}
