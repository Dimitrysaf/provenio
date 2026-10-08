package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.HomeShelfLayout
import io.github.dimitrysaf.provenio.shell.components.SelectableListRow
import io.github.dimitrysaf.provenio.shell.components.ShelfExpansion
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * How the shelves look: a grid that wraps, or a row that scrolls sideways, chosen by looking at
 * each, the way the Continue Watching card style is. Whether grid shelves start open only means
 * something for the grid, so its switch shows only then.
 */
@Composable
internal fun HomeShelfSettings(
    shelfLayout: HomeShelfLayout,
    shelvesExpandedByDefault: Boolean,
) {
    val layouts = HomeShelfLayout.entries
    Column(verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace)) {
        layouts.forEachIndexed { index, layout ->
            SelectableListRow(
                selected = layout == shelfLayout,
                onClick = { HomeCatalogSettingsRepository.setShelfLayout(layout) },
                headline = stringResource(layout.labelRes),
                supporting = stringResource(layout.descriptionRes),
                unselectedShape = segmentShape(index = index, count = layouts.size),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                leadingContent = {
                    ShelfLayoutMiniature(
                        layout = layout,
                        modifier = Modifier.size(width = 100.dp, height = 72.dp),
                    )
                },
            )
        }
    }
    if (shelfLayout == HomeShelfLayout.Grid) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsList {
            switchRow(
                title = stringResource(Res.string.settings_shelf_expanded_by_default),
                description = stringResource(Res.string.settings_shelf_expanded_by_default_description),
                checked = { shelvesExpandedByDefault },
                onCheckedChange = { expanded ->
                    ShelfExpansion.resetHomeAndDetails()
                    HomeCatalogSettingsRepository.setShelvesExpandedByDefault(expanded)
                },
            )
        }
    }
}

/**
 * A shelf drawn small: its title bar, then posters wrapping into rows for the grid, or one row
 * running off the right edge for horizontal.
 */
@Composable
private fun ShelfLayoutMiniature(
    layout: HomeShelfLayout,
    modifier: Modifier = Modifier,
) {
    val posterColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
    val titleColor = MaterialTheme.colorScheme.outlineVariant
    val posterShape = RoundedCornerShape(3.dp)
    val poster = Modifier.size(width = 16.dp, height = 24.dp)

    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.CenterStart) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(titleColor),
            )
            val rows = if (layout == HomeShelfLayout.Grid) 2 else 1
            repeat(rows) {
                Row(
                    modifier = if (layout == HomeShelfLayout.Horizontal) {
                        Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true)
                    } else {
                        Modifier
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(if (layout == HomeShelfLayout.Grid) 5 else 7) {
                        Box(modifier = poster.clip(posterShape).background(posterColor))
                    }
                }
            }
        }
    }
}

private val HomeShelfLayout.labelRes: StringResource
    get() = when (this) {
        HomeShelfLayout.Grid -> Res.string.settings_shelf_layout_grid
        HomeShelfLayout.Horizontal -> Res.string.settings_shelf_layout_horizontal
    }

private val HomeShelfLayout.descriptionRes: StringResource
    get() = when (this) {
        HomeShelfLayout.Grid -> Res.string.settings_shelf_layout_grid_description
        HomeShelfLayout.Horizontal -> Res.string.settings_shelf_layout_horizontal_description
    }
