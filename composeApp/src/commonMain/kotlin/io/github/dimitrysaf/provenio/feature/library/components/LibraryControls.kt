package io.github.dimitrysaf.provenio.feature.library.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.designsystem.components.PickerChip
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.library_all_types
import io.github.dimitrysaf.provenio.resources.library_list_completed
import io.github.dimitrysaf.provenio.resources.library_list_plan_to_watch
import io.github.dimitrysaf.provenio.resources.library_list_watching
import io.github.dimitrysaf.provenio.resources.library_select_list
import io.github.dimitrysaf.provenio.resources.library_select_type
import io.github.dimitrysaf.provenio.resources.library_sort_action
import io.github.dimitrysaf.provenio.resources.library_sort_least_recently_watched
import io.github.dimitrysaf.provenio.resources.library_sort_recently_watched
import io.github.dimitrysaf.provenio.resources.library_sort_title_ascending
import io.github.dimitrysaf.provenio.resources.library_sort_title_descending
import io.github.dimitrysaf.provenio.resources.library_type_anime
import io.github.dimitrysaf.provenio.resources.library_type_movies
import io.github.dimitrysaf.provenio.resources.library_type_series
import io.github.dimitrysaf.provenio.simkl.SimklStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The value the type picker carries for "every type".
 *
 * A sentinel, not a label: it is compared against and passed around, so it must not change
 * with the language. [mediaTypeLabel] is what turns it into something readable.
 */
const val AllTypes = "all-types"

/**
 * How the library is ordered.
 *
 * Simkl records when a title was last watched but not when it was added, so these sort by
 * the timestamp that exists rather than offering an "added" order the data cannot answer.
 * Titles sort case-insensitively, otherwise every lowercase title lands after every
 * uppercase one.
 */
enum class LibrarySort(val label: StringResource, val comparator: Comparator<SimklItem>) {
    RecentlyWatched(
        Res.string.library_sort_recently_watched,
        compareByDescending<SimklItem> { it.lastWatchedAt ?: "" }.thenBy { it.title.lowercase() },
    ),
    LeastRecentlyWatched(
        Res.string.library_sort_least_recently_watched,
        compareBy<SimklItem> { it.lastWatchedAt ?: "" }.thenBy { it.title.lowercase() },
    ),
    TitleAscending(Res.string.library_sort_title_ascending, compareBy { it.title.lowercase() }),
    TitleDescending(
        Res.string.library_sort_title_descending,
        compareByDescending { it.title.lowercase() },
    ),
}

/** The three lists Simkl keeps that are worth browsing. Hold and Dropped are not offered. */
private val Lists = listOf(
    SimklStatus.Watching,
    SimklStatus.PlanToWatch,
    SimklStatus.Completed,
)

@Composable
fun listLabel(status: String): String = when (status) {
    SimklStatus.PlanToWatch -> stringResource(Res.string.library_list_plan_to_watch)
    SimklStatus.Completed -> stringResource(Res.string.library_list_completed)
    else -> stringResource(Res.string.library_list_watching)
}

/** Simkl's own library names. Null means every type. */
@Composable
fun mediaTypeLabel(mediaType: String?): String = when (mediaType) {
    null -> stringResource(Res.string.library_all_types)
    "shows" -> stringResource(Res.string.library_type_series)
    "movies" -> stringResource(Res.string.library_type_movies)
    "anime" -> stringResource(Res.string.library_type_anime)
    // Simkl could add a library this build has never heard of; its own name is the only
    // honest thing to show for it.
    else -> mediaType.replaceFirstChar { it.uppercase() }
}

/**
 * Which list, which type, which order.
 *
 * The type picker offers only the types actually present in the chosen list, so it can
 * never filter the grid down to nothing.
 */
@Composable
fun LibraryControls(
    list: String,
    onSelectList: (String) -> Unit,
    types: List<String>,
    selectedType: String?,
    onSelectType: (String?) -> Unit,
    sort: LibrarySort,
    onSelectSort: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerChip(
            sheetTitle = stringResource(Res.string.library_select_list),
            label = listLabel(list),
            options = Lists.map { it to listLabel(it) },
            selected = list,
            onPick = onSelectList,
        )
        PickerChip(
            sheetTitle = stringResource(Res.string.library_select_type),
            label = mediaTypeLabel(selectedType),
            options = (listOf(AllTypes) + types).map { it to mediaTypeLabel(it.asMediaType()) },
            selected = selectedType ?: AllTypes,
            onPick = { onSelectType(it.asMediaType()) },
        )
        PickerChip(
            sheetTitle = stringResource(Res.string.library_sort_action),
            label = stringResource(sort.label),
            options = LibrarySort.entries.map { it to stringResource(it.label) },
            selected = sort,
            onPick = onSelectSort,
        )
    }
}

/** The type picker carries [AllTypes] as a value; everywhere else null means every type. */
private fun String.asMediaType(): String? = takeIf { it != AllTypes }
