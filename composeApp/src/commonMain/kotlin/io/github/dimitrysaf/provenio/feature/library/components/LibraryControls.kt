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
import io.github.dimitrysaf.provenio.simkl.SimklStatus

/** What "no type chosen" reads as, and what the picker resets to. */
const val AllTypes = "All types"

/**
 * How the library is ordered.
 *
 * Simkl records when a title was last watched but not when it was added, so these sort by
 * the timestamp that exists rather than offering an "added" order the data cannot answer.
 * Titles sort case-insensitively, otherwise every lowercase title lands after every
 * uppercase one.
 */
enum class LibrarySort(val label: String, val comparator: Comparator<SimklItem>) {
    RecentlyWatched(
        "Recently watched",
        compareByDescending<SimklItem> { it.lastWatchedAt ?: "" }.thenBy { it.title.lowercase() },
    ),
    LeastRecentlyWatched(
        "Least recently watched",
        compareBy<SimklItem> { it.lastWatchedAt ?: "" }.thenBy { it.title.lowercase() },
    ),
    TitleAscending("Title A–Z", compareBy { it.title.lowercase() }),
    TitleDescending("Title Z–A", compareByDescending { it.title.lowercase() }),
}

/** The three lists Simkl keeps that are worth browsing. Hold and Dropped are not offered. */
private val Lists = listOf(
    SimklStatus.Watching,
    SimklStatus.PlanToWatch,
    SimklStatus.Completed,
)

fun listLabel(status: String): String = when (status) {
    SimklStatus.PlanToWatch -> "Plan to Watch"
    SimklStatus.Completed -> "Completed"
    else -> "Watching"
}

/** Simkl's own library names. Null means every type. */
fun mediaTypeLabel(mediaType: String?): String = when (mediaType) {
    null -> AllTypes
    "shows" -> "Series"
    "movies" -> "Movies"
    "anime" -> "Anime"
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
            sheetTitle = "Select list",
            label = listLabel(list),
            options = Lists.map { it to listLabel(it) },
            selected = list,
            onPick = onSelectList,
        )
        PickerChip(
            sheetTitle = "Select type",
            label = mediaTypeLabel(selectedType),
            options = (listOf(AllTypes) + types).map { it to mediaTypeLabel(it.asMediaType()) },
            selected = selectedType ?: AllTypes,
            onPick = { onSelectType(it.asMediaType()) },
        )
        PickerChip(
            sheetTitle = "Sort library",
            label = sort.label,
            options = LibrarySort.entries.map { it to it.label },
            selected = sort,
            onPick = onSelectSort,
        )
    }
}

/** The type picker carries [AllTypes] as a value; everywhere else null means every type. */
private fun String.asMediaType(): String? = takeIf { it != AllTypes }
