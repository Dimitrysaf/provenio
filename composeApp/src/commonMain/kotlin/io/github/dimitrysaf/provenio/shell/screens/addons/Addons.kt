package io.github.dimitrysaf.provenio.shell.screens.addons

import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
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
import io.github.dimitrysaf.provenio.shell.components.StatusModal
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.TextPromptDialog
import io.github.dimitrysaf.provenio.shell.components.onSecondaryClick
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList
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

private const val AddAddonRowKey = "addons:add"

/** How tall the list may grow before it scrolls inside the page. */
private val AddonListMaxHeight = 640.dp

@Composable
internal fun AddonsSettingsPageContent(
    modifier: Modifier = Modifier,
    onGuideClick: (() -> Unit)? = null,
) {
    LaunchedEffect(Unit) {
        AddonRepository.initialize()
    }

    val uiState by AddonRepository.uiState.collectAsStateWithLifecycle()
    val addRequested by AddonAddRequests.requested.collectAsStateWithLifecycle()
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

    LaunchedEffect(addRequested) {
        if (addRequested) {
            showAddDialog = true
            AddonAddRequests.consume()
        }
    }

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
        if (onGuideClick != null) {
            SettingsList {
                navigationRow(
                    title = stringResource(Res.string.addons_guide_title),
                    description = stringResource(Res.string.addons_guide_entry_description),
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    opensPage = true,
                    onClick = onGuideClick,
                )
            }
        }

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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = AddonListMaxHeight),
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = AddAddonRowKey) {
                NewEntryRow(
                    text = stringResource(Res.string.addons_add_row),
                    shape = segmentShape(index = 0, count = 1),
                    onClick = { showAddDialog = true },
                )
            }

            itemsIndexed(addons, key = { _, addon -> addon.manifestUrl }) { index, addon ->
                ReorderableItem(reorderableLazyListState, key = addon.manifestUrl) { isDragging ->
                    val configureUrl = addon.configurePageUrl()

                    AddonCard(
                        addon = addon,
                        dragging = isDragging,
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
 * One installed addon, as a card in the Accounts cards' style: a banner with its logo, name and
 * version, tags for what it provides, then a row for each thing that can be done to it. Holding
 * the banner and dragging moves the addon in the list.
 */
@Composable
private fun AddonCard(
    addon: ManagedAddon,
    dragging: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onConfigure: (() -> Unit)?,
    onDelete: () -> Unit,
    onCopyUrl: () -> Unit,
    onReload: () -> Unit,
    dragHandleScope: ReorderableCollectionItemScope,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val manifest = addon.manifest
    val tags = addonTags(addon)
    val enabledTitle = stringResource(Res.string.addons_enabled)
    val description = listOfNotNull(
        manifest?.description?.takeIf { it.isNotBlank() },
        addon.errorMessage,
    ).joinToString(" · ").takeIf(String::isNotBlank)
    val configureTitle = stringResource(Res.string.addons_configure_action)
    val copyTitle = stringResource(Res.string.addons_copy_url)
    val reloadTitle = stringResource(Res.string.addons_reload_manifest)
    val deleteTitle = stringResource(Res.string.addons_delete)
    val dragModifier = with(dragHandleScope) {
        Modifier.longPressDraggableHandle(
            onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
            onDragStopped = { hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
        )
    }

    SettingsList(
        modifier = Modifier.graphicsLayer {
            val scale = if (dragging) 1.02f else 1f
            scaleX = scale
            scaleY = scale
        },
    ) {
        shapedRow { shape ->
            AddonBanner(
                title = addon.displayTitle,
                version = manifest?.version?.let { stringResource(Res.string.addons_version_format, it) },
                logoUrl = manifest?.logoUrl,
                shape = shape,
                modifier = dragModifier,
            )
        }
        if (tags.isNotEmpty()) {
            shapedRow { shape ->
                AddonTagsRow(tags = tags, active = addon.isActive, shape = shape)
            }
        }
        switchRow(
            title = enabledTitle,
            description = description,
            checked = { addon.enabled },
            onCheckedChange = onEnabledChange,
        )
        if (onConfigure != null) {
            navigationRow(title = configureTitle, onClick = onConfigure)
        }
        navigationRow(title = copyTitle, onClick = onCopyUrl)
        navigationRow(
            title = reloadTitle,
            enabled = !addon.isRefreshing,
            onClick = onReload,
        )
        navigationRow(title = deleteTitle, onClick = onDelete)
    }
}

/** What the addon provides, its resources then its content types, as tags. */
@Composable
private fun addonTags(addon: ManagedAddon): List<Pair<ImageVector, String>> {
    val manifest = addon.manifest ?: return emptyList()
    val resources = manifest.resources.map { it.name.lowercase() }.distinct().map { resource ->
        when (resource) {
            "catalog" -> Icons.Rounded.CollectionsBookmark to stringResource(Res.string.addons_resource_catalogs)
            "meta" -> Icons.Rounded.Info to stringResource(Res.string.addons_resource_details)
            "stream" -> Icons.Rounded.PlayArrow to stringResource(Res.string.addons_resource_streams)
            "subtitles" -> Icons.Rounded.Subtitles to stringResource(Res.string.addons_resource_subtitles)
            "addon_catalog" -> Icons.Rounded.Extension to stringResource(Res.string.addons_resource_addon_catalogs)
            else -> Icons.Rounded.Extension to resource.replaceFirstChar { it.uppercase() }
        }
    }
    val types = manifest.types.map { it.lowercase() }.distinct().map { type ->
        when (type) {
            "movie" -> Icons.Rounded.Movie to stringResource(Res.string.addons_type_movies)
            "series" -> Icons.Rounded.Tv to stringResource(Res.string.addons_type_series)
            "channel" -> Icons.Rounded.LiveTv to stringResource(Res.string.addons_type_channels)
            "tv" -> Icons.Rounded.LiveTv to stringResource(Res.string.addons_type_tv)
            else -> Icons.Rounded.Movie to type.replaceFirstChar { it.uppercase() }
        }
    }
    return resources + types
}

/** The addon's banner: its logo blurred behind, the logo itself, its name and version. */
@Composable
private fun AddonBanner(
    title: String,
    version: String?,
    logoUrl: String?,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val container = MaterialTheme.colorScheme.secondaryContainer
    val content = MaterialTheme.colorScheme.onSecondaryContainer
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AddonBannerHeight)
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(container, MaterialTheme.colorScheme.tertiaryContainer)),
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .blur(28.dp)
                    .alpha(0.45f),
                contentScale = ContentScale.Crop,
            )
        }
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AddonIconBadge(
                imageUrl = logoUrl,
                icon = Icons.Rounded.Extension,
                tint = content,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = content,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (version != null) {
                    Text(
                        text = version,
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

/** What the addon provides, as tonal chips: filled in while it is on, muted while it is off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddonTagsRow(
    tags: List<Pair<ImageVector, String>>,
    active: Boolean,
    shape: RoundedCornerShape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tags.forEach { (icon, label) ->
                Surface(
                    shape = CircleShape,
                    color = if (active) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    contentColor = if (active) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(text = label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private val AddonBannerHeight = 104.dp

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
