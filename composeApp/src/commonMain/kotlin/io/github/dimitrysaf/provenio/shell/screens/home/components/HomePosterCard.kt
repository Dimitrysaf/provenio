package io.github.dimitrysaf.provenio.shell.screens.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.core.format.formatReleaseDateForDisplay
import io.github.dimitrysaf.provenio.shell.components.PosterCard
import io.github.dimitrysaf.provenio.shell.components.PosterCardShape
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.PosterShape

@Composable
fun HomePosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    useLandscapeBackdropMode: Boolean = false,
    isWatched: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showLandscapeOverlay: Boolean = true,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = useLandscapeBackdropMode || posterCardStyle.catalogLandscapeModeEnabled

    PosterCard(
        title = item.name,
        imageUrl = if (isLandscapeMode) (item.banner ?: item.poster) else item.poster,
        modifier = modifier,
        shape = if (isLandscapeMode) PosterCardShape.Landscape else item.posterShape.toPosterCardShape(),
        detailLine = if (isLandscapeMode || posterCardStyle.hideLabelsEnabled) null else item.releaseInfo?.let { formatReleaseDateForDisplay(it) },
        showTitleBelow = !posterCardStyle.hideLabelsEnabled,
        bottomLeftLogoUrl = if (isLandscapeMode && showLandscapeOverlay) item.logo else null,
        bottomLeftText = if (isLandscapeMode && showLandscapeOverlay && item.logo.isNullOrBlank() && !posterCardStyle.hideLabelsEnabled) item.name else null,
        isWatched = isWatched,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

private fun PosterShape.toPosterCardShape(): PosterCardShape =
    when (this) {
        PosterShape.Poster -> PosterCardShape.Poster
        PosterShape.Square -> PosterCardShape.Square
        PosterShape.Landscape -> PosterCardShape.Landscape
    }
