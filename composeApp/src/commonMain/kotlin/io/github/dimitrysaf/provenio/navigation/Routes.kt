package io.github.dimitrysaf.provenio.navigation

/**
 * Navigation routes.
 *
 * [Home] is the only top-level destination: it owns the navigation rail/bar and the tab
 * set. Search and Settings are tabs within it rather than routes of their own, so moving
 * between them never covers the navigation.
 *
 * What is left as a route is what genuinely covers the whole window: a title's details and
 * the player. Both carry data, so both are typed — see [DetailRoute] and [PlayerRoute].
 */
object Routes {
    const val Home = "home"
}
