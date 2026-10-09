package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import io.github.dimitrysaf.provenio.core.settings.MaxCardsPerRow
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleRepository
import io.github.dimitrysaf.provenio.core.settings.PosterCardStyleUiState
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_reset
import provenio.composeapp.generated.resources.settings_homescreen_section_shelves
import provenio.composeapp.generated.resources.settings_poster_cards_per_row
import provenio.composeapp.generated.resources.settings_poster_cards_per_row_auto
import provenio.composeapp.generated.resources.settings_poster_cards_per_row_auto_description
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
    val cardsPerRowOptions = listOf(
        PresetOption(
            label = stringResource(Res.string.settings_poster_cards_per_row_auto),
            value = 0,
            supporting = stringResource(Res.string.settings_poster_cards_per_row_auto_description),
        ),
    ) + (MinCardsPerRowOption..MaxCardsPerRowOption).map { PresetOption(it.toString(), it) }
    val selectedCardsPerRow = if (dynamicSizeEnabled) cardsPerRow else 0
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
    var showCardsPerRowSheet by remember { mutableStateOf(false) }

    SettingsList {
        navigationRow(
            title = stringResource(Res.string.settings_poster_card_width),
            description = sizeOptions.labelFor(selectedSize, customLabel),
            onClick = { showSizeSheet = true },
        )
        navigationRow(
            title = stringResource(Res.string.settings_poster_cards_per_row),
            description = cardsPerRowOptions.labelFor(selectedCardsPerRow, selectedCardsPerRow.toString()),
            onClick = { showCardsPerRowSheet = true },
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

    Spacer(modifier = Modifier.height(8.dp))

    // Resetting acts on every property above, so it follows them rather than sitting in the
    // heading where it competes with the section's name.
    TextButton(
        onClick = PosterCardStyleRepository::resetToDefaults,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.action_reset))
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

    if (showCardsPerRowSheet) {
        PresetChoiceSheet(
            title = stringResource(Res.string.settings_poster_cards_per_row),
            options = cardsPerRowOptions,
            selectedValue = selectedCardsPerRow,
            onSelected = PosterCardStyleRepository::setCardsPerRow,
            onDismiss = { showCardsPerRowSheet = false },
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

private const val MinCardsPerRowOption = 2

private val MaxCardsPerRowOption = minOf(10, MaxCardsPerRow)

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
