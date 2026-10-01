package io.github.dimitrysaf.provenio.shell.screens.streams

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.dimitrysaf.provenio.shell.components.LocalWindowBreakpoint
import io.github.dimitrysaf.provenio.shell.components.MultiChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
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
                    if (hasSizes) {
                        StreamSizeOrderMenuButton(
                            selected = state.sizeOrder,
                            onSelected = { state.sizeOrder = it },
                        )
                    }
                    if (qualities.size > 1) {
                        StreamQualityMenuButton(
                            qualities = qualities,
                            selected = state.qualities,
                            onSelectionChanged = { state.qualities = it },
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

// Picks any number of resolutions: the app's multiple-choice sheet on phones, a checkbox menu on large windows.
@Composable
private fun StreamQualityMenuButton(
    qualities: List<StreamQuality>,
    selected: Set<StreamQuality>,
    onSelectionChanged: (Set<StreamQuality>) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.streams_filter_quality)
    val allLabel = stringResource(Res.string.streams_filter_all_qualities)
    val labels = qualities.map { it.label() }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.Rounded.FilterList,
                contentDescription = title,
                tint = if (selected.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            )
        }
        if (LocalWindowBreakpoint.current.isTwoPane) {
            // Stays open while ticking, as a multiple choice needs.
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(
                    text = { Text(allLabel) },
                    onClick = { onSelectionChanged(emptySet()) },
                    leadingIcon = { Checkbox(checked = selected.isEmpty(), onCheckedChange = null) },
                )
                qualities.forEachIndexed { index, quality ->
                    val checked = quality in selected
                    DropdownMenuItem(
                        text = { Text(labels[index]) },
                        onClick = { onSelectionChanged(if (checked) selected - quality else selected + quality) },
                        leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                    )
                }
            }
        } else if (open) {
            MultiChoiceBottomSheet(
                title = title,
                options = qualities.mapIndexed { index, quality -> SingleChoiceOption(value = quality, label = labels[index]) },
                selected = selected,
                onSelectionChanged = onSelectionChanged,
                onDismiss = { open = false },
                allLabel = allLabel,
            )
        }
    }
}

// Orders the streams by file size: a menu on large windows, the app's single-choice sheet on phones.
@Composable
private fun StreamSizeOrderMenuButton(
    selected: StreamSizeOrder,
    onSelected: (StreamSizeOrder) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.streams_sort_title)
    val orders = StreamSizeOrder.entries
    val labels = orders.map { it.label() }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Sort,
                contentDescription = title,
                tint = if (selected != StreamSizeOrder.DEFAULT) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
            )
        }
        if (LocalWindowBreakpoint.current.isTwoPane) {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                orders.forEachIndexed { index, order ->
                    DropdownMenuItem(
                        text = { Text(labels[index]) },
                        onClick = {
                            onSelected(order)
                            open = false
                        },
                        leadingIcon = { RadioButton(selected = order == selected, onClick = null) },
                    )
                }
            }
        } else if (open) {
            SingleChoiceBottomSheet(
                title = title,
                options = orders.mapIndexed { index, order -> SingleChoiceOption(value = order, label = labels[index]) },
                isSelected = { it == selected },
                onSelected = onSelected,
                onDismiss = { open = false },
            )
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
