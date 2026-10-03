package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.components.SkeletonPosterRow
import io.github.dimitrysaf.provenio.shell.components.horizontalScrollBleed
import io.github.dimitrysaf.provenio.shell.components.landscapePosterHeightForWidth
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.shell.components.skeleton

@Composable
internal fun DetailSectionSkeleton(
    key: MetaScreenSectionKey,
    horizontalScrollPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(width = 160.dp, height = 24.dp)
        when (key) {
            MetaScreenSectionKey.CAST -> CastSkeletonRow(horizontalScrollPadding)
            MetaScreenSectionKey.TRAILERS -> LandscapeCardSkeletonRow(horizontalScrollPadding)
            MetaScreenSectionKey.PRODUCTION -> ProductionSkeletonRow()
            MetaScreenSectionKey.COLLECTION,
            MetaScreenSectionKey.MORE_LIKE_THIS -> PosterSkeletonRow(horizontalScrollPadding)
            MetaScreenSectionKey.COMMENTS -> CommentSkeletonRow(horizontalScrollPadding)
            MetaScreenSectionKey.DETAILS -> InfoRowsSkeleton()
            else -> TextLinesSkeleton()
        }
    }
}

@Composable
private fun SkeletonRow(
    horizontalScrollPadding: Dp,
    spacing: Dp,
    itemWidth: Dp,
    content: @Composable (count: Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .horizontalScrollBleed(horizontalScrollPadding)
            .fillMaxWidth()
            .clipToBounds(),
    ) {
        val count = ((maxWidth - horizontalScrollPadding) / (itemWidth + spacing)).toInt().coerceAtLeast(0) + 1
        Row(
            modifier = Modifier.padding(start = horizontalScrollPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
        ) {
            content(count)
        }
    }
}

@Composable
private fun CastSkeletonRow(horizontalScrollPadding: Dp) {
    val photoWidth = 80.dp
    SkeletonRow(horizontalScrollPadding = horizontalScrollPadding, spacing = 16.dp, itemWidth = photoWidth) { count ->
        repeat(count) {
            Column(
                modifier = Modifier.width(photoWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .skeleton(MaterialTheme.shapes.medium),
                )
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.86f), height = 14.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f), height = 12.dp)
            }
        }
    }
}

@Composable
private fun LandscapeCardSkeletonRow(horizontalScrollPadding: Dp) {
    val cardWidth = 260.dp
    SkeletonRow(horizontalScrollPadding = horizontalScrollPadding, spacing = 12.dp, itemWidth = cardWidth) { count ->
        repeat(count) {
            Column(
                modifier = Modifier.width(cardWidth),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .skeleton(MaterialTheme.shapes.medium),
                )
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.7f), height = 14.dp)
            }
        }
    }
}

@Composable
private fun ProductionSkeletonRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(132.dp, 108.dp, 120.dp).forEach { width ->
            SkeletonBlock(width = width, height = 32.dp, cornerRadius = 8.dp)
        }
    }
}

@Composable
private fun CommentSkeletonRow(horizontalScrollPadding: Dp) {
    val cardWidth = 280.dp
    SkeletonRow(horizontalScrollPadding = horizontalScrollPadding, spacing = 12.dp, itemWidth = cardWidth) { count ->
        repeat(count) {
            SkeletonBlock(width = cardWidth, height = 140.dp, cornerRadius = 16.dp)
        }
    }
}

@Composable
private fun PosterSkeletonRow(horizontalScrollPadding: Dp) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val landscape = posterCardStyle.catalogLandscapeModeEnabled
    val width = if (landscape) landscapePosterWidth(posterCardStyle.widthDp) else posterCardStyle.widthDp.dp
    val height = if (landscape) landscapePosterHeightForWidth(width) else posterCardStyle.heightDp.dp
    SkeletonPosterRow(
        width = width,
        height = height,
        cornerRadius = posterCardStyle.cornerRadiusDp.dp,
        showLabels = !landscape && !posterCardStyle.hideLabelsEnabled,
        modifier = Modifier.horizontalScrollBleed(horizontalScrollPadding),
        horizontalPadding = horizontalScrollPadding,
    )
}

@Composable
private fun InfoRowsSkeleton() {
    Column(modifier = Modifier.fillMaxWidth()) {
        listOf(0.36f, 0.24f, 0.18f, 0.3f).forEachIndexed { index, valueFraction ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.28f), height = 14.dp)
                SkeletonBlock(modifier = Modifier.fillMaxWidth(valueFraction / 0.72f), height = 14.dp)
            }
            if (index < 3) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TextLinesSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1f, 0.94f, 0.72f).forEach { fraction ->
            SkeletonBlock(modifier = Modifier.fillMaxWidth(fraction), height = 16.dp)
        }
    }
}
