package io.github.dimitrysaf.provenio.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.nav_home
import io.github.dimitrysaf.provenio.resources.nav_library
import io.github.dimitrysaf.provenio.resources.nav_search
import io.github.dimitrysaf.provenio.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

enum class Destination(
    val label: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(Res.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Search(Res.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search),
    Library(Res.string.nav_library, Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary),
    Settings(Res.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
}
