package io.github.dimitrysaf.provenio.shell.screens.streams

import io.github.dimitrysaf.provenio.shell.components.SearchField
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.streams.AddonStreamGroup
import io.github.dimitrysaf.provenio.core.streams.StreamQuality
import io.github.dimitrysaf.provenio.core.streams.StreamSortOrder
import io.github.dimitrysaf.provenio.core.streams.filteredBy
import io.github.dimitrysaf.provenio.core.streams.quality
import io.github.dimitrysaf.provenio.core.streams.seeds
import io.github.dimitrysaf.provenio.core.streams.sizeBytes
import io.github.dimitrysaf.provenio.core.streams.sortedByOrder
import io.github.dimitrysaf.provenio.core.streams.streamQueryRegex
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.LocalWindowBreakpoint
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SelectableListRow
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** The chosen qualities and order, kept from one list of streams to the next until changed. */
private object RememberedStreamFilter {
    var qualities by mutableStateOf(emptySet<StreamQuality>())
    var sortOrder by mutableStateOf(StreamSortOrder.DEFAULT)
}

/**
 * The search text for one list of streams, with the qualities and order chosen last time.
 * Chosen qualities a list does not have are left out for it, so they never hide every stream.
 */
@Stable
internal class StreamFilterState {
    var query by mutableStateOf("")

    var qualities: Set<StreamQuality>
        get() = RememberedStreamFilter.qualities
        set(value) {
            RememberedStreamFilter.qualities = value
        }

    var sortOrder: StreamSortOrder
        get() = RememberedStreamFilter.sortOrder
        set(value) {
            RememberedStreamFilter.sortOrder = value
        }

    fun apply(groups: List<AddonStreamGroup>): List<AddonStreamGroup> {
        val available = groups.flatMap { it.streams }.map { it.quality() }.toSet()
        return groups.filteredBy(streamQueryRegex(query), qualities intersect available).sortedByOrder(sortOrder)
    }
}

@Composable
internal fun rememberStreamFilterState(key: Any?): StreamFilterState = remember(key) { StreamFilterState() }

// The app's search field over a divider; it filters by regular expression, and its menu by resolution.
@Composable
internal fun StreamFilterBar(state: StreamFilterState, groups: List<AddonStreamGroup>) {
    val qualities = remember(groups) {
        groups.flatMap { it.streams }.map { it.quality() }.toSet().sorted()
    }
    // Each way of sorting is offered only when some stream says the size or seeds it sorts by.
    val orders = remember(groups) {
        val streams = groups.flatMap { it.streams }
        buildList {
            add(StreamSortOrder.DEFAULT)
            if (streams.any { it.sizeBytes() != null }) {
                add(StreamSortOrder.LARGEST_FIRST)
                add(StreamSortOrder.SMALLEST_FIRST)
            }
            if (streams.any { it.seeds() != null }) {
                add(StreamSortOrder.MOST_SEEDS)
                add(StreamSortOrder.FEWEST_SEEDS)
            }
        }.takeIf { it.size > 1 }.orEmpty()
    }
    SearchField(
        query = state.query,
        onQueryChange = { state.query = it },
        placeholder = stringResource(Res.string.streams_search_placeholder),
        divided = true,
        actions = if (orders.isNotEmpty() || qualities.size > 1) {
            {
                StreamFilterMenuButton(
                    state = state,
                    qualities = qualities.takeIf { it.size > 1 }.orEmpty(),
                    orders = orders,
                )
            }
        } else {
            null
        },
    )
}

// Shown in place of the list when streams exist but none match the search or qualities.
@Composable
internal fun StreamsNoMatchesBlock() {
    Text(
        text = stringResource(Res.string.streams_filter_no_matches),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = StreamsHorizontalPadding, vertical = 16.dp),
    )
}

@Composable
private fun StreamFilterMenuButton(
    state: StreamFilterState,
    qualities: List<StreamQuality>,
    orders: List<StreamSortOrder>,
) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.streams_filter_title)
    // An order this list cannot sort by leaves it as it came, so it shows as the default.
    val shownOrder = state.sortOrder.takeIf { it in orders } ?: StreamSortOrder.DEFAULT
    val active = state.qualities.isNotEmpty() || shownOrder != StreamSortOrder.DEFAULT
    Box {
        WithTooltip(title) {
            IconButton(onClick = { open = true }) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = title,
                    tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                )
            }
        }
        if (LocalWindowBreakpoint.current.isTwoPane) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (orders.isNotEmpty()) {
                    StreamFilterMenuLabel(stringResource(Res.string.streams_sort_title))
                    orders.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label()) },
                            onClick = { state.sortOrder = order },
                            leadingIcon = { RadioButton(selected = order == shownOrder, onClick = null) },
                        )
                    }
                }
                if (orders.isNotEmpty() && qualities.isNotEmpty()) {
                    HorizontalDivider()
                }
                if (qualities.isNotEmpty()) {
                    StreamFilterMenuLabel(stringResource(Res.string.streams_filter_quality))
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.streams_filter_all_qualities)) },
                        onClick = { state.qualities = emptySet() },
                        leadingIcon = { Checkbox(checked = state.qualities.isEmpty(), onCheckedChange = null) },
                    )
                    qualities.forEach { quality ->
                        val checked = quality in state.qualities
                        DropdownMenuItem(
                            text = { Text(quality.label()) },
                            onClick = {
                                state.qualities = if (checked) state.qualities - quality else state.qualities + quality
                            },
                            leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                        )
                    }
                }
            }
        } else if (open) {
            StreamFilterSheet(
                title = title,
                state = state,
                qualities = qualities,
                orders = orders,
                shownOrder = shownOrder,
                onDismiss = { open = false },
            )
        }
    }
}

@Composable
private fun StreamFilterMenuLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreamFilterSheet(
    title: String,
    state: StreamFilterState,
    qualities: List<StreamQuality>,
    orders: List<StreamSortOrder>,
    shownOrder: StreamSortOrder,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = {
        scope.launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) }
    }
    ModalSheet(
        onDismissRequest = dismiss,
        sheetState = sheetState,
    ) {
        SheetHeader(
            title = title,
            navigation = SheetNavigation.Back,
            onNavigate = dismiss,
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = BottomSheetBodyMargin),
        ) {
            if (orders.isNotEmpty()) {
                item {
                    ListSubheader(
                        text = stringResource(Res.string.streams_sort_title),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(orders) { order ->
                    val selected = order == shownOrder
                    SelectableListRow(
                        selected = selected,
                        onClick = { state.sortOrder = order },
                        headline = order.label(),
                        trailingContent = {
                            RadioButton(selected = selected, onClick = null)
                        },
                    )
                }
            }
            if (qualities.isNotEmpty()) {
                item {
                    ListSubheader(
                        text = stringResource(Res.string.streams_filter_quality),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item {
                    val allSelected = state.qualities.isEmpty()
                    SelectableListRow(
                        selected = allSelected,
                        onClick = { state.qualities = emptySet() },
                        headline = stringResource(Res.string.streams_filter_all_qualities),
                        trailingContent = {
                            Checkbox(checked = allSelected, onCheckedChange = null)
                        },
                    )
                }
                items(qualities) { quality ->
                    val checked = quality in state.qualities
                    SelectableListRow(
                        selected = checked,
                        onClick = {
                            state.qualities = if (checked) state.qualities - quality else state.qualities + quality
                        },
                        headline = quality.label(),
                        trailingContent = {
                            Checkbox(checked = checked, onCheckedChange = null)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamSortOrder.label(): String = when (this) {
    StreamSortOrder.DEFAULT -> stringResource(Res.string.streams_sort_default)
    StreamSortOrder.LARGEST_FIRST -> stringResource(Res.string.streams_sort_size_desc)
    StreamSortOrder.SMALLEST_FIRST -> stringResource(Res.string.streams_sort_size_asc)
    StreamSortOrder.MOST_SEEDS -> stringResource(Res.string.streams_sort_seeds_desc)
    StreamSortOrder.FEWEST_SEEDS -> stringResource(Res.string.streams_sort_seeds_asc)
}

@Composable
private fun StreamQuality.label(): String = when (this) {
    StreamQuality.UHD -> "4K"
    StreamQuality.FHD -> "1080p"
    StreamQuality.HD -> "720p"
    StreamQuality.SD -> "SD"
    StreamQuality.UNKNOWN -> stringResource(Res.string.streams_filter_unknown_quality)
}
