package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.feature.detail.trackedEpisodeCount
import io.github.dimitrysaf.provenio.stremio.model.Meta

/** Backdrop behind, poster and title in front. */
@Composable
fun DetailHeader(meta: Meta) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            if (meta.background != null) {
                AsyncImage(
                    model = meta.background,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
            // Keeps the back button and the title legible over a bright still. `scrim` is
            // the role for darkening media, so a scheme override carries through here.
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        ),
                    ),
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (meta.poster != null) {
                    AsyncImage(
                        model = meta.poster,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.name ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmall,
                )
                val facts = listOfNotNull(
                    meta.releaseInfo,
                    meta.runtime,
                    meta.trackedEpisodeCount()
                        .takeIf { it > 0 }
                        ?.let { "$it eps" },
                )
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
