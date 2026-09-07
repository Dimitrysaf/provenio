package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineLarge)

        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )

        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(themeMode.label) },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                modifier = Modifier.clickable { menuExpanded = true },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                ThemeMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        onClick = {
                            onThemeModeChange(mode)
                            menuExpanded = false
                        },
                        leadingIcon = {
                            if (mode == themeMode) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                            }
                        },
                    )
                }
            }
        }

        ListItem(
            headlineContent = { Text("Dynamic color") },
            supportingContent = {
                Text(
                    if (dynamicColorAvailable) "Use colors from your wallpaper"
                    else "Not available on this device",
                )
            },
            leadingContent = { Icon(Icons.Outlined.ColorLens, contentDescription = null) },
            trailingContent = {
                Switch(
                    checked = useDynamicColor && dynamicColorAvailable,
                    onCheckedChange = onUseDynamicColorChange,
                    enabled = dynamicColorAvailable,
                )
            },
        )
    }
}
