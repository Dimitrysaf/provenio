package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        SettingsCategory(
            title = "Appearance",
            summary = "Theme and dynamic color",
            icon = Icons.Outlined.Palette,
            onClick = onOpenAppearance,
        )

        SettingsCategory(
            title = "Add-ons",
            summary = "Catalogs, metadata and streams",
            icon = Icons.Outlined.Extension,
            onClick = onOpenAddons,
        )
    }
}

/**
 * A two-line list item is the M3 shape for a settings category — 72dp tall, one target.
 * Categories carry an icon; the individual settings inside them do not, since there are
 * far more of those than there are sensible icons to give them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategory(
    title: String,
    summary: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
