package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.ui.backhandler.SystemBackHandler
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold
import io.github.dimitrysaf.provenio.ui.components.ResponsiveBody
import io.github.dimitrysaf.provenio.ui.responsive.listPaneWidth
import io.github.dimitrysaf.provenio.ui.responsive.usesTwoPanes
import io.github.dimitrysaf.provenio.ui.responsive.windowSizeClassOf

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
    // Stored by name so it survives the destination being disposed and recreated.
    var openCategoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val openCategory = openCategoryName?.let { name ->
        SettingsCategory.entries.firstOrNull { it.name == name }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val twoPanes = windowSizeClassOf(maxWidth).usesTwoPanes()

        // On one pane, back should close the open category before leaving settings.
        SystemBackHandler(enabled = !twoPanes && openCategory != null) {
            openCategoryName = null
        }

        if (twoPanes) {
            val selected = openCategory ?: SettingsCategory.entries.first()
            Column(modifier = Modifier.fillMaxSize()) {
                BackTopBar(title = "Settings", onBack = onBack)
                Row(modifier = Modifier.weight(1f)) {
                    ResponsiveBody(modifier = Modifier.width(listPaneWidth).fillMaxHeight()) {
                        CategoryList(
                            selected = selected,
                            onSelect = { openCategoryName = it.name },
                        )
                    }
                    VerticalDivider()
                    ResponsiveBody(modifier = Modifier.weight(1f).fillMaxHeight()) {
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
            }
        } else if (openCategory == null) {
            PageScaffold(
                topBar = { scrollBehavior ->
                    BackTopBar("Settings", onBack = onBack, scrollBehavior = scrollBehavior)
                },
            ) {
                CategoryList(selected = null, onSelect = { openCategoryName = it.name })
            }
        } else {
            PageScaffold(
                topBar = { scrollBehavior ->
                    BackTopBar(
                        title = openCategory.title,
                        onBack = { openCategoryName = null },
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) {
                CategoryContent(
                    category = openCategory,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    useDynamicColor = useDynamicColor,
                    onUseDynamicColorChange = onUseDynamicColorChange,
                    dynamicColorAvailable = dynamicColorAvailable,
                )
            }
        }
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
