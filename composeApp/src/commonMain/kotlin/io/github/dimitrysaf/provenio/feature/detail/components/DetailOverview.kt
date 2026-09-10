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

@Composable
fun Ratings(meta: Meta) {
    SectionCard(title = "Ratings", icon = Icons.Outlined.StarOutline) {
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
            text = value ?: "Not rated",
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun Synopsis(meta: Meta) {
    SectionCard(title = "Plot", icon = Icons.AutoMirrored.Outlined.Subject) {
        Text(
            text = meta.description ?: "No description provided",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
