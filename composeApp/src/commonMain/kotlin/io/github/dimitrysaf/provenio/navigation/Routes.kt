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

    const val Player = "player"

    /** A public sample clip, used to prove the playback path end to end. */
    const val SampleVideoUrl =
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
}
