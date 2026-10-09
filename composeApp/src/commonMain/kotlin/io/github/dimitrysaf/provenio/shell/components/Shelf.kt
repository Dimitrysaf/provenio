package io.github.dimitrysaf.provenio.shell.components

import kotlin.math.ceil
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.home_view_all
import provenio.composeapp.generated.resources.shelf_collapse
import provenio.composeapp.generated.resources.shelf_expand
import org.jetbrains.compose.resources.stringResource

enum class ViewAllPillSize {
    Default,
    Compact,
}

/**
 * A titled shelf of cards that scrolls sideways. With [rows] above 1 the shelf is blocks of
 * [columns] by [rows] cards, side by side, each filled a row at a time from the left like a grid;
 * [columns] at 0 makes a single block that spreads the cards evenly over the rows. With
 * [snapToItems] a fling settles on the start of a column rather than partway through one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> ShelfSection(
    title: String,
    entries: List<T>,
    modifier: Modifier = Modifier,
    headerHorizontalPadding: Dp = 0.dp,
    rowContentPadding: PaddingValues = PaddingValues(0.dp),
    rowModifier: Modifier = Modifier,
    itemSpacing: Dp = 10.dp,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: ViewAllPillSize = ViewAllPillSize.Default,
    key: ((T) -> Any)? = null,
    animatePlacement: Boolean = false,
    state: LazyListState = rememberLazyListState(),
    wheelScrollsRow: Boolean = false,
    rows: Int = 1,
    columns: Int = 0,
    snapToItems: Boolean = false,
    itemContent: @Composable (T) -> Unit,
) {
    ScreenActivityEffect(state) { active ->
        if (!active) state.stopScroll()
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title.isNotBlank()) {
            ShelfSectionHeader(
                title = title,
                horizontalPadding = headerHorizontalPadding,
                onViewAllClick = onViewAllClick,
                viewAllPillSize = viewAllPillSize,
            )
        }
        LazyRow(
            // The mouse wheel scrolls the page; only a row that asks for it takes the wheel.
            modifier = if (wheelScrollsRow) rowModifier.horizontalWheelScroll(state) else rowModifier,
            state = state,
            contentPadding = rowContentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            flingBehavior = if (snapToItems) {
                rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Start)
            } else {
                ScrollableDefaults.flingBehavior()
            },
        ) {
            if (rows > 1) {
                val shelfColumns: List<Pair<Any, List<T>>> = if (key != null) {
                    shelfBlockColumns(entries.withDuplicateSafeLazyKeys(key), rows, columns).map { column ->
                        column.first().lazyKey to column.map { it.value }
                    }
                } else {
                    shelfBlockColumns(entries, rows, columns).mapIndexed { index, column -> index to column }
                }
                items(
                    items = shelfColumns,
                    key = if (key != null) { column: Pair<Any, List<T>> -> column.first } else null,
                ) { (_, column) ->
                    Column(
                        modifier = if (animatePlacement) Modifier.animateItem() else Modifier,
                        verticalArrangement = Arrangement.spacedBy(itemSpacing),
                    ) {
                        column.forEach { entry -> itemContent(entry) }
                    }
                }
            } else if (key != null) {
                items(
                    items = entries.withDuplicateSafeLazyKeys(key),
                    key = { entry -> entry.lazyKey },
                ) { keyedEntry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(keyedEntry.value) }
                    } else {
                        itemContent(keyedEntry.value)
                    }
                }
            } else {
                items(entries) { entry ->
                    if (animatePlacement) {
                        Box(modifier = Modifier.animateItem()) { itemContent(entry) }
                    } else {
                        itemContent(entry)
                    }
                }
            }
        }
    }
}

/**
 * The columns of a shelf laid out as blocks of [columns] by [rows] entries, each block filled a row
 * at a time: the first [columns] entries are its top row, the next [columns] the row below. A last
 * block that is not full keeps the same rows, so its columns may be shorter. [columns] at 0 makes a
 * single block just wide enough for every entry.
 */
private fun <E> shelfBlockColumns(entries: List<E>, rows: Int, columns: Int): List<List<E>> {
    if (entries.isEmpty()) return emptyList()
    val blockColumns = if (columns > 0) columns else ceil(entries.size / rows.toDouble()).toInt()
    return entries.chunked(blockColumns * rows).flatMap { block ->
        val width = minOf(blockColumns, block.size)
        List(width) { column ->
            (0 until rows).map { row -> row * blockColumns + column }.filter { it < block.size }.map { block[it] }
        }
    }
}

/**
 * A shelf's heading, and the way into the rest of it.
 *
 * The whole row is the target rather than a 40dp pill at its end: the title says where the row
 * goes, so it is part of the same gesture. The chevron is left as a plain icon, with no container
 * of its own, because the row it sits in is already the button.
 */
@Composable
private fun ShelfSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    onViewAllClick: (() -> Unit)? = null,
    viewAllPillSize: ViewAllPillSize = ViewAllPillSize.Default,
) {
    val viewAllText = stringResource(Res.string.home_view_all)
    val iconSize = if (viewAllPillSize == ViewAllPillSize.Compact) {
        16.dp
    } else {
        20.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onViewAllClick != null) {
                    Modifier.clickable(onClickLabel = viewAllText, onClick = onViewAllClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onViewAllClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * A grid shelf's heading. The title row opens and closes the shelf, with the chevron turning the
 * way the details table under Play does; View All sits apart at the end, since the title no longer
 * leads into the full catalog.
 */
@Composable
fun CollapsibleShelfHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    onViewAllClick: (() -> Unit)? = null,
    viewAllLabel: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "shelfChevronRotation",
    )
    val toggleLabel = stringResource(if (expanded) Res.string.shelf_collapse else Res.string.shelf_expand)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke()
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = toggleLabel, onClick = onToggle)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = toggleLabel,
                modifier = Modifier.rotate(chevronRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onViewAllClick != null) {
            TextButton(onClick = onViewAllClick) {
                Text(viewAllLabel ?: stringResource(Res.string.home_view_all))
            }
        }
    }
}
