package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.detail_no_description
import io.github.dimitrysaf.provenio.resources.detail_not_rated
import io.github.dimitrysaf.provenio.resources.detail_plot
import io.github.dimitrysaf.provenio.resources.detail_ratings
import org.jetbrains.compose.resources.stringResource

@Composable
fun Ratings(meta: Meta) {
    SectionCard(
        title = stringResource(Res.string.detail_ratings),
        icon = Icons.Outlined.StarOutline,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            RatingBlock("IMDb", meta.imdbRating)
            RatingBlock("Simkl", null)
        }
    }
}

@Composable
private fun RatingBlock(source: String, value: String?) {
    Column {
        Text(
            text = source,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(Res.string.detail_not_rated),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun Synopsis(meta: Meta) {
    SectionCard(
        title = stringResource(Res.string.detail_plot),
        icon = Icons.AutoMirrored.Outlined.Subject,
    ) {
        Text(
            text = meta.description ?: stringResource(Res.string.detail_no_description),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
