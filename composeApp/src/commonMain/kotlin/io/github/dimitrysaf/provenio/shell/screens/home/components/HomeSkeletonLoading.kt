package io.github.dimitrysaf.provenio.shell.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.SkeletonBlock
import io.github.dimitrysaf.provenio.shell.components.SkeletonPosterRow
import io.github.dimitrysaf.provenio.shell.components.landscapePosterHeightForWidth
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState

@Composable
fun HomeSkeletonHero(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
        )

        HomeSkeletonHeroCarousel(layout = layout)
    }
}

/** The hero loading state: the focal card, the slivers beside it and the indicator row. */
@Composable
private fun HomeSkeletonHeroCarousel(layout: HomeHeroLayout) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = heroCarouselTopInset()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .padding(horizontal = layout.contentHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(layout.itemSpacing),
        ) {
            SkeletonBlock(
                width = layout.smallItemWidth,
                height = layout.heroHeight,
                cornerRadius = HeroSkeletonCornerRadius,
            )
            SkeletonBlock(
                modifier = Modifier.weight(1f),
                height = layout.heroHeight,
                cornerRadius = HeroSkeletonCornerRadius,
            )
            SkeletonBlock(
                width = layout.smallItemWidth,
                height = layout.heroHeight,
                cornerRadius = HeroSkeletonCornerRadius,
            )
        }

        Spacer(modifier = Modifier.height(layout.contentVerticalPadding))

        Row(
            modifier = Modifier.height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBlock(width = 32.dp, height = 8.dp, cornerRadius = 4.dp)
            SkeletonBlock(width = 8.dp, height = 8.dp, cornerRadius = 4.dp)
            SkeletonBlock(width = 8.dp, height = 8.dp, cornerRadius = 4.dp)
        }
    }
}

private val HeroSkeletonCornerRadius = 28.dp

@Composable
fun HomeSkeletonRow(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val skeletonWidth = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterWidth(posterCardStyle.widthDp)
    } else {
        posterCardStyle.widthDp.dp
    }
    val skeletonHeight = if (posterCardStyle.catalogLandscapeModeEnabled) {
        landscapePosterHeightForWidth(skeletonWidth)
    } else {
        posterCardStyle.heightDp.dp
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sectionPadding = horizontalPadding ?: homeSectionHorizontalPaddingForWidth(maxWidth.value)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.padding(horizontal = sectionPadding).height(32.dp)) {
                SkeletonBlock(
                    modifier = Modifier.align(Alignment.CenterStart),
                    width = 128.dp,
                    height = 16.dp,
                )
            }
            SkeletonPosterRow(
                width = skeletonWidth,
                height = skeletonHeight,
                cornerRadius = posterCardStyle.cornerRadiusDp.dp,
                horizontalPadding = sectionPadding,
                showLabels = !posterCardStyle.hideLabelsEnabled,
                showDetail = !posterCardStyle.catalogLandscapeModeEnabled,
            )
        }
    }
}
