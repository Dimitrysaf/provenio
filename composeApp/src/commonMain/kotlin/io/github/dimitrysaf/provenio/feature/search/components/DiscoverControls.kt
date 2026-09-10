package io.github.dimitrysaf.provenio.feature.search.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.PickerChip
import io.github.dimitrysaf.provenio.stremio.InstalledAddon
import io.github.dimitrysaf.provenio.stremio.model.ManifestCatalog
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.discover_all_genres
import io.github.dimitrysaf.provenio.resources.discover_catalog
import io.github.dimitrysaf.provenio.resources.discover_genre
import io.github.dimitrysaf.provenio.resources.discover_title
import io.github.dimitrysaf.provenio.resources.discover_type
import io.github.dimitrysaf.provenio.resources.discover_type_channels
import io.github.dimitrysaf.provenio.resources.discover_type_movies
import io.github.dimitrysaf.provenio.resources.discover_type_series
import io.github.dimitrysaf.provenio.resources.discover_type_tv
import org.jetbrains.compose.resources.stringResource

/**
 * The value the genre picker carries for "every genre".
 *
 * A sentinel rather than a label, for the same reason as AllTypes in the library: it is
 * compared against and sent to addons, so it must not move with the language.
 */
const val AllGenres = "all-genres"

/**
 * The three things that decide what Discover is showing: a type, a catalog of that type,
 * and optionally one of that catalog's own genres.
 *
 * Draws no side padding of its own — it sits inside the Discover grid, which already pads
 * to the page margin, and padding again here would indent it twice.
 *
 * The genre picker is absent rather than disabled for a catalog that advertises none: a
 * control that can never do anything is worse than no control.
 */
@Composable
fun DiscoverControls(
    types: List<String>,
    selectedType: String?,
    onSelectType: (String) -> Unit,
    catalogs: List<Pair<InstalledAddon, ManifestCatalog>>,
    selected: Pair<InstalledAddon, ManifestCatalog>?,
    onSelectCatalog: (Pair<InstalledAddon, ManifestCatalog>) -> Unit,
    genres: List<String>,
    selectedGenre: String?,
    onSelectGenre: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.discover_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerChip(
                sheetTitle = stringResource(Res.string.discover_type),
                label = selectedType?.let { typeLabel(it) }
                    ?: stringResource(Res.string.discover_type),
                options = types.map { it to typeLabel(it) },
                selected = selectedType,
                onPick = onSelectType,
            )
            PickerChip(
                sheetTitle = stringResource(Res.string.discover_catalog),
                label = selected?.second?.displayName()
                    ?: stringResource(Res.string.discover_catalog),
                options = catalogs.map { it to it.second.displayName() },
                selected = selected,
                onPick = onSelectCatalog,
            )
            if (genres.isNotEmpty()) {
                // Genre names come from the addon and are shown as it wrote them. Only the
                // "every genre" entry is ours, so only it is translated.
                val everyGenre = stringResource(Res.string.discover_all_genres)
                PickerChip(
                    sheetTitle = stringResource(Res.string.discover_genre),
                    label = selectedGenre ?: everyGenre,
                    options = (listOf(AllGenres) + genres).map { genre ->
                        genre to if (genre == AllGenres) everyGenre else genre
                    },
                    selected = selectedGenre ?: AllGenres,
                    onPick = { onSelectGenre(it.takeIf { g -> g != AllGenres }) },
                )
            }
        }

        if (selected != null) {
            Text(
                text = "${selected.first.manifest.name} · ${typeLabel(selected.second.type)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** The protocol's type strings are lowercase and terse; these are what a person reads. */
@Composable
internal fun typeLabel(type: String): String = when (type) {
    "movie" -> stringResource(Res.string.discover_type_movies)
    "series" -> stringResource(Res.string.discover_type_series)
    "channel" -> stringResource(Res.string.discover_type_channels)
    "tv" -> stringResource(Res.string.discover_type_tv)
    else -> type.replaceFirstChar { it.uppercase() }
}

internal fun ManifestCatalog.displayName(): String = name ?: id
