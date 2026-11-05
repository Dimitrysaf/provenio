package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.EpisodeLayout

/**
 * A small wireframe of one episode layout, for its row in the layout picker: plain blocks only,
 * like the home layout preview, so it reads as a shape rather than as content.
 */
@Composable
internal fun EpisodeLayoutPreview(layout: EpisodeLayout) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .size(width = PreviewWidth, height = PreviewHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainerLow)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        when (layout) {
            EpisodeLayout.List -> {
                // An open season, its episodes, then a folded season.
                PreviewSeasonRow(color = colors.surfaceContainerHighest)
                repeat(2) { PreviewEpisodeRow(block = colors.surfaceContainerHighest, thumbnail = colors.outlineVariant) }
                PreviewSeasonRow(color = colors.surfaceContainerHighest)
            }

            EpisodeLayout.Carousel -> {
                // The seasons' posters, the first one picked, then its episodes.
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .width(PosterWidth)
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceContainerHighest)
                                .then(
                                    if (index == 0) {
                                        Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(4.dp))
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
                repeat(3) { PreviewEpisodeRow(block = colors.surfaceContainerHighest, thumbnail = colors.outlineVariant) }
            }
        }
    }
}

@Composable
private fun PreviewSeasonRow(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

@Composable
private fun PreviewEpisodeRow(block: Color, thumbnail: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(block)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(thumbnail),
        )
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(thumbnail),
        )
    }
}

private val PreviewWidth = 72.dp
private val PreviewHeight = 84.dp
private val PosterWidth = 16.dp
