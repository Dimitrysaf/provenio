package io.github.dimitrysaf.provenio.feature.settings.panes

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsGroup
import io.github.dimitrysaf.provenio.core.i18n.AppLanguage
import io.github.dimitrysaf.provenio.core.i18n.LanguageRepository
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTile
import io.github.dimitrysaf.provenio.designsystem.components.SettingsTileSpacing
import io.github.dimitrysaf.provenio.designsystem.components.TilePosition
import io.github.dimitrysaf.provenio.designsystem.theme.ThemeMode
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.appearance_dynamic_color
import io.github.dimitrysaf.provenio.resources.appearance_dynamic_color_off
import io.github.dimitrysaf.provenio.resources.appearance_dynamic_color_on
import io.github.dimitrysaf.provenio.resources.appearance_theme
import io.github.dimitrysaf.provenio.resources.cancel
import io.github.dimitrysaf.provenio.resources.language
import io.github.dimitrysaf.provenio.resources.language_system
import org.jetbrains.compose.resources.stringResource

/**
 * The Appearance settings themselves. No top bar, no scroll container — SettingsScreen owns
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    val language by LanguageRepository.language.collectAsState()

    // Settings inside a category carry no icons — there are far more settings than there
    // are sensible icons to give them.
    SettingsGroup(verticalArrangement = Arrangement.spacedBy(SettingsTileSpacing)) {
        SettingsTile(
            title = { Text(stringResource(Res.string.appearance_theme)) },
            subtitle = { Text(stringResource(themeMode.label)) },
            position = TilePosition.First,
            onClick = { showThemeDialog = true },
        )
        SettingsTile(
            title = { Text(stringResource(Res.string.language)) },
            subtitle = { Text(languageLabel(language)) },
            position = TilePosition.Middle,
            onClick = { showLanguageDialog = true },
        )
        // A plain row with a switch on the trailing edge. The filled, state-tinted
        // treatment belongs to a page's *main* toggle — the one that turns the whole
        // feature on — not to an ordinary setting.
        SettingsTile(
            title = { Text(stringResource(Res.string.appearance_dynamic_color)) },
            subtitle = {
                Text(
                    if (dynamicColorAvailable) {
                        stringResource(Res.string.appearance_dynamic_color_on)
                    } else {
                        stringResource(Res.string.appearance_dynamic_color_off)
                    },
                )
            },
            enabled = dynamicColorAvailable,
            position = TilePosition.Last,
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
        ChoiceDialog(
            title = stringResource(Res.string.appearance_theme),
            options = ThemeMode.entries,
            labelOf = { stringResource(it.label) },
            selected = themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(Res.string.language),
            options = AppLanguage.entries,
            labelOf = { languageLabel(it) },
            selected = language,
            onSelect = {
                LanguageRepository.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

/**
 * A language is named in itself, so the entry someone is looking for is readable even when
 * the rest of the app is in a language they cannot read. Only "system" is translated,
 * because it names a setting rather than a language.
 */
@Composable
private fun languageLabel(language: AppLanguage): String =
    language.endonym ?: stringResource(Res.string.language_system)

/**
 * A short enumerated choice is a dialog in M3, not a dropdown menu hung off a full-width
 * list row — a menu would anchor to the row's leading edge and read as an overflow menu.
 */
@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    labelOf: @Composable (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = option == selected,
                                onClick = { onSelect(option) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = labelOf(option),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
