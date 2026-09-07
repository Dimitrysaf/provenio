package io.github.dimitrysaf.provenio.navigation

sealed interface Screen {
    data class Tab(val destination: Destination) : Screen
    data object Settings : Screen
    data object Search : Screen
}
