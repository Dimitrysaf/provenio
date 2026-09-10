package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.db.SimklItem
import io.github.dimitrysaf.provenio.designsystem.components.PosterCard
import io.github.dimitrysaf.provenio.designsystem.components.PosterShelfWidth
import io.github.dimitrysaf.provenio.simkl.SimklImages

/**
 * Simkl types its libraries as shows, movies and anime. The addon protocol does not.
 *
 * Shared by the shelves in this package rather than declared twice, since both open a
 * title the same way.
 */
internal fun SimklItem.stremioType(): String = if (mediaType == "movies") "movie" else "series"

/**
 * One shelf. Never rendered empty: a heading over nothing is noise, so the screen shows a
 * single explanation instead when the whole library is empty.
 */
@Composable
fun Shelf(
    title: String,
    items: List<SimklItem>,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { item ->
                PosterCard(
                    title = item.title,
                    posterUrl = SimklImages.poster(item.poster),
                    onClick = { item.imdbId?.let { onOpenDetail(item.stremioType(), it) } },
                    modifier = Modifier.width(PosterShelfWidth),
                )
            }
        }
    }
}
