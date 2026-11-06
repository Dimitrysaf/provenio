package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.details_view_all_count
import androidx.compose.ui.unit.dp

@Composable
fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showHeader) {
            DetailSectionTitle(title = title)
        }
        content()
    }
}

/**
 * A section's heading.
 *
 * `titleLarge` as published, rather than the same style with its size and weight overwritten: the
 * type scale already answers "how big is a section heading", and it answers it once for the whole
 * app instead of per breakpoint.
 *
 * m3.material.io/styles/typography/type-scale-tokens
 */
@Composable
fun DetailSectionTitle(
    title: String,
    fullWidth: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
}
/** The rows of a Details segmented list, with the list's own gap between them. */
@Composable
internal fun DetailSegmentedColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
        content = content,
    )
}

/**
 * A row of a Details segmented list that does nothing when tapped. Material's segmented list item
 * always takes a click, so this is the plain list item in the same segment shape and colours.
 */
@Composable
internal fun DetailStaticSegment(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ListItem(
        headlineContent = content,
        modifier = modifier.fillMaxWidth().clip(segmentShape(index = index, count = count)),
        leadingContent = leadingContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

/** The shapes a tappable segmented row takes at [index] of [count], as the episode list uses. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun detailSegmentShapes(index: Int, count: Int): ListItemShapes {
    val shape = segmentShape(index = index, count = count)
    return ListItemDefaults.shapes(
        shape = shape,
        selectedShape = RoundedCornerShape(OuterCorner),
        pressedShape = shape,
        focusedShape = shape,
        hoveredShape = shape,
    )
}

/** How many rows a Details list shows before its View all row. */
internal const val DetailListPreviewCount = 5

/**
 * A Details segmented list: the first [previewCount] rows, then a View all row that runs
 * [onViewAll], as the last segment of the same list. Every row shows once [showingAll] is true or
 * there is nowhere to view them all.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun <T> DetailSegmentedList(
    items: List<T>,
    previewCount: Int,
    showingAll: Boolean,
    onViewAll: (() -> Unit)?,
    row: @Composable (item: T, shapes: ListItemShapes) -> Unit,
) {
    val viewAll = onViewAll?.takeIf { !showingAll && items.size > previewCount }
    val shown = if (viewAll != null) items.take(previewCount) else items
    val count = shown.size + if (viewAll != null) 1 else 0
    DetailSegmentedColumn {
        shown.forEachIndexed { index, item -> row(item, detailSegmentShapes(index = index, count = count)) }
        if (viewAll != null) {
            SegmentedListItem(
                onClick = viewAll,
                shapes = detailSegmentShapes(index = count - 1, count = count),
                modifier = Modifier.fillMaxWidth(),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                trailingContent = {
                    Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                },
            ) {
                Text(stringResource(Res.string.details_view_all_count, items.size))
            }
        }
    }
}
