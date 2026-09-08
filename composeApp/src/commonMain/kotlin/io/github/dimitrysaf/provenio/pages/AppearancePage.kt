package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.theme.ThemeMode
import io.github.dimitrysaf.provenio.ui.components.BackTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    PageScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            BackTopBar(title = "Appearance", onBack = onBack, scrollBehavior = scrollBehavior)
        },
    ) {
        ListItem(
            headlineContent = { Text("Theme") },
            supportingContent = { Text(themeMode.label) },
            modifier = Modifier.clickable { showThemeDialog = true },
        )

        // The whole row toggles, not just the switch: the switch itself takes no click of
        // its own so the row owns a single 48dp+ target and one state layer.
        ListItem(
            headlineContent = { Text("Dynamic color") },
            supportingContent = {
                Text(
                    if (dynamicColorAvailable) "Use colors from your wallpaper"
                    else "Not available on this device",
                )
            },
            trailingContent = {
                Switch(
                    checked = useDynamicColor && dynamicColorAvailable,
                    onCheckedChange = null,
                    enabled = dynamicColorAvailable,
                )
            },
            modifier = Modifier.toggleable(
                value = useDynamicColor && dynamicColorAvailable,
                enabled = dynamicColorAvailable,
                role = Role.Switch,
                onValueChange = onUseDynamicColorChange,
            ),
        )
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            selected = themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }
}

/**
 * A short enumerated choice is a dialog in M3, not a dropdown menu hung off a full-width
 * list row — a menu would anchor to the row's leading edge and read as an overflow menu.
 */
@Composable
private fun ThemeModeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = mode == selected,
                                onClick = { onSelect(mode) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(text = mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
