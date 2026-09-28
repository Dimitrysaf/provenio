package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.build.isDesktop
import io.github.dimitrysaf.provenio.core.build.isMacOs
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

private class ShortcutEntry(
    val labelRes: StringResource,
    val combinations: List<List<ShortcutKey>>,
)

private sealed interface ShortcutKey {
    class Literal(val text: String) : ShortcutKey
    class Named(val res: StringResource) : ShortcutKey
}

private fun keys(vararg parts: Any): List<ShortcutKey> = parts.map { part ->
    when (part) {
        is StringResource -> ShortcutKey.Named(part)
        else -> ShortcutKey.Literal(part.toString())
    }
}

private val Ctrl = Res.string.keyboard_key_ctrl
private val Alt = Res.string.keyboard_key_alt
private val Shift = Res.string.keyboard_key_shift
private val Space = Res.string.keyboard_key_space
private val Esc = Res.string.keyboard_key_esc
private val Home = Res.string.keyboard_key_home
private val Menu = Res.string.keyboard_key_menu
private val RightClick = Res.string.keyboard_key_right_click

private fun generalShortcuts(): List<ShortcutEntry> = buildList {
    add(ShortcutEntry(Res.string.keyboard_action_home, listOf(keys(Ctrl, "1"))))
    add(ShortcutEntry(Res.string.keyboard_action_search, listOf(keys(Ctrl, "2"), keys(Ctrl, "F"), keys(Ctrl, "K"))))
    add(ShortcutEntry(Res.string.keyboard_action_library, listOf(keys(Ctrl, "3"))))
    add(ShortcutEntry(Res.string.keyboard_action_settings, listOf(keys(Ctrl, "4"), keys(Ctrl, ","))))
    add(ShortcutEntry(Res.string.keyboard_action_downloads, listOf(keys(Ctrl, "J"))))
    add(ShortcutEntry(Res.string.keyboard_action_back, if (isMacOs) listOf(keys(Ctrl, "[")) else listOf(keys(Alt, "←"), keys(Ctrl, "["))))
    add(ShortcutEntry(Res.string.keyboard_action_shortcuts, listOf(keys(Ctrl, "/"), keys("F1"))))
    if (isDesktop) {
        add(ShortcutEntry(Res.string.keyboard_action_fullscreen, listOf(keys("F11"))))
    }
}

private fun navigationShortcuts(): List<ShortcutEntry> = listOf(
    ShortcutEntry(Res.string.keyboard_action_move_focus, listOf(keys("Tab"), keys(Shift, "Tab"))),
    ShortcutEntry(Res.string.keyboard_action_open_item, listOf(keys("Enter"), keys(Space))),
    ShortcutEntry(Res.string.keyboard_action_item_menu, listOf(keys(RightClick), keys(Menu), keys(Shift, "F10"))),
    ShortcutEntry(Res.string.keyboard_action_close_sheet, listOf(keys(Esc))),
)

private fun playerShortcuts(): List<ShortcutEntry> = buildList {
    add(ShortcutEntry(Res.string.keyboard_action_play_pause, listOf(keys(Space), keys("K"))))
    add(ShortcutEntry(Res.string.keyboard_action_speed_up, listOf(keys(Res.string.keyboard_key_hold_space))))
    add(ShortcutEntry(Res.string.keyboard_action_seek_back, listOf(keys("←"), keys("J"))))
    add(ShortcutEntry(Res.string.keyboard_action_seek_forward, listOf(keys("→"), keys("L"))))
    add(ShortcutEntry(Res.string.keyboard_action_jump_percent, listOf(keys("0–9"))))
    add(ShortcutEntry(Res.string.keyboard_action_restart, listOf(keys(Home))))
    if (!isDesktop) {
        add(ShortcutEntry(Res.string.keyboard_action_volume, listOf(keys("↑"), keys("↓"))))
    }
    add(ShortcutEntry(Res.string.keyboard_action_mute, listOf(keys("M"))))
    add(ShortcutEntry(Res.string.keyboard_action_subtitles, listOf(keys("C"))))
    add(ShortcutEntry(Res.string.keyboard_action_audio, listOf(keys("A"))))
    add(ShortcutEntry(Res.string.keyboard_action_speed, listOf(keys("S"))))
    add(ShortcutEntry(Res.string.keyboard_action_next_episode, listOf(keys("N"))))
    if (isDesktop) {
        add(ShortcutEntry(Res.string.keyboard_action_player_fullscreen, listOf(keys("F"))))
    }
    add(ShortcutEntry(Res.string.keyboard_action_leave_player, listOf(keys(Esc))))
}

internal fun LazyListScope.keyboardShortcutsContent(isTablet: Boolean) {
    item {
        Text(
            text = stringResource(Res.string.keyboard_shortcuts_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        ShortcutSection(Res.string.keyboard_section_general, generalShortcuts(), isTablet)
    }
    item {
        ShortcutSection(Res.string.keyboard_section_navigation, navigationShortcuts(), isTablet)
    }
    item {
        ShortcutSection(Res.string.keyboard_section_player, playerShortcuts(), isTablet)
    }
}

@Composable
private fun ShortcutSection(
    titleRes: StringResource,
    entries: List<ShortcutEntry>,
    isTablet: Boolean,
) {
    SettingsSection(
        title = stringResource(titleRes),
        isTablet = isTablet,
    ) {
        val or = stringResource(Res.string.keyboard_or)
        SettingsList {
            entries.forEach { entry ->
                shapedRow { shape ->
                    ShortcutRow(
                        label = stringResource(entry.labelRes),
                        combinations = entry.combinations,
                        or = or,
                        shape = shape,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShortcutRow(
    label: String,
    combinations: List<List<ShortcutKey>>,
    or: String,
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                modifier = Modifier.widthIn(max = 260.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                combinations.forEachIndexed { index, combination ->
                    if (index > 0) {
                        Text(
                            text = or,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        combination.forEach { key ->
                            KeyCap(
                                text = when (key) {
                                    is ShortcutKey.Literal -> key.text
                                    is ShortcutKey.Named -> stringResource(key.res)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCap(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier
                .heightIn(min = 28.dp)
                .widthIn(min = 28.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
