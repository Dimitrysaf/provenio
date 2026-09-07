package io.github.dimitrysaf.provenio.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person),
    Tv("TV", Icons.Filled.Tv, Icons.Outlined.Tv),
    Movies("Movies", Icons.Filled.Movie, Icons.Outlined.Movie),
    Lists("Lists", Icons.Filled.List, Icons.Outlined.List),
    Settings("Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
}
