package io.github.dimitrysaf.provenio.shell.screens.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.i18n.localizedMediaTypeLabel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import provenio.composeapp.generated.resources.discover_filters
import provenio.composeapp.generated.resources.discover_type
import provenio.composeapp.generated.resources.library_filter_list_label
import provenio.composeapp.generated.resources.search_sort
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.shell.screens.home.components.PosterGridRow
import io.github.dimitrysaf.provenio.shell.screens.home.components.PosterGridSkeletonRow
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.library_filter_all_types
import provenio.composeapp.generated.resources.library_filter_list
import provenio.composeapp.generated.resources.library_filter_sort
import provenio.composeapp.generated.resources.library_filter_type
import provenio.composeapp.generated.resources.library_sort_added_asc
import provenio.composeapp.generated.resources.library_sort_added_desc
import provenio.composeapp.generated.resources.library_sort_title_asc
import provenio.composeapp.generated.resources.library_sort_title_desc
import provenio.composeapp.generated.resources.library_sort_provider_order
import org.jetbrains.compose.resources.stringResource
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.library.LibraryLayoutMode
import io.github.dimitrysaf.provenio.core.library.LibrarySection
import io.github.dimitrysaf.provenio.core.library.LibrarySortOption
import io.github.dimitrysaf.provenio.core.library.LibrarySourceMode
import io.github.dimitrysaf.provenio.core.library.LibraryVerticalEntry
import io.github.dimitrysaf.provenio.core.library.LibraryVerticalProjection
import io.github.dimitrysaf.provenio.core.library.availableLibrarySortOptions
import io.github.dimitrysaf.provenio.core.library.isRemoteTrackingSource
import io.github.dimitrysaf.provenio.core.library.toMetaPreview


private enum class LibraryFilter {
    List,
    Type,
    Sort,
}

/**
 * The Library's filters, as a button inside its search field: the list and the type to show, for
 * the grid layout, and the order. It opens the filters as a sheet, each opening its own choices,
 * as Search's filters do, and is badged while a type is picked.
 */
@Composable
internal fun LibraryFiltersButton(
    layoutMode: LibraryLayoutMode,
    sourceMode: LibrarySourceMode,
    sortOption: LibrarySortOption,
    verticalProjection: LibraryVerticalProjection,
    enabled: Boolean,
    onSectionSelected: (String) -> Unit,
    onTypeSelected: (String?) -> Unit,
    onSortSelected: (LibrarySortOption) -> Unit,
) {
    var showFilters by remember { mutableStateOf(false) }
    var openFilter by remember { mutableStateOf<LibraryFilter?>(null) }
    val sortOptions = availableLibrarySortOptions(sourceMode)
    val allTypesLabel = stringResource(Res.string.library_filter_all_types)
    val showsList = layoutMode == LibraryLayoutMode.VERTICAL && sourceMode.isRemoteTrackingSource
    val showsType = layoutMode == LibraryLayoutMode.VERTICAL
    val selectedSection = verticalProjection.availableSections
        .firstOrNull { section -> section.type == verticalProjection.selectedSectionKey }
    val label = stringResource(Res.string.discover_filters)

    WithTooltip(label) {
        IconButton(onClick = { showFilters = true }, enabled = enabled) {
            BadgedBox(badge = { if (verticalProjection.selectedType != null) Badge() }) {
                Icon(imageVector = Icons.Rounded.Tune, contentDescription = label)
            }
        }
    }

    if (showFilters) {
        SingleChoiceBottomSheet(
            title = label,
            options = buildList {
                if (showsList) {
                    add(
                        SingleChoiceOption(
                            value = LibraryFilter.List,
                            label = stringResource(Res.string.library_filter_list_label),
                            supportingText = selectedSection?.displayTitle,
                            enabled = verticalProjection.availableSections.size > 1,
                        ),
                    )
                }
                if (showsType) {
                    add(
                        SingleChoiceOption(
                            value = LibraryFilter.Type,
                            label = stringResource(Res.string.discover_type),
                            supportingText = verticalProjection.selectedType?.let(::localizedMediaTypeLabel)
                                ?: allTypesLabel,
                            enabled = verticalProjection.availableTypes.isNotEmpty(),
                        ),
                    )
                }
                add(
                    SingleChoiceOption(
                        value = LibraryFilter.Sort,
                        label = stringResource(Res.string.search_sort),
                        supportingText = librarySortOptionLabel(sortOption),
                        enabled = sortOptions.size > 1,
                    ),
                )
            },
            isSelected = { false },
            onSelected = { filter -> openFilter = filter },
            onDismiss = { showFilters = false },
        )
    }

    when (openFilter) {
        LibraryFilter.List -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.library_filter_list),
            options = verticalProjection.availableSections.map { section ->
                SingleChoiceOption(value = section.type, label = section.displayTitle)
            },
            isSelected = { it == verticalProjection.selectedSectionKey },
            onSelected = onSectionSelected,
            onDismiss = { openFilter = null },
        )

        LibraryFilter.Type -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.library_filter_type),
            options = buildList {
                add(SingleChoiceOption(value = "", label = allTypesLabel))
                verticalProjection.availableTypes.forEach { type ->
                    add(SingleChoiceOption(value = type, label = localizedMediaTypeLabel(type)))
                }
            },
            isSelected = { it == verticalProjection.selectedType.orEmpty() },
            onSelected = { type -> onTypeSelected(type.ifBlank { null }) },
            onDismiss = { openFilter = null },
        )

        LibraryFilter.Sort -> SingleChoiceBottomSheet(
            title = stringResource(Res.string.library_filter_sort),
            options = sortOptions.map { option ->
                SingleChoiceOption(value = option, label = librarySortOptionLabel(option))
            },
            isSelected = { it == sortOption },
            onSelected = onSortSelected,
            onDismiss = { openFilter = null },
        )

        null -> Unit
    }
}

internal fun LazyListScope.libraryVerticalContent(
    projection: LibraryVerticalProjection,
    columns: Int,
    watchedKeys: Set<String>,
    fullyWatchedSeriesKeys: Set<String>,
    onPosterClick: ((LibraryItem) -> Unit)?,
    onPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)?,
) {
    items(
        items = projection.entries.chunked(columns),
        key = { rowEntries ->
            val firstEntry = rowEntries.first()
            "library-vertical:${firstEntry.item.type}:${firstEntry.item.id}"
        },
    ) { rowEntries ->
        PosterGridRow(
            items = rowEntries.map { entry -> entry.item.toMetaPreview() },
            columns = columns,
            modifier = libraryContentTransitionModifier()
                .padding(horizontal = 16.dp),
            watchedKeys = watchedKeys,
            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
            onPosterClick = onPosterClick?.let { callback ->
                { preview -> rowEntries.findEntry(preview)?.item?.let(callback) }
            },
            onPosterLongClick = onPosterLongClick?.let { callback ->
                { preview ->
                    rowEntries.findEntry(preview)?.let { entry -> callback(entry.item, entry.section) }
                }
            },
        )
    }
}

internal fun LazyListScope.libraryVerticalSkeletonItems(columns: Int) {
    items(
        count = 2,
        key = { index -> "library-vertical-skeleton:$index" },
    ) {
        PosterGridSkeletonRow(
            columns = columns,
            modifier = libraryContentTransitionModifier()
                .padding(horizontal = 16.dp),
        )
    }
}

internal fun LazyItemScope.libraryContentTransitionModifier(): Modifier =
    Modifier.animateItem(
        fadeInSpec = tween(durationMillis = 160),
        placementSpec = tween(durationMillis = 180),
        fadeOutSpec = tween(durationMillis = 90),
    )

@Composable
private fun librarySortOptionLabel(option: LibrarySortOption): String =
    when (option) {
        LibrarySortOption.DEFAULT -> stringResource(Res.string.library_sort_provider_order)
        LibrarySortOption.ADDED_DESC -> stringResource(Res.string.library_sort_added_desc)
        LibrarySortOption.ADDED_ASC -> stringResource(Res.string.library_sort_added_asc)
        LibrarySortOption.TITLE_ASC -> stringResource(Res.string.library_sort_title_asc)
        LibrarySortOption.TITLE_DESC -> stringResource(Res.string.library_sort_title_desc)
    }

private fun List<LibraryVerticalEntry>.findEntry(preview: MetaPreview): LibraryVerticalEntry? =
    firstOrNull { entry -> entry.item.id == preview.id && entry.item.type == preview.type }
