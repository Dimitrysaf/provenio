package io.github.dimitrysaf.provenio.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tv("TV", Icons.Filled.Tv, Icons.Outlined.Tv),
    Movies("Movies", Icons.Filled.Movie, Icons.Outlined.Movie),
    Lists("Lists", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
}
