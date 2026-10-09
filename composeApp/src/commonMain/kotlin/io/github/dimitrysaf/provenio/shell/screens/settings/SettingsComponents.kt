package io.github.dimitrysaf.provenio.shell.screens.settings

import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.theme.Tokens
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsItem
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_homescreen_collection_with_addon
import provenio.composeapp.generated.resources.settings_homescreen_hero_source
import provenio.composeapp.generated.resources.settings_homescreen_hidden
import provenio.composeapp.generated.resources.settings_homescreen_not_in_hero
import provenio.composeapp.generated.resources.settings_homescreen_pinned
import provenio.composeapp.generated.resources.settings_homescreen_pinned_to_top
import provenio.composeapp.generated.resources.settings_homescreen_visible
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
private fun SettingsCard(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.provenio
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = if (isTablet) RoundedCornerShape(Tokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(
            tokens.borders.hairline,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(content = content)
    }
}

/**
 * A sidebar destination. Unlike the settings rows on the other side, this genuinely is a
 * selection list — one category is active at a time — so it uses the selectable list item and
 * keeps the spec's shape morph: 16dp when selected, the 4dp inner corner otherwise.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsSidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    // The group's first and last items take the outer corner on their outward-facing edge, the
    // same rule the settings rows follow. Selected still morphs to a full 16dp.
    val unselected = segmentShape(index = index, count = count)
    val selectedShape = RoundedCornerShape(OuterCorner)
    SegmentedListItem(
        selected = selected,
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = unselected,
            pressedShape = unselected,
            focusedShape = unselected,
            hoveredShape = unselected,
            selectedShape = selectedShape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.padding(horizontal = 12.dp),
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null)
        },
    ) {
        Text(label)
    }
}

@Composable
internal fun SettingsSection(
    title: String,
    isTablet: Boolean,
    showTitle: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.provenio
    Column {
        if (showTitle) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListSubheader(text = title)
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
            Spacer(modifier = Modifier.height(if (isTablet) tokens.spacing.listGap else Tokens.Space.s10))
        }
        content()
    }
}

/**
 * A catalog in the Home layout list. [onRename] is null for a catalog that has nothing to edit,
 * a collection, and the row is then inert rather than clickable to no effect.
 */
@Composable
internal fun HomescreenCatalogRow(
    item: HomeCatalogSettingsItem,
    isTablet: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
    onRename: (() -> Unit)? = null,
    selected: Boolean = false,
    onPinnedDragAttempt: () -> Unit = {},
) {
    val tokens = MaterialTheme.provenio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp
    val hapticFeedback = LocalHapticFeedback.current
    // A selected row sits on the selected container, so its text takes that container's own
    // on-colour instead of the surface pair it would otherwise be unreadable against.
    val headlineColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val supportingColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // The whole row is the drag handle: holding it picks the row up, a tap still renames. A
    // pinned row cannot move, so holding it explains why instead.
    val reorderModifier = if (item.isPinnedToTop) {
        Modifier.pointerInput(onPinnedDragAttempt) {
            detectTapGestures(onLongPress = { onPinnedDragAttempt() })
        }
    } else {
        with(dragHandleScope) {
            Modifier.longPressDraggableHandle(
                onDragStarted = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragStopped = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(reorderModifier)
            .then(
                if (onRename != null) Modifier.clickable(onClick = onRename) else Modifier,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
                    .then(if (isTablet) Modifier.widthIn(max = 560.dp) else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = headlineColor,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (item.isCollection) {
                        stringResource(Res.string.settings_homescreen_collection_with_addon, item.addonName)
                    } else {
                        item.addonName
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor,
                )
                Text(
                    text = buildString {
                        append(
                            if (item.enabled) {
                                stringResource(Res.string.settings_homescreen_visible)
                            } else {
                                stringResource(Res.string.settings_homescreen_hidden)
                            },
                        )
                        if (item.isCollection) {
                            if (item.isPinnedToTop) {
                                append(" • ")
                                append(stringResource(Res.string.settings_homescreen_pinned_to_top))
                            }
                        } else {
                            append(" • ")
                            append(
                                if (item.heroSourceEnabled) {
                                    stringResource(Res.string.settings_homescreen_hero_source)
                                } else {
                                    stringResource(Res.string.settings_homescreen_not_in_hero)
                                },
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Switch(
                    checked = item.enabled,
                    onCheckedChange = onEnabledChange,
                )
                if (item.isPinnedToTop) {
                    WithTooltip(stringResource(Res.string.settings_homescreen_pinned)) {
                        IconButton(
                            onClick = onPinnedDragAttempt,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = stringResource(Res.string.settings_homescreen_pinned),
                                tint = supportingColor.copy(alpha = tokens.opacity.medium),
                            )
                        }
                    }
                }
            }
        }
    }
}
