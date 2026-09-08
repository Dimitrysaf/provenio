package io.github.dimitrysaf.provenio.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold

/**
 * Settings root: a list of categories, each opening its own page. Nothing is configured
 * here directly, so the page stays readable as more categories arrive.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAddons: () -> Unit,
) {
    PageScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            BackTopBar(title = "Settings", onBack = onBack, scrollBehavior = scrollBehavior)
        },
    ) {
        SettingsGroup {
            SettingsMenuLink(
                title = { Text("Appearance") },
                subtitle = { Text("Theme and dynamic color") },
                icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                onClick = onOpenAppearance,
            )
            SettingsMenuLink(
                title = { Text("Add-ons") },
                subtitle = { Text("Catalogs, metadata and streams") },
                icon = { Icon(Icons.Outlined.Extension, contentDescription = null) },
                onClick = onOpenAddons,
            )
        }
    }
}
