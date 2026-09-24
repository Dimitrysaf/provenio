package io.github.dimitrysaf.provenio.shell.screens.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.EmptyState
import io.github.dimitrysaf.provenio.shell.components.NewEntryRow
import io.github.dimitrysaf.provenio.shell.components.ScreenScaffold
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.TextPromptDialog
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import io.github.dimitrysaf.provenio.core.addons.AddAddonResult
import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon

@Composable
fun AddonsScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
) {
    ScreenScaffold(
        title = title ?: stringResource(Res.string.addon_title),
        modifier = modifier,
        onBack = onBack,
    ) {
        item {
            AddonsSettingsPageContent()
        }
    }
}

private const val AddAddonRowKey = "addons:add"

/** How tall the list may grow before it scrolls inside the page. */
private val AddonListMaxHeight = 640.dp

@Composable
internal fun AddonsSettingsPageContent(
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val addons = uiState.addons
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var installModalState by remember { mutableStateOf<AddonInstallModalState?>(null) }
    var addonPendingDeletionUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var addonPendingConfigureUrl by rememberSaveable { mutableStateOf<String?>(null) }
    val urlCopiedMessage = stringResource(Res.string.addons_url_copied)

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
    ) { from, to ->
        // The add row holds the first slot and never moves, so the addons start one further in.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex >= 0 && toIndex >= 0) {
            AddonRepository.moveAddon(fromIndex, toIndex)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val installAddon: (String) -> Unit = { requestedUrl ->
        installModalState = AddonInstallModalState.Checking
        coroutineScope.launch {
            installModalState = when (val result = AddonRepository.addAddon(requestedUrl)) {
                is AddAddonResult.Success -> AddonInstallModalState.Success(result.manifest.name)
                is AddAddonResult.Error -> AddonInstallModalState.Error(result.message)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // How many are active is a property of this list, so it rides the heading rather than
        // taking a summary block of its own above it. With nothing installed there is no count
        // worth stating, and the empty state below says it better than "(0/0)" would.
        val heading = stringResource(Res.string.addons_section_installed)
        ListSubheader(
            text = if (addons.isEmpty()) {
                heading
            } else {
                "$heading (${addons.count { it.isActive }}/${addons.size})"
            },
        )

        // The row count includes the add row, so every addon's corners know where it sits.
        val rowCount = addons.size + 1
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = AddonListMaxHeight),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
        ) {
            item(key = AddAddonRowKey) {
                NewEntryRow(
                    text = stringResource(Res.string.addons_add_row),
                    shape = segmentShape(index = 0, count = rowCount),
                    onClick = { showAddDialog = true },
                )
            }

            itemsIndexed(addons, key = { _, addon -> addon.manifestUrl }) { index, addon ->
                ReorderableItem(reorderableLazyListState, key = addon.manifestUrl) { isDragging ->
                    val configureUrl = addon.configurePageUrl()

                    AddonListRow(
                        addon = addon,
                        shape = segmentShape(index = index + 1, count = rowCount),
                        selected = isDragging,
                        onEnabledChange = { enabled ->
                            AddonRepository.setAddonEnabled(addon.manifestUrl, enabled)
                        },
                        onConfigure = configureUrl?.let { url -> { addonPendingConfigureUrl = url } },
                        onDelete = { addonPendingDeletionUrl = addon.manifestUrl },
                        onCopyUrl = {
                            runCatching { clipboardManager.setText(AnnotatedString(addon.manifestUrl)) }
                                .onSuccess { ToastController.show(urlCopiedMessage) }
                        },
                        onReload = {
                            AddonRepository.refreshAddon(
                                manifestUrl = addon.manifestUrl,
                                forceRefresh = true,
                            )
                        },
                        dragHandleScope = this@ReorderableItem,
                    )
                }
            }
        }

        if (addons.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Extension,
                title = stringResource(Res.string.addons_empty_title),
                message = stringResource(Res.string.addons_empty_subtitle),
            )
        }
    }

    if (showAddDialog) {
        TextPromptDialog(
            title = stringResource(Res.string.addons_add_row),
            label = stringResource(Res.string.addons_input_placeholder),
            initialValue = "",
            confirmText = stringResource(Res.string.addons_install_button),
            keyboardType = KeyboardType.Uri,
            onConfirm = { url ->
                showAddDialog = false
                installAddon(url)
            },
            onDismiss = { showAddDialog = false },
        )
    }

    val pendingConfigureUrl = addonPendingConfigureUrl
    if (pendingConfigureUrl != null) {
        StatusModal(
            title = stringResource(Res.string.addons_configure_prompt_title),
            message = stringResource(Res.string.addons_configure_prompt_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                runCatching { uriHandler.openUri(pendingConfigureUrl) }
                addonPendingConfigureUrl = null
            },
            onDismiss = { addonPendingConfigureUrl = null },
        )
    }

    val pendingDeletionUrl = addonPendingDeletionUrl
    if (pendingDeletionUrl != null) {
        StatusModal(
            title = stringResource(Res.string.addons_delete_confirm_title),
            message = stringResource(Res.string.action_delete_confirm_message),
            isVisible = true,
            confirmText = stringResource(Res.string.action_yes),
            dismissText = stringResource(Res.string.action_no),
            onConfirm = {
                AddonRepository.removeAddon(pendingDeletionUrl)
                addonPendingDeletionUrl = null
            },
            onDismiss = { addonPendingDeletionUrl = null },
        )
    }

    val modalState = installModalState
    if (modalState != null) {
        val modalTitle = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_title)
            is AddonInstallModalState.Success -> stringResource(Res.string.addons_modal_success_title)
            is AddonInstallModalState.Error -> stringResource(Res.string.addons_modal_failure_title)
        }
        val modalMessage = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addons_modal_checking_message)
            is AddonInstallModalState.Success -> stringResource(
                Res.string.addons_modal_success_message,
                modalState.addonName,
            )
            is AddonInstallModalState.Error -> modalState.reason
        }
        val modalConfirmText = when (modalState) {
            AddonInstallModalState.Checking -> stringResource(Res.string.addon_installing)
            is AddonInstallModalState.Success -> stringResource(Res.string.action_done)
            is AddonInstallModalState.Error -> stringResource(Res.string.action_close)
        }
        StatusModal(
            title = modalTitle,
            message = modalMessage,
            isVisible = true,
            isBusy = modalState.isBusy,
            confirmText = modalConfirmText,
            onConfirm = {
                if (!modalState.isBusy) {
                    installModalState = null
                }
            },
        )
    }
}

/**
 * One installed addon.
 *
 * The row states what the addon is and whether it is on. What you can do to it lives in a menu
 * behind the trailing button, because four labelled actions do not fit across a phone's width and
 * a menu is what the spec offers for a row's overflow. Dragging is the row's one selected state,
 * and it starts from a long press rather than a handle, so the row carries no control that is
 * only there to be grabbed.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AddonListRow(
    addon: ManagedAddon,
    shape: RoundedCornerShape,
    selected: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onConfigure: (() -> Unit)?,
    onDelete: () -> Unit,
    onCopyUrl: () -> Unit,
    onReload: () -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val manifest = addon.manifest
    var menuOpen by remember { mutableStateOf(false) }
    val supporting = listOfNotNull(
        manifest?.version?.let { stringResource(Res.string.addons_version_format, it) },
        manifest?.description?.takeIf { it.isNotBlank() },
        addon.errorMessage,
    ).joinToString(" · ")

    SegmentedListItem(
        modifier = with(dragHandleScope) {
            Modifier.longPressDraggableHandle(
                onDragStarted = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragStopped = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
            )
        },
        selected = selected,
        onClick = { menuOpen = true },
        // Only the selected shape differs, so a dragged row takes the group's outer corner
        // while pressing or focusing one leaves it where it sits.
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = RoundedCornerShape(OuterCorner),
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            AddonIconBadge(
                imageUrl = manifest?.logoUrl,
                icon = Icons.Rounded.Extension,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // Neither line is clamped: an addon's name and description are what the row is for, so
        // the row grows to hold them rather than trimming them to a fixed height.
        supportingContent = supporting.takeIf { it.isNotBlank() }?.let {
            { Text(text = it) }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(
                    checked = addon.enabled,
                    onCheckedChange = onEnabledChange,
                )
                // The menu anchors to this box, so it opens at the button it belongs to.
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(Res.string.addons_more_actions),
                        )
                    }
                    AddonActionsMenu(
                        expanded = menuOpen,
                        isRefreshing = addon.isRefreshing,
                        onDismiss = { menuOpen = false },
                        onConfigure = onConfigure,
                        onCopyUrl = onCopyUrl,
                        onReload = onReload,
                        onDelete = onDelete,
                    )
                }
            }
        },
    ) {
        Text(text = addon.displayTitle)
    }
}

/** What can be done to one addon. Each action names itself and shows what it does. */
@Composable
private fun AddonActionsMenu(
    expanded: Boolean,
    isRefreshing: Boolean,
    onDismiss: () -> Unit,
    onConfigure: (() -> Unit)?,
    onCopyUrl: () -> Unit,
    onReload: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // An addon with no configuration page is offered no button, rather than one that can
        // only be dismissed.
        onConfigure?.let { configure ->
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.addons_configure_action)) },
                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                onClick = {
                    onDismiss()
                    configure()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.addons_copy_url)) },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            onClick = {
                onDismiss()
                onCopyUrl()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.addons_reload_manifest)) },
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
            enabled = !isRefreshing,
            onClick = {
                onDismiss()
                onReload()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.addons_delete)) },
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            colors = MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            ),
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}

private sealed interface AddonInstallModalState {
    val isBusy: Boolean

    data object Checking : AddonInstallModalState {
        override val isBusy: Boolean = true
    }

    data class Success(
        val addonName: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }

    data class Error(
        val reason: String,
    ) : AddonInstallModalState {
        override val isBusy: Boolean = false
    }
}

/** The addon's configuration page, when its manifest says it has one. */
private fun ManagedAddon.configurePageUrl(): String? {
    val hints = manifest?.behaviorHints ?: return null
    if (!hints.configurable && !hints.configurationRequired) return null
    return manifestUrl.toConfigureUrl().takeIf { it.isNotBlank() }
}

@Composable
private fun AddonIconBadge(
    imageUrl: String?,
    icon: ImageVector,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

private fun String.toConfigureUrl(): String {
    val base = substringBefore("?").trimEnd('/')
    return if (base.endsWith("/manifest.json")) {
        base.removeSuffix("/manifest.json") + "/configure"
    } else {
        "$base/configure"
    }
}
