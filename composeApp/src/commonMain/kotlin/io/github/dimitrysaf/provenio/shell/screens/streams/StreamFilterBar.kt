package io.github.dimitrysaf.provenio.shell.screens.streams

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.streams.AddonStreamGroup
import io.github.dimitrysaf.provenio.core.streams.StreamQuality
import io.github.dimitrysaf.provenio.core.streams.StreamSizeOrder
import io.github.dimitrysaf.provenio.core.streams.filteredBy
import io.github.dimitrysaf.provenio.core.streams.quality
import io.github.dimitrysaf.provenio.core.streams.sizeBytes
import io.github.dimitrysaf.provenio.core.streams.sortedBySize
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

// The search text and chosen qualities for one list of streams.
@Stable
internal class StreamFilterState {
    var query by mutableStateOf("")
    var qualities by mutableStateOf(emptySet<StreamQuality>())
    var sizeOrder by mutableStateOf(StreamSizeOrder.DEFAULT)

    fun apply(groups: List<AddonStreamGroup>): List<AddonStreamGroup> =
        groups.filteredBy(streamQueryRegex(query), qualities).sortedBySize(sizeOrder)
}

@Composable
internal fun rememberStreamFilterState(key: Any?): StreamFilterState = remember(key) { StreamFilterState() }

// A fill-less search field over a full-width divider; it filters by regular expression, and its menu by resolution.
@Composable
internal fun StreamFilterBar(state: StreamFilterState, groups: List<AddonStreamGroup>) {
    val qualities = remember(groups) {
        groups.flatMap { it.streams }.map { it.quality() }.toSet().sorted()
    }
    val hasSizes = remember(groups) { groups.any { group -> group.streams.any { it.sizeBytes() != null } } }
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = state.query,
            onValueChange = { state.query = it },
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.streams_search_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { state.query = "" }) {
                            Icon(imageVector = Icons.Rounded.Close, contentDescription = stringResource(Res.string.action_clear))
                        }
                    }
                    if (hasSizes || qualities.size > 1) {
                        StreamFilterMenuButton(
                            state = state,
                            qualities = qualities.takeIf { it.size > 1 }.orEmpty(),
                            showSizeOrder = hasSizes,
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
    }
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
    showSizeOrder: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.streams_filter_title)
    val active = state.qualities.isNotEmpty() || state.sizeOrder != StreamSizeOrder.DEFAULT
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = title,
                tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }
        if (LocalWindowBreakpoint.current.isTwoPane) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (showSizeOrder) {
                    StreamFilterMenuLabel(stringResource(Res.string.streams_sort_title))
                    StreamSizeOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label()) },
                            onClick = { state.sizeOrder = order },
                            leadingIcon = { RadioButton(selected = order == state.sizeOrder, onClick = null) },
                        )
                    }
                }
                if (showSizeOrder && qualities.isNotEmpty()) {
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
                showSizeOrder = showSizeOrder,
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
    showSizeOrder: Boolean,
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
            if (showSizeOrder) {
                item {
                    ListSubheader(
                        text = stringResource(Res.string.streams_sort_title),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(StreamSizeOrder.entries) { order ->
                    val selected = order == state.sizeOrder
                    SelectableListRow(
                        selected = selected,
                        onClick = { state.sizeOrder = order },
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
private fun StreamSizeOrder.label(): String = when (this) {
    StreamSizeOrder.DEFAULT -> stringResource(Res.string.streams_sort_default)
    StreamSizeOrder.LARGEST_FIRST -> stringResource(Res.string.streams_sort_size_desc)
    StreamSizeOrder.SMALLEST_FIRST -> stringResource(Res.string.streams_sort_size_asc)
}

@Composable
private fun StreamQuality.label(): String = when (this) {
    StreamQuality.UHD -> "4K"
    StreamQuality.FHD -> "1080p"
    StreamQuality.HD -> "720p"
    StreamQuality.SD -> "SD"
    StreamQuality.UNKNOWN -> stringResource(Res.string.streams_filter_unknown_quality)
}
