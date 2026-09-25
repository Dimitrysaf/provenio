package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.dimitrysaf.provenio.shell.components.ContentDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.shell.theme.Tokens
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.theme.provenio
import io.github.dimitrysaf.provenio.core.streams.STREAM_BADGE_IMPORT_LIMIT
import io.github.dimitrysaf.provenio.shell.screens.streams.StreamBadgeChip
import io.github.dimitrysaf.provenio.shell.screens.streams.StreamBadgeChipSize
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeFilter
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeImport
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeImportResult
import io.github.dimitrysaf.provenio.core.streams.StreamBadgePlacement
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeRules
import io.github.dimitrysaf.provenio.core.streams.StreamBadgeSettingsRepository
import io.github.dimitrysaf.provenio.core.streams.StreamBackgroundMode
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.action_delete
import provenio.composeapp.generated.resources.action_import
import provenio.composeapp.generated.resources.settings_fusion_badge_group_title
import provenio.composeapp.generated.resources.settings_fusion_badge_other_group_title
import provenio.composeapp.generated.resources.settings_fusion_badge_preview_action
import provenio.composeapp.generated.resources.settings_fusion_badge_preview_count
import provenio.composeapp.generated.resources.settings_fusion_badge_preview_empty
import provenio.composeapp.generated.resources.settings_fusion_badge_preview_title
import provenio.composeapp.generated.resources.settings_fusion_badge_url_active
import provenio.composeapp.generated.resources.settings_fusion_badge_url_inactive
import provenio.composeapp.generated.resources.settings_fusion_badge_url_label
import provenio.composeapp.generated.resources.settings_fusion_badge_url_status_summary
import provenio.composeapp.generated.resources.settings_fusion_badge_urls_imported
import provenio.composeapp.generated.resources.settings_fusion_badges_empty
import provenio.composeapp.generated.resources.settings_fusion_badges_summary
import provenio.composeapp.generated.resources.settings_stream_badge_position_bottom
import provenio.composeapp.generated.resources.settings_stream_badge_position_description
import provenio.composeapp.generated.resources.settings_stream_badge_position_dialog_description
import provenio.composeapp.generated.resources.settings_stream_badge_position_dialog_title
import provenio.composeapp.generated.resources.settings_stream_badge_position_title
import provenio.composeapp.generated.resources.settings_stream_badge_position_top
import provenio.composeapp.generated.resources.settings_stream_badge_urls_description
import provenio.composeapp.generated.resources.settings_stream_badge_urls_title
import provenio.composeapp.generated.resources.settings_stream_badges_section
import provenio.composeapp.generated.resources.settings_stream_size_badges_description
import provenio.composeapp.generated.resources.settings_stream_size_badges_title
import provenio.composeapp.generated.resources.settings_stream_addon_logo_title
import provenio.composeapp.generated.resources.settings_stream_addon_logo_description
import provenio.composeapp.generated.resources.settings_stream_display_section
import provenio.composeapp.generated.resources.settings_stream_background_title
import provenio.composeapp.generated.resources.settings_stream_background_description
import provenio.composeapp.generated.resources.settings_meta_background_mode_cinematic
import provenio.composeapp.generated.resources.settings_meta_background_mode_normal
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.streamsSettingsContent(isTablet: Boolean) {
    item {
        val currentSettings by remember {
            StreamBadgeSettingsRepository.ensureLoaded()
            StreamBadgeSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        val currentRules = currentSettings.rules
        var showBadgeImportDialog by rememberSaveable { mutableStateOf(false) }
        var showBadgePositionDialog by rememberSaveable { mutableStateOf(false) }
        var showBackgroundDialog by rememberSaveable { mutableStateOf(false) }
        val badgePlacementLabel = streamBadgePlacementLabel(currentSettings.badgePlacement)

        SettingsSection(
            title = stringResource(Res.string.settings_stream_badges_section),
            isTablet = isTablet,
        ) {
            SettingsList {
                switchRow(
                    title = stringResource(Res.string.settings_stream_size_badges_title),
                    description = stringResource(Res.string.settings_stream_size_badges_description),
                    checked = { currentSettings.showFileSizeBadges },
                    onCheckedChange = StreamBadgeSettingsRepository::setShowFileSizeBadges,
                )
                navigationRow(
                    title = stringResource(Res.string.settings_stream_badge_position_title),
                    description = badgePlacementLabel,
                    onClick = { showBadgePositionDialog = true },
                )
                navigationRow(
                    title = stringResource(Res.string.settings_stream_badge_urls_title),
                    description = badgeRulesPreview(currentRules),
                    onClick = { showBadgeImportDialog = true },
                )
            }
        }

        Spacer(
            modifier = Modifier.height(
                if (isTablet) Tokens.Space.s18 else MaterialTheme.provenio.spacing.listGap,
            ),
        )

        SettingsSection(
            title = stringResource(Res.string.settings_stream_display_section),
            isTablet = isTablet,
        ) {
            SettingsList {
                if (!isTablet) {
                    navigationRow(
                        title = stringResource(Res.string.settings_stream_background_title),
                        description = streamBackgroundModeLabel(currentSettings.backgroundMode),
                        onClick = { showBackgroundDialog = true },
                    )
                }
                switchRow(
                    title = stringResource(Res.string.settings_stream_addon_logo_title),
                    description = stringResource(Res.string.settings_stream_addon_logo_description),
                    checked = { currentSettings.showAddonLogo },
                    onCheckedChange = StreamBadgeSettingsRepository::setShowAddonLogo,
                )
            }
        }

        if (showBadgeImportDialog) {
            BadgeUrlManagerDialog(
                currentRules = currentRules,
                onDismiss = { showBadgeImportDialog = false },
            )
        }

        if (showBackgroundDialog && !isTablet) {
            StreamBackgroundModeDialog(
                selectedMode = currentSettings.backgroundMode,
                // The sheet animates itself out and then calls onDismiss, which closes it.
                onModeSelected = StreamBadgeSettingsRepository::setBackgroundMode,
                onDismiss = { showBackgroundDialog = false },
            )
        }

        if (showBadgePositionDialog) {
            StreamBadgePositionDialog(
                selectedPlacement = currentSettings.badgePlacement,
                onPlacementSelected = StreamBadgeSettingsRepository::setBadgePlacement,
                onDismiss = { showBadgePositionDialog = false },
            )
        }
    }
}

@Composable
private fun streamBadgePlacementLabel(placement: StreamBadgePlacement): String =
    when (placement) {
        StreamBadgePlacement.TOP -> stringResource(Res.string.settings_stream_badge_position_top)
        StreamBadgePlacement.BOTTOM -> stringResource(Res.string.settings_stream_badge_position_bottom)
    }

@Composable
private fun streamBackgroundModeLabel(mode: StreamBackgroundMode): String = stringResource(
    when (mode) {
        StreamBackgroundMode.Cinematic -> Res.string.settings_meta_background_mode_cinematic
        StreamBackgroundMode.Normal -> Res.string.settings_meta_background_mode_normal
    },
)

@Composable
private fun StreamBackgroundModeDialog(
    selectedMode: StreamBackgroundMode,
    onModeSelected: (StreamBackgroundMode) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_stream_background_title),
        description = stringResource(Res.string.settings_stream_background_description),
        options = StreamBackgroundMode.entries.map { mode ->
            SingleChoiceOption(value = mode, label = streamBackgroundModeLabel(mode))
        },
        isSelected = { it == selectedMode },
        onSelected = onModeSelected,
        onDismiss = onDismiss,
    )
}

@Composable
private fun badgeRulesPreview(rules: StreamBadgeRules): String {
    val normalizedRules = rules.normalized()
    return if (normalizedRules.hasImport) {
        stringResource(
            Res.string.settings_fusion_badges_summary,
            normalizedRules.imports.size,
            STREAM_BADGE_IMPORT_LIMIT,
            normalizedRules.enabledFilterCount,
        )
    } else {
        stringResource(Res.string.settings_fusion_badges_empty)
    }
}

@Composable
private fun StreamBadgePositionDialog(
    selectedPlacement: StreamBadgePlacement,
    onPlacementSelected: (StreamBadgePlacement) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = stringResource(Res.string.settings_stream_badge_position_dialog_title),
        description = stringResource(Res.string.settings_stream_badge_position_dialog_description),
        options = StreamBadgePlacement.entries.map { placement ->
            SingleChoiceOption(value = placement, label = streamBadgePlacementLabel(placement))
        },
        isSelected = { it == selectedPlacement },
        onSelected = onPlacementSelected,
        onDismiss = onDismiss,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BadgeUrlManagerDialog(
    currentRules: StreamBadgeRules,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.provenio
    val scope = rememberCoroutineScope()
    val imports = currentRules.normalized().imports
    var draftUrl by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isImporting by rememberSaveable { mutableStateOf(false) }
    var previewImport by remember { mutableStateOf<StreamBadgeImport?>(null) }

    ContentDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_stream_badge_urls_title),
        buttons = {
            TextButton(
                enabled = !isImporting,
                onClick = onDismiss,
            ) {
                Text(text = stringResource(Res.string.action_cancel), maxLines = 1)
            }
            TextButton(
                enabled = !isImporting && draftUrl.isNotBlank(),
                onClick = {
                    scope.launch {
                        isImporting = true
                        errorMessage = null
                        when (val result = StreamBadgeSettingsRepository.importStreamBadgeRulesFromUrl(draftUrl)) {
                            is StreamBadgeImportResult.Success -> {
                                draftUrl = ""
                                isImporting = false
                            }
                            is StreamBadgeImportResult.Error -> {
                                errorMessage = result.message
                                isImporting = false
                            }
                        }
                    }
                },
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(tokens.icons.sm), strokeWidth = 2.dp)
                } else {
                    Text(text = stringResource(Res.string.action_import), maxLines = 1)
                }
            }
        },
    ) {
        Text(
            text = stringResource(Res.string.settings_stream_badge_urls_description, STREAM_BADGE_IMPORT_LIMIT),
            style = MaterialTheme.typography.bodyMedium,
                    )
        OutlinedTextField(
            value = draftUrl,
            onValueChange = {
                draftUrl = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.settings_fusion_badge_url_label)) },
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            enabled = !isImporting,
        )
        Text(
            text = stringResource(
                Res.string.settings_fusion_badge_urls_imported,
                imports.size,
                STREAM_BADGE_IMPORT_LIMIT,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (imports.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            ) {
                items(
                    items = imports,
                    key = { import -> import.sourceUrl },
                ) { import ->
                    BadgeUrlRow(
                        import = import,
                        showActiveChoice = imports.size > 1,
                        enabled = !isImporting,
                        onActivate = {
                            StreamBadgeSettingsRepository.setActiveStreamBadgeRulesSource(import.sourceUrl)
                        },
                        onPreview = { previewImport = import },
                        onDelete = {
                            StreamBadgeSettingsRepository.deleteStreamBadgeRulesSource(import.sourceUrl)
                            if (previewImport?.sourceUrl.equals(import.sourceUrl, ignoreCase = true)) {
                                previewImport = null
                            }
                        },
                    )
                }
            }
        }
    }

    previewImport?.let { import ->
        BadgePreviewDialog(
            import = import,
            onDismiss = { previewImport = null },
        )
    }
}

@Composable
private fun BadgeUrlRow(
    import: StreamBadgeImport,
    showActiveChoice: Boolean,
    enabled: Boolean,
    onActivate: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = MaterialTheme.provenio
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (import.isActive) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Tokens.Space.s12, vertical = Tokens.Space.s10),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            ) {
                if (showActiveChoice) {
                    RadioButton(
                        selected = import.isActive,
                        onClick = onActivate,
                        enabled = enabled,
                    )
                }
                Text(
                    text = import.sourceUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap, Alignment.End),
            ) {
                val status = if (import.isActive) {
                    stringResource(Res.string.settings_fusion_badge_url_active)
                } else {
                    stringResource(Res.string.settings_fusion_badge_url_inactive)
                }
                Text(
                    text = stringResource(
                        Res.string.settings_fusion_badge_url_status_summary,
                        status,
                        import.enabledFilterCount,
                        import.groups.size,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    enabled = enabled,
                    onClick = onPreview,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(tokens.icons.sm),
                    )
                    Spacer(modifier = Modifier.width(Tokens.Space.s4))
                    Text(text = stringResource(Res.string.settings_fusion_badge_preview_action), maxLines = 1)
                }
                IconButton(
                    enabled = enabled,
                    onClick = onDelete,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(Res.string.action_delete),
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun BadgePreviewDialog(
    import: StreamBadgeImport,
    onDismiss: () -> Unit,
) {
    val tokens = MaterialTheme.provenio
    val sections = badgePreviewSections(import)
    val badgeCount = sections.sumOf { it.filters.size }

    ContentDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.settings_fusion_badge_preview_title),
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.action_close), maxLines = 1)
            }
        },
    ) {
        Text(
            text = import.sourceUrl,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.settings_fusion_badge_preview_count, badgeCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (sections.isEmpty()) {
            Text(
                text = stringResource(Res.string.settings_fusion_badge_preview_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.railGap),
            ) {
                items(
                    items = sections,
                    key = { section -> section.id },
                ) { section ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.s5),
                            verticalArrangement = Arrangement.spacedBy(Tokens.Space.s5),
                        ) {
                            section.filters.forEach { filter ->
                                StreamBadgeChip(
                                    imageURL = filter.imageURL,
                                    name = filter.name,
                                    tagColor = filter.tagColor,
                                    tagStyle = filter.tagStyle,
                                    borderColor = filter.borderColor,
                                    size = StreamBadgeChipSize.PREVIEW,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class BadgePreviewSection(
    val id: String,
    val title: String,
    val filters: List<StreamBadgeFilter>,
)

@Composable
private fun badgePreviewSections(import: StreamBadgeImport): List<BadgePreviewSection> {
    val filters = import.filters.filter { it.imageURL.isNotBlank() }
    if (filters.isEmpty()) return emptyList()

    val filtersByGroupId = filters.groupBy { it.groupId }
    val usedGroupIds = mutableSetOf<String>()
    val sections = mutableListOf<BadgePreviewSection>()
    import.groups.forEachIndexed { index, group ->
        val groupFilters = filtersByGroupId[group.id].orEmpty()
        if (groupFilters.isNotEmpty()) {
            usedGroupIds += group.id
            val fallbackTitle = stringResource(Res.string.settings_fusion_badge_group_title, index + 1)
            sections += BadgePreviewSection(
                id = group.id.ifBlank { "group-$index" },
                title = group.name.ifBlank { fallbackTitle },
                filters = groupFilters,
            )
        }
    }

    val ungroupedFilters = filters.filter { it.groupId !in usedGroupIds }
    if (ungroupedFilters.isNotEmpty()) {
        sections += BadgePreviewSection(
            id = "other",
            title = stringResource(Res.string.settings_fusion_badge_other_group_title),
            filters = ungroupedFilters,
        )
    }
    return sections
}
