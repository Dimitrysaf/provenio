package io.github.dimitrysaf.provenio.shell.screens.settings

import provenio.composeapp.generated.resources.settings_poster_restore_defaults
import provenio.composeapp.generated.resources.settings_poster_columns_above_automatic
import androidx.compose.foundation.layout.padding
import io.github.dimitrysaf.provenio.shell.components.rememberAutoPosterColumns
import io.github.dimitrysaf.provenio.shell.components.ShelfExpansion
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsUiState
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.core.home.HomeShelfLayout
import io.github.dimitrysaf.provenio.core.settings.MaxCardCount
import io.github.dimitrysaf.provenio.core.settings.MinCardCount
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleRepository
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_homescreen_section_shelves
import provenio.composeapp.generated.resources.action_cancel
import provenio.composeapp.generated.resources.action_save
import provenio.composeapp.generated.resources.settings_poster_columns
import provenio.composeapp.generated.resources.settings_poster_columns_description
import provenio.composeapp.generated.resources.settings_poster_count_automatic
import provenio.composeapp.generated.resources.settings_poster_count_helper
import provenio.composeapp.generated.resources.settings_poster_rows
import provenio.composeapp.generated.resources.settings_poster_rows_description
import provenio.composeapp.generated.resources.settings_poster_section_cards
import provenio.composeapp.generated.resources.settings_poster_card_radius
import provenio.composeapp.generated.resources.settings_poster_card_width
import provenio.composeapp.generated.resources.settings_poster_custom
import provenio.composeapp.generated.resources.settings_poster_hide_labels
import provenio.composeapp.generated.resources.settings_poster_landscape_mode
import provenio.composeapp.generated.resources.settings_poster_radius_classic
import provenio.composeapp.generated.resources.settings_poster_radius_pill
import provenio.composeapp.generated.resources.settings_poster_radius_rounded
import provenio.composeapp.generated.resources.settings_poster_radius_sharp
import provenio.composeapp.generated.resources.settings_poster_radius_subtle
import provenio.composeapp.generated.resources.settings_poster_size_dynamic
import provenio.composeapp.generated.resources.settings_poster_size_dynamic_description
import provenio.composeapp.generated.resources.settings_poster_width_balanced
import provenio.composeapp.generated.resources.settings_poster_width_comfort
import provenio.composeapp.generated.resources.settings_poster_width_compact
import provenio.composeapp.generated.resources.settings_poster_width_dense
import provenio.composeapp.generated.resources.settings_poster_width_large
import org.jetbrains.compose.resources.stringResource

/** The Card styles page: a preview, the card's own properties, then how shelves lay those cards out. */
internal fun LazyListScope.posterCustomizationSettingsContent(
    isTablet: Boolean,
    uiState: PosterCardStyleUiState,
    shelfLayout: HomeShelfLayout,
    shelvesExpandedByDefault: Boolean,
) {
    item {
        HomeLayoutPreview(highlight = HomePreviewSection.Catalogs)
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_poster_section_cards),
            isTablet = isTablet,
        ) {
            PosterCardStyleControls(
                widthDp = uiState.widthDp,
                dynamicSizeEnabled = uiState.dynamicSizeEnabled,
                cardsPerRow = uiState.cardsPerRow,
                shelfRows = uiState.shelfRows,
                cornerRadiusDp = uiState.cornerRadiusDp,
                catalogLandscapeModeEnabled = uiState.catalogLandscapeModeEnabled,
                hideLabelsEnabled = uiState.hideLabelsEnabled,
                onCornerRadiusSelected = PosterCardStyleRepository::setCornerRadiusDp,
                onCatalogLandscapeModeChange = PosterCardStyleRepository::setCatalogLandscapeModeEnabled,
                onHideLabelsChange = PosterCardStyleRepository::setHideLabelsEnabled,
            )
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_homescreen_section_shelves),
            isTablet = isTablet,
        ) {
            HomeShelfSettings(
                shelfLayout = shelfLayout,
                shelvesExpandedByDefault = shelvesExpandedByDefault,
            )
        }
    }
    item {
        TextButton(
            onClick = ::restoreCardStyleDefaults,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            Text(stringResource(Res.string.settings_poster_restore_defaults))
        }
    }
}

/** Puts every setting on the Card styles page back to its default: the card's style and the shelves. */
private fun restoreCardStyleDefaults() {
    val defaults = HomeCatalogSettingsUiState()
    PosterCardStyleRepository.resetToDefaults()
    HomeCatalogSettingsRepository.setShelfLayout(defaults.shelfLayout)
    ShelfExpansion.resetHomeAndDetails()
    HomeCatalogSettingsRepository.setShelvesExpandedByDefault(defaults.shelvesExpandedByDefault)
}

/**
 * The poster card's properties, as rows of one list, under the home screen preview that shows
 * what they add up to. Size and radius each state their current value and open the app's
 * single-choice sheet.
 */
@Composable
private fun PosterCardStyleControls(
    widthDp: Int,
    dynamicSizeEnabled: Boolean,
    cardsPerRow: Int,
    shelfRows: Int,
    cornerRadiusDp: Int,
    catalogLandscapeModeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
    onCornerRadiusSelected: (Int) -> Unit,
    onCatalogLandscapeModeChange: (Boolean) -> Unit,
    onHideLabelsChange: (Boolean) -> Unit,
) {
    // Steps that grow as the cards do, so each one reads as a different size, up to one meant
    // for a TV across the room.
    val sizeOptions = listOf(
        PresetOption(
            label = stringResource(Res.string.settings_poster_size_dynamic),
            value = DynamicSizeValue,
            supporting = stringResource(Res.string.settings_poster_size_dynamic_description),
        ),
        PresetOption(stringResource(Res.string.settings_poster_width_compact), 96),
        PresetOption(stringResource(Res.string.settings_poster_width_dense), 110),
        PresetOption(stringResource(Res.string.settings_poster_width_balanced), 126),
        PresetOption(stringResource(Res.string.settings_poster_width_comfort), 150),
        PresetOption(stringResource(Res.string.settings_poster_width_large), 220),
    )
    val selectedSize = if (dynamicSizeEnabled) DynamicSizeValue else widthDp
    val selectedColumns = if (dynamicSizeEnabled) cardsPerRow else 0
    val automaticLabel = stringResource(Res.string.settings_poster_count_automatic)
    val radiusOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_radius_sharp), 0),
        PresetOption(stringResource(Res.string.settings_poster_radius_subtle), 4),
        PresetOption(stringResource(Res.string.settings_poster_radius_classic), 8),
        PresetOption(stringResource(Res.string.settings_poster_radius_rounded), 12),
        PresetOption(stringResource(Res.string.settings_poster_radius_pill), 16),
    )
    val customLabel = stringResource(Res.string.settings_poster_custom)
    var showSizeSheet by remember { mutableStateOf(false) }
    var showRadiusSheet by remember { mutableStateOf(false) }
    var showColumnsDialog by remember { mutableStateOf(false) }
    var showRowsDialog by remember { mutableStateOf(false) }

    SettingsList {
        navigationRow(
            title = stringResource(Res.string.settings_poster_card_width),
            description = sizeOptions.labelFor(selectedSize, customLabel),
            onClick = { showSizeSheet = true },
        )
        navigationRow(
            title = stringResource(Res.string.settings_poster_columns),
            description = selectedColumns.takeIf { it > 0 }?.toString() ?: automaticLabel,
            onClick = { showColumnsDialog = true },
        )
        navigationRow(
            title = stringResource(Res.string.settings_poster_rows),
            description = shelfRows.takeIf { it > 0 }?.toString() ?: automaticLabel,
            onClick = { showRowsDialog = true },
        )
        navigationRow(
            title = stringResource(Res.string.settings_poster_card_radius),
            description = radiusOptions.labelFor(cornerRadiusDp, customLabel),
            onClick = { showRadiusSheet = true },
        )
        switchRow(
            title = stringResource(Res.string.settings_poster_landscape_mode),
            checked = { catalogLandscapeModeEnabled },
            onCheckedChange = onCatalogLandscapeModeChange,
        )
        switchRow(
            title = stringResource(Res.string.settings_poster_hide_labels),
            checked = { hideLabelsEnabled },
            onCheckedChange = onHideLabelsChange,
        )
    }

    if (showSizeSheet) {
        PresetChoiceSheet(
            title = stringResource(Res.string.settings_poster_card_width),
            options = sizeOptions,
            selectedValue = selectedSize,
            onSelected = { value ->
                if (value == DynamicSizeValue) {
                    PosterCardStyleRepository.setDynamicSizeEnabled(true)
                } else {
                    PosterCardStyleRepository.setWidthDp(value)
                }
            },
            onDismiss = { showSizeSheet = false },
        )
    }

    if (showColumnsDialog) {
        CardCountDialog(
            title = stringResource(Res.string.settings_poster_columns),
            description = stringResource(Res.string.settings_poster_columns_description),
            value = selectedColumns,
            recommendedMax = rememberAutoPosterColumns(),
            onConfirm = { count ->
                PosterCardStyleRepository.setCardsPerRow(count)
                showColumnsDialog = false
            },
            onDismiss = { showColumnsDialog = false },
        )
    }

    if (showRowsDialog) {
        CardCountDialog(
            title = stringResource(Res.string.settings_poster_rows),
            description = stringResource(Res.string.settings_poster_rows_description),
            value = shelfRows,
            onConfirm = { count ->
                PosterCardStyleRepository.setShelfRows(count)
                showRowsDialog = false
            },
            onDismiss = { showRowsDialog = false },
        )
    }

    if (showRadiusSheet) {
        PresetChoiceSheet(
            title = stringResource(Res.string.settings_poster_card_radius),
            options = radiusOptions,
            selectedValue = cornerRadiusDp,
            onSelected = onCornerRadiusSelected,
            onDismiss = { showRadiusSheet = false },
        )
    }
}

/** Stands for dynamic sizing among the size presets, which are otherwise widths. */
private const val DynamicSizeValue = -1

/**
 * A dialog for a number of columns or rows. The field takes digits only; on save, a number outside
 * [MinCardCount]..[MaxCardCount] snaps to the nearest end, and an empty field means automatic (0),
 * sized to the screen.
 */
@Composable
private fun CardCountDialog(
    title: String,
    description: String,
    value: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    recommendedMax: Int? = null,
) {
    var draft by remember { mutableStateOf(value.takeIf { it > 0 }?.toString().orEmpty()) }
    val focusRequester = remember { FocusRequester() }
    val confirm = { onConfirm(snapCardCount(draft)) }
    val snapped = snapCardCount(draft)
    val aboveRecommended = recommendedMax != null && snapped > recommendedMax

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = draft,
                    onValueChange = { input -> draft = input.filter(Char::isDigit).take(MaxCardCountDigits) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = { Text(stringResource(Res.string.settings_poster_count_automatic)) },
                    isError = aboveRecommended,
                    supportingText = {
                        if (aboveRecommended && recommendedMax != null) {
                            Text(stringResource(Res.string.settings_poster_columns_above_automatic, recommendedMax))
                        } else {
                            Text(stringResource(Res.string.settings_poster_count_helper, MinCardCount, MaxCardCount))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = confirm) {
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

/** The typed count snapped into [MinCardCount]..[MaxCardCount], or 0 (automatic) for an empty field. */
private fun snapCardCount(text: String): Int {
    val digits = text.trim().trimStart('0')
    if (text.isBlank()) return 0
    if (digits.isEmpty()) return MinCardCount
    if (digits.length > MaxCardCountDigits) return MaxCardCount
    return digits.toInt().coerceIn(MinCardCount, MaxCardCount)
}

/** Digits enough for any count up to [MaxCardCount] and one more, so an overlong entry still snaps down. */
private val MaxCardCountDigits = MaxCardCount.toString().length + 1

/** The label of whichever preset holds this value, or the word for one that no preset covers. */
private fun List<PresetOption>.labelFor(value: Int, customLabel: String): String =
    firstOrNull { it.value == value }?.label ?: customLabel

/** One set of presets, in the app's single-choice sheet. */
@Composable
private fun PresetChoiceSheet(
    title: String,
    options: List<PresetOption>,
    selectedValue: Int,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = title,
        options = options.map {
            SingleChoiceOption(value = it.value, label = it.label, supportingText = it.supporting)
        },
        isSelected = { it == selectedValue },
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}

private data class PresetOption(
    val label: String,
    val value: Int,
    val supporting: String? = null,
)
