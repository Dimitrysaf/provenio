package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
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
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import io.github.dimitrysaf.provenio.theme.ThemeMode

/**
 * The Appearance settings themselves. No top bar, no scroll container — SettingsPage owns
 * that, so this renders identically whether it is a pane or a whole screen.
 */
@Composable
fun AppearanceContent(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onUseDynamicColorChange: (Boolean) -> Unit,
    dynamicColorAvailable: Boolean,
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    // Settings inside a category carry no icons — there are far more settings than there
    // are sensible icons to give them.
    SettingsGroup {
        SettingsMenuLink(
            title = { Text("Theme") },
            subtitle = { Text(themeMode.label) },
            onClick = { showThemeDialog = true },
        )
        // A plain row with a switch on the trailing edge. The filled, state-tinted
        // treatment belongs to a page's *main* toggle — the one that turns the whole
        // feature on — not to an ordinary setting.
        SettingsMenuLink(
            title = { Text("Dynamic color") },
            subtitle = {
                Text(
                    if (dynamicColorAvailable) "Use colors from your wallpaper"
                    else "Not available on this device",
                )
            },
            enabled = dynamicColorAvailable,
            action = {
                Switch(
                    checked = useDynamicColor && dynamicColorAvailable,
                    onCheckedChange = null,
                    enabled = dynamicColorAvailable,
                )
            },
            onClick = { onUseDynamicColorChange(!useDynamicColor) },
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
