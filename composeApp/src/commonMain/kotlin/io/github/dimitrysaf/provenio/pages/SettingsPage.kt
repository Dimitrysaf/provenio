package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.theme.ThemeMode
import kotlinx.coroutines.launch
import io.github.dimitrysaf.provenio.ui.backhandler.SystemBackHandler
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.SettingsTile
import io.github.dimitrysaf.provenio.ui.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.ui.components.tilePositionOf
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
    PeerToPeer("Peer-to-peer", "Sharing, cache and status", Icons.Outlined.Share),
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

    // Which category the detail pane shows is our state, not the navigator's. Reading it
    // back from the navigator meant that popping cleared it before the pane had finished
    // animating out, so the leaving pane swapped to the fallback category mid-exit.
    var selectedName by rememberSaveable { mutableStateOf(SettingsCategory.entries.first().name) }
    val selected = SettingsCategory.entries.firstOrNull { it.name == selectedName }
        ?: SettingsCategory.entries.first()

    // One definition of "back" for both the gesture and the top bar's arrow. While a
    // category is open on a single pane, back closes that category; only once there is
    // no pane left to close does it leave settings. canNavigateBack() reports false when
    // popping would not change what is on screen, so on a two-pane window the arrow
    // leaves settings directly rather than silently doing nothing.
    val canClosePane = navigator.canNavigateBack()
    val closePane: () -> Unit = { scope.launch { navigator.navigateBack() } }

    SystemBackHandler(enabled = canClosePane, onBack = closePane)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        BackTopBar(
            title = if (canClosePane) selected.title else "Settings",
            onBack = { if (canClosePane) closePane() else onBack() },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )

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
                                selectedName = category.name
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
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        val categories = SettingsCategory.entries
        categories.forEachIndexed { index, category ->
            SettingsTile(
                title = { Text(category.title) },
                subtitle = { Text(category.summary) },
                icon = { CategoryIcon(category) },
                position = tilePositionOf(index, categories.size),
                onClick = { onSelect(category) },
            )
        }
    }
}

/**
 * A category's icon sits in a filled circle, the way the platform settings do it. Colours
 * come from the scheme's container roles rather than fixed hues, so the tinting survives
 * dark mode and wallpaper-derived palettes.
 */
@Composable
private fun CategoryIcon(category: SettingsCategory) {
    val scheme = MaterialTheme.colorScheme
    val container: Color
    val content: Color
    when (category.ordinal % 3) {
        0 -> {
            container = scheme.primaryContainer
            content = scheme.onPrimaryContainer
        }
        1 -> {
            container = scheme.tertiaryContainer
            content = scheme.onTertiaryContainer
        }
        else -> {
            container = scheme.secondaryContainer
            content = scheme.onSecondaryContainer
        }
    }

    Box(
        modifier = Modifier.size(40.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(24.dp),
        )
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
        SettingsCategory.PeerToPeer -> P2pContent()
    }
}
