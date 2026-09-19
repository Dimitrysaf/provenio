package com.nuvio.app.features.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.SingleChoiceBottomSheet
import com.nuvio.app.core.ui.SingleChoiceOption
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.landscapePosterHeightForWidth
import com.nuvio.app.core.ui.landscapePosterWidth
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_reset
import nuvio.composeapp.generated.resources.settings_poster_card_radius
import nuvio.composeapp.generated.resources.settings_poster_card_width
import nuvio.composeapp.generated.resources.settings_poster_custom
import nuvio.composeapp.generated.resources.settings_poster_hide_labels
import nuvio.composeapp.generated.resources.settings_poster_landscape_mode
import nuvio.composeapp.generated.resources.settings_poster_preview_label
import nuvio.composeapp.generated.resources.settings_poster_radius_classic
import nuvio.composeapp.generated.resources.settings_poster_radius_pill
import nuvio.composeapp.generated.resources.settings_poster_radius_rounded
import nuvio.composeapp.generated.resources.settings_poster_radius_sharp
import nuvio.composeapp.generated.resources.settings_poster_radius_subtle
import nuvio.composeapp.generated.resources.settings_poster_width_balanced
import nuvio.composeapp.generated.resources.settings_poster_width_comfort
import nuvio.composeapp.generated.resources.settings_poster_width_compact
import nuvio.composeapp.generated.resources.settings_poster_width_dense
import nuvio.composeapp.generated.resources.settings_poster_width_large
import nuvio.composeapp.generated.resources.settings_poster_width_standard
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.posterCustomizationSettingsContent(
    uiState: PosterCardStyleUiState,
) {
    item {
        PosterCardStyleControls(
            widthDp = uiState.widthDp,
            cornerRadiusDp = uiState.cornerRadiusDp,
            catalogLandscapeModeEnabled = uiState.catalogLandscapeModeEnabled,
            hideLabelsEnabled = uiState.hideLabelsEnabled,
            onWidthSelected = PosterCardStyleRepository::setWidthDp,
            onCornerRadiusSelected = PosterCardStyleRepository::setCornerRadiusDp,
            onCatalogLandscapeModeChange = PosterCardStyleRepository::setCatalogLandscapeModeEnabled,
            onHideLabelsChange = PosterCardStyleRepository::setHideLabelsEnabled,
        )
    }
}

/**
 * The poster card's properties, as rows of one list.
 *
 * The preview leads the section the way a collection's backdrop leads its editor, except nothing
 * here is edited by tapping it: every property that shapes it is a row underneath, so the preview
 * only ever reports. Width and radius were rows of chips, six and five of them wrapping onto
 * three lines; as a row each states its current value and opens the app's single-choice sheet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PosterCardStyleControls(
    widthDp: Int,
    cornerRadiusDp: Int,
    catalogLandscapeModeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
    onWidthSelected: (Int) -> Unit,
    onCornerRadiusSelected: (Int) -> Unit,
    onCatalogLandscapeModeChange: (Boolean) -> Unit,
    onHideLabelsChange: (Boolean) -> Unit,
) {
    val widthOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_width_compact), 104),
        PresetOption(stringResource(Res.string.settings_poster_width_dense), 112),
        PresetOption(stringResource(Res.string.settings_poster_width_standard), 120),
        PresetOption(stringResource(Res.string.settings_poster_width_balanced), 126),
        PresetOption(stringResource(Res.string.settings_poster_width_comfort), 134),
        PresetOption(stringResource(Res.string.settings_poster_width_large), 140),
    )
    val radiusOptions = listOf(
        PresetOption(stringResource(Res.string.settings_poster_radius_sharp), 0),
        PresetOption(stringResource(Res.string.settings_poster_radius_subtle), 4),
        PresetOption(stringResource(Res.string.settings_poster_radius_classic), 8),
        PresetOption(stringResource(Res.string.settings_poster_radius_rounded), 12),
        PresetOption(stringResource(Res.string.settings_poster_radius_pill), 16),
    )
    val customLabel = stringResource(Res.string.settings_poster_custom)
    var showWidthSheet by remember { mutableStateOf(false) }
    var showRadiusSheet by remember { mutableStateOf(false) }

    SettingsList {
        customRow {
            PosterCardStylePreviewRow(
                widthDp = widthDp,
                cornerRadiusDp = cornerRadiusDp,
                landscapeEnabled = catalogLandscapeModeEnabled,
                hideLabelsEnabled = hideLabelsEnabled,
                shape = segmentShape(index = 0, count = PosterCardStyleRowCount),
            )
        }
        navigationRow(
            title = stringResource(Res.string.settings_poster_card_width),
            description = widthOptions.labelFor(widthDp, customLabel),
            onClick = { showWidthSheet = true },
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

    if (showWidthSheet) {
        PresetChoiceSheet(
            title = stringResource(Res.string.settings_poster_card_width),
            options = widthOptions,
            selectedValue = widthDp,
            onSelected = onWidthSelected,
            onDismiss = { showWidthSheet = false },
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
        options = options.map { SingleChoiceOption(value = it.value, label = it.label) },
        isSelected = { it == selectedValue },
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}

/** The live preview, as a row of the list rather than a block floating above it. */
@Composable
private fun PosterCardStylePreviewRow(
    widthDp: Int,
    cornerRadiusDp: Int,
    landscapeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
    shape: Shape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            PosterCardLivePreview(
                widthDp = widthDp,
                cornerRadiusDp = cornerRadiusDp,
                landscapeEnabled = landscapeEnabled,
                hideLabelsEnabled = hideLabelsEnabled,
            )
        }
    }
}

/**
 * A miniature of the card the settings describe: the same width, the same corners, the shape the
 * landscape switch gives it, and the label the hide-labels switch takes away. It carries a
 * placeholder image rather than artwork, since nothing here is about a particular title.
 */
@Composable
private fun PosterCardLivePreview(
    widthDp: Int,
    cornerRadiusDp: Int,
    landscapeEnabled: Boolean,
    hideLabelsEnabled: Boolean,
) {
    // The catalog derives a landscape card's width and height from the same width setting, so the
    // preview derives them the same way instead of keeping its own numbers.
    val cardWidth = if (landscapeEnabled) landscapePosterWidth(widthDp) else widthDp.dp
    val cardHeight = if (landscapeEnabled) {
        landscapePosterHeightForWidth(cardWidth)
    } else {
        (widthDp * 3 / 2).dp
    }
    val animatedWidth by animateDpAsState(
        targetValue = cardWidth,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewWidth",
    )
    val animatedHeight by animateDpAsState(
        targetValue = cardHeight,
        animationSpec = tween(durationMillis = 280),
        label = "posterPreviewHeight",
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = cornerRadiusDp.dp,
        animationSpec = tween(durationMillis = 220),
        label = "posterPreviewCornerRadius",
    )
    val cardShape = RoundedCornerShape(animatedCornerRadius)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(animatedWidth)
                .height(animatedHeight)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = cardShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }

        if (!hideLabelsEnabled) {
            Text(
                text = stringResource(Res.string.settings_poster_preview_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                // The label sits under the card and to its edge, the way a catalog tile's does.
                modifier = Modifier.width(animatedWidth),
            )
        }
    }
}

private const val PosterCardStyleRowCount = 5

private data class PresetOption(
    val label: String,
    val value: Int,
)
