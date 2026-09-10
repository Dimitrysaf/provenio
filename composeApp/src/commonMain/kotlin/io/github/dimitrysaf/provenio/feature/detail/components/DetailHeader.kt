package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.designsystem.components.Backdrop
import io.github.dimitrysaf.provenio.feature.detail.trackedEpisodeCount
import io.github.dimitrysaf.provenio.stremio.model.Meta

/**
 * Backdrop behind, poster and title in front.
 *
 * The backdrop is the same component and the same height math as the hero on Home, minus
 * everything written over it there: the logo only, since the year, type and genre already
 * have their own place in the row below.
 *
 * [backdropAlpha] comes from how far the page has scrolled, so the artwork dissolves on
 * its way out rather than sliding under the bar still at full strength. Only the backdrop
 * takes it — the poster and title below scroll normally.
 *
 * [showBackdrop] is false on a large screen, where the artwork is dropped altogether and
 * the page opens on the poster row under a bar that is always there.
 */
@Composable
fun DetailHeader(
    meta: Meta,
    backdropHeight: Dp,
    backdropAlpha: Float = 1f,
    showBackdrop: Boolean = true,
) {
    // Tied to the backdrop rather than fixed, exactly as the hero sizes its own.
    val logoHeight = minOf(backdropHeight * 0.22f, 110.dp)

    Column {
        if (showBackdrop) {
            // topScrim because a back button floats over this one, which the hero has no
            // equivalent of — without it the icon disappears into a bright still.
            Backdrop(
                url = meta.background,
                height = backdropHeight,
                topScrim = true,
                modifier = Modifier.graphicsLayer { alpha = backdropAlpha },
            ) {
                if (meta.logo != null) {
                    // The padding goes on the box, not the image: constraints flow outside
                    // in, so padding under a heightIn eats into the height the image is
                    // allowed rather than sitting beneath it, and the logo comes out short.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = meta.logo,
                            contentDescription = meta.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(0.75f).heightIn(max = logoHeight),
                        )
                    }
                }
            }
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
