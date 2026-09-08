package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import io.github.dimitrysaf.provenio.theme.ThemeMode
import kotlinx.coroutines.launch
import io.github.dimitrysaf.provenio.ui.backhandler.SystemBackHandler
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.ResponsiveBody

/**
 * A settings category. Adding one here is the whole job — both layouts read this list, so
 * a category cannot exist in one and not the other.
 */
enum class SettingsCategory(
    val title: String,
    val summary: String,
    val icon: ImageVector,
) {
    Appearance("Appearance", "Theme and dynamic color", Icons.Outlined.Palette),
    Addons("Add-ons", "Catalogs, metadata and streams", Icons.Outlined.Extension),
}

/**
 * Settings, in one of two shapes.
 *
 * Wide enough for two panes and the categories sit beside their settings; narrower and
 * the category list is the page, with a category opening over it. Both shapes are driven
 * from the same category list and the same content composables, so the two cannot drift
 * apart visually.
 */
/**
 * Settings as an M3 list-detail layout.
 *
 * The scaffold decides for itself whether the two panes sit side by side or one at a time,
 * from the same window metrics the rest of the app uses, and owns back between them. Both
 * shapes are fed by the same category list and the same content composables, so they
 * cannot drift apart visually.
 *
 * Back *within* settings just pops the detail pane. Predictive back is left to the
 * navigation host, where it moves between real destinations — Settings back to Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    // Keyed by name rather than the enum itself so the navigator's saved state stays a
    // plain string.
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()

    val selected = navigator.currentDestination?.contentKey?.let { name ->
        SettingsCategory.entries.firstOrNull { it.name == name }
    } ?: SettingsCategory.entries.first()

    // Back inside settings closes the detail pane and nothing more. Leaving settings
    // altogether is the navigation host's job, which is where predictive back lives.
    SystemBackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch { navigator.navigateBack() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        BackTopBar(title = "Settings", onBack = onBack)

        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            modifier = Modifier.weight(1f),
            listPane = {
                AnimatedPane {
                    ResponsiveBody(modifier = Modifier.fillMaxSize()) {
                        CategoryList(
                            selected = selected,
                            onSelect = { category ->
                                scope.launch {
                                    navigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail,
                                        category.name,
                                    )
                                }
                            },
                        )
                    }
                }
            },
            detailPane = {
                AnimatedPane {
                    ResponsiveBody(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = selected.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        CategoryContent(
                            category = selected,
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            useDynamicColor = useDynamicColor,
                            onUseDynamicColorChange = onUseDynamicColorChange,
                            dynamicColorAvailable = dynamicColorAvailable,
                        )
                    }
                }
            },
        )
    }
}

/** Categories carry an icon; the individual settings inside them do not. */
@Composable
private fun CategoryList(
    selected: SettingsCategory?,
    onSelect: (SettingsCategory) -> Unit,
) {
    SettingsGroup {
        SettingsCategory.entries.forEach { category ->
            SettingsMenuLink(
                title = { Text(category.title) },
                subtitle = { Text(category.summary) },
                icon = { Icon(category.icon, contentDescription = null) },
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryContent(
    category: SettingsCategory,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    when (category) {
        SettingsCategory.Appearance -> AppearanceContent(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            useDynamicColor = useDynamicColor,
            onUseDynamicColorChange = onUseDynamicColorChange,
            dynamicColorAvailable = dynamicColorAvailable,
        )
        SettingsCategory.Addons -> AddonsContent()
    }
}
