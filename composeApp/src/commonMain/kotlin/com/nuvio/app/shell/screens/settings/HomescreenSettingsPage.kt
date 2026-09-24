package com.nuvio.app.shell.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nuvio.app.shell.components.EmptyState
import com.nuvio.app.shell.components.NuvioLoadingIndicator
import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.shell.theme.nuvio
import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.shell.screens.home.HomeCatalogSettingsItem
import com.nuvio.app.shell.screens.home.HomeCatalogSettingsRepository
import com.nuvio.app.shell.screens.home.components.HomeEmptyStateCard
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_cancel
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.action_retry
import nuvio.composeapp.generated.resources.action_save
import nuvio.composeapp.generated.resources.layout_catalog_type
import nuvio.composeapp.generated.resources.layout_catalog_type_sub
import nuvio.composeapp.generated.resources.layout_hide_unreleased
import nuvio.composeapp.generated.resources.layout_hide_unreleased_sub
import nuvio.composeapp.generated.resources.settings_homescreen_display_name
import nuvio.composeapp.generated.resources.settings_homescreen_empty_message
import nuvio.composeapp.generated.resources.settings_homescreen_empty_title
import nuvio.composeapp.generated.resources.settings_homescreen_load_failed_title
import nuvio.composeapp.generated.resources.settings_homescreen_no_sources_selected
import nuvio.composeapp.generated.resources.settings_homescreen_pin_to_move_toast
import nuvio.composeapp.generated.resources.settings_homescreen_section_catalogs
import nuvio.composeapp.generated.resources.settings_homescreen_section_catalogs_collections
import nuvio.composeapp.generated.resources.settings_homescreen_section_collections
import nuvio.composeapp.generated.resources.settings_homescreen_section_hero
import nuvio.composeapp.generated.resources.settings_homescreen_section_hero_sources
import nuvio.composeapp.generated.resources.settings_homescreen_selected_count
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero
import nuvio.composeapp.generated.resources.settings_homescreen_show_hero_description
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

internal fun LazyListScope.homescreenSettingsContent(
    isTablet: Boolean,
    heroEnabled: Boolean,
    showCatalogType: Boolean,
    hideUnreleasedContent: Boolean,
    items: List<HomeCatalogSettingsItem>,
    isCatalogLoading: Boolean,
    catalogErrorMessage: String?,
) {
    val selectedHeroSourceCount = items.count { it.heroSourceEnabled }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_homescreen_section_hero),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_homescreen_show_hero),
                    description = stringResource(Res.string.settings_homescreen_show_hero_description),
                    checked = { heroEnabled },
                    onCheckedChange = HomeCatalogSettingsRepository::setHeroEnabled,
                )
                switchRow(
                    title = stringResource(Res.string.layout_catalog_type),
                    description = stringResource(Res.string.layout_catalog_type_sub),
                    checked = { showCatalogType },
                    onCheckedChange = HomeCatalogSettingsRepository::setShowCatalogType,
                )
                switchRow(
                    title = stringResource(Res.string.layout_hide_unreleased),
                    description = stringResource(Res.string.layout_hide_unreleased_sub),
                    checked = { hideUnreleasedContent },
                    onCheckedChange = HomeCatalogSettingsRepository::setHideUnreleasedContent,
                )
            }
        }
    }
    item {
        val catalogOnlyItems = items.filter { !it.isCollection }
        if (heroEnabled && catalogOnlyItems.isNotEmpty()) {
            var heroSourcesExpanded by remember { mutableStateOf(false) }
            SettingsSection(
                title = stringResource(Res.string.settings_homescreen_section_hero_sources),
                isTablet = isTablet,
            ) {
                HeroSourcesDropdown(
                    isTablet = isTablet,
                    items = catalogOnlyItems,
                    selectedHeroSourceCount = selectedHeroSourceCount,
                    expanded = heroSourcesExpanded,
                    onExpandedChange = { heroSourcesExpanded = it },
                )
            }
        }
    }
    item {
        if (isCatalogLoading && items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                NuvioLoadingIndicator(
                    modifier = Modifier.size(28.dp),
                )
            }
        } else if (catalogErrorMessage != null && items.isEmpty()) {
            HomeEmptyStateCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(Res.string.settings_homescreen_load_failed_title),
                message = catalogErrorMessage,
                actionLabel = stringResource(Res.string.action_retry),
                onActionClick = AddonRepository::refreshAll,
            )
        } else if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.GridView,
                title = stringResource(Res.string.settings_homescreen_empty_title),
                message = stringResource(Res.string.settings_homescreen_empty_message),
            )
        } else {
            val catalogCount = items.count { !it.isCollection }
            val collectionCount = items.count { it.isCollection }
            val sectionName = when {
                collectionCount > 0 && catalogCount > 0 -> stringResource(Res.string.settings_homescreen_section_catalogs_collections)
                collectionCount > 0 -> stringResource(Res.string.settings_homescreen_section_collections)
                else -> stringResource(Res.string.settings_homescreen_section_catalogs)
            }
            // How many are visible is a property of this list, so it rides its heading rather
            // than taking a block of its own at the top of the page.
            val enabledCount = items.count { it.enabled }
            val sectionTitle = "$sectionName ($enabledCount/${items.size})"

            SettingsSection(
                title = sectionTitle,
                isTablet = isTablet,
            ) {
                val hapticFeedback = LocalHapticFeedback.current
                val pinToMoveToast = stringResource(Res.string.settings_homescreen_pin_to_move_toast)

                HomescreenCatalogList(
                    isTablet = isTablet,
                    items = items,
                    onPinnedDragAttempt = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        NuvioToastController.show(pinToMoveToast)
                    },
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Resetting acts on the whole list, so it follows it rather than sitting in the
                // heading where it competes with the list's own name.
                TextButton(
                    onClick = HomeCatalogSettingsRepository::resetCatalogsToDefaults,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.action_reset))
                }
            }
        }
    }
}

@Composable
private fun HeroSourcesDropdown(
    isTablet: Boolean,
    items: List<HomeCatalogSettingsItem>,
    selectedHeroSourceCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val noSourcesSelected = stringResource(Res.string.settings_homescreen_no_sources_selected)
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "heroSourcesChevron",
    )
    // The summary and the sources it reveals are one segmented group, so they are declared as
    // rows: the summary then takes the group's top outer corner and the last source the bottom
    // one, and the corners stay right as the group grows and shrinks. Composed inside the list
    // instead of declared to it, they would have drawn on the page with no container at all.
    SettingsList(modifier = Modifier.animateContentSize()) {
        navigationRow(
            title = stringResource(
                Res.string.settings_homescreen_selected_count,
                selectedHeroSourceCount,
                HomeCatalogSettingsRepository.HERO_SOURCE_SELECTION_LIMIT,
            ),
            description = items.filter { it.heroSourceEnabled }
                .joinToString(separator = ", ") { it.displayTitle }
                .ifBlank { noSourcesSelected },
            trailingContent = {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = { onExpandedChange(!expanded) },
        )

        if (expanded) {
            items.forEach { item ->
                switchRow(
                    // No "limit reached" note: the row above already reads "n of n selected", and
                    // a row that cannot be turned on is already disabled.
                    title = item.displayTitle,
                    description = item.addonName,
                    checked = { item.heroSourceEnabled },
                    enabled = item.heroSourceEnabled ||
                        selectedHeroSourceCount < HomeCatalogSettingsRepository.HERO_SOURCE_SELECTION_LIMIT,
                    onCheckedChange = { HomeCatalogSettingsRepository.setHeroSourceEnabled(item.key, it) },
                )
            }
        }
    }
}

@Composable
private fun HomescreenCatalogList(
    isTablet: Boolean,
    items: List<HomeCatalogSettingsItem>,
    onPinnedDragAttempt: () -> Unit,
) {
    var renamingKey by remember { mutableStateOf<String?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        val fromItem = items.getOrNull(from.index)
        val toItem = items.getOrNull(to.index)
        if (fromItem?.isPinnedToTop == true || toItem?.isPinnedToTop == true) {
            return@rememberReorderableLazyListState
        }
        HomeCatalogSettingsRepository.moveByIndex(from.index, to.index)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Each row is its own segmented container, the same as the rest of the page's lists, so the
    // list is built from the rows directly rather than wrapped in a `SettingsList` that would
    // never have been handed them.
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = if (isTablet) 900.dp else 680.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
            ReorderableItem(
                reorderableLazyListState,
                key = item.key,
                enabled = !item.isPinnedToTop,
            ) { isDragging ->
                // A row being dragged is the one case these rows have a selected state, so it
                // takes the spec's selected treatment: the container morphs to the group's outer
                // corner on every edge, and the tone steps up to the selected pair. It lifts as
                // well, since it is travelling over the rows it passes.
                val topCorner by animateDpAsState(
                    targetValue = if (isDragging || index == 0) OuterCorner else InnerCorner,
                    label = "catalogRowTopCorner",
                )
                val bottomCorner by animateDpAsState(
                    targetValue = if (isDragging || index == items.lastIndex) OuterCorner else InnerCorner,
                    label = "catalogRowBottomCorner",
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isDragging) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    label = "catalogRowContainer",
                )
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 6.dp else 0.dp,
                    label = "catalogRowElevation",
                )

                // A collection's title is its own, so only a catalog opens the editor; a row with
                // nothing to edit is left inert rather than clickable to no effect.
                val onRenameCatalog: (() -> Unit)? = if (item.isCollection) {
                    null
                } else {
                    { renamingKey = item.key }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = topCorner,
                        topEnd = topCorner,
                        bottomStart = bottomCorner,
                        bottomEnd = bottomCorner,
                    ),
                    color = containerColor,
                    shadowElevation = elevation,
                ) {
                    HomescreenCatalogRow(
                        item = item,
                        isTablet = isTablet,
                        selected = isDragging,
                        onRename = onRenameCatalog,
                        onEnabledChange = { HomeCatalogSettingsRepository.setEnabled(item.key, it) },
                        dragHandleScope = this@ReorderableItem,
                        onPinnedDragAttempt = onPinnedDragAttempt,
                    )
                }
            }
        }
    }

    val renamingItem = items.find { it.key == renamingKey }
    if (renamingItem != null) {
        HomescreenCatalogRenameDialog(
            item = renamingItem,
            onConfirm = { title ->
                HomeCatalogSettingsRepository.setCustomTitle(renamingItem.key, title)
                renamingKey = null
            },
            onDismiss = { renamingKey = null },
        )
    }
}

/**
 * Renames one catalog. Editing a name is a short, self-contained task with a clear commit point,
 * so it is a dialog: the list keeps its place behind it, and the draft is only written when the
 * edit is confirmed, which gives the cancel something to cancel.
 */
@Composable
private fun HomescreenCatalogRenameDialog(
    item: HomeCatalogSettingsItem,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    // Keyed on the catalog, so opening another one starts from that one's name rather than
    // holding the last edit.
    var draft by remember(item.key) { mutableStateOf(item.customTitle) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_homescreen_display_name)) },
        text = {
            // Requested from inside the dialog's own content: the dialog composes in a
            // subcomposition of its own, so an effect in the body around it can run before this
            // field exists and would find the requester unattached.
            LaunchedEffect(item.key) { focusRequester.requestFocus() }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                // Empty means "use the catalog's own name", so the default is shown as the
                // placeholder rather than prefilled, which would make clearing it impossible.
                placeholder = { Text(item.defaultTitle) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.colors.borderFocus.copy(alpha = tokens.opacity.strong),
                    unfocusedBorderColor = tokens.colors.borderDefault.copy(alpha = tokens.opacity.medium),
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onConfirm(draft) }),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}
