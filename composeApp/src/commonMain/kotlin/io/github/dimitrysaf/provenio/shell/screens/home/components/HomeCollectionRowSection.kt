package io.github.dimitrysaf.provenio.shell.screens.home.components

import io.github.dimitrysaf.provenio.shell.components.PosterLandscapeAspectRatio
import io.github.dimitrysaf.provenio.shell.components.PosterPortraitAspectRatio
import io.github.dimitrysaf.provenio.shell.components.SkeletonAsyncImage
import io.github.dimitrysaf.provenio.shell.components.landscapePosterWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.CollapsibleShelfHeader
import io.github.dimitrysaf.provenio.shell.components.ShelfGrid
import io.github.dimitrysaf.provenio.shell.components.ShelfSection
import io.github.dimitrysaf.provenio.shell.components.posterCardClickable
import io.github.dimitrysaf.provenio.shell.components.rememberPosterCardStyleUiState
import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.core.collection.CollectionFolder

@Composable
fun HomeCollectionRowSection(
    collection: Collection,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    gridExpanded: Boolean? = null,
    onToggleExpanded: () -> Unit = {},
) {
    if (collection.folders.isEmpty()) return

    if (sectionPadding != null) {
        HomeCollectionRowSectionContent(
            collection = collection,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            onFolderClick = onFolderClick,
            gridExpanded = gridExpanded,
            onToggleExpanded = onToggleExpanded,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeCollectionRowSectionContent(
                collection = collection,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                    onFolderClick = onFolderClick,
                gridExpanded = gridExpanded,
                onToggleExpanded = onToggleExpanded,
            )
        }
    }
}

@Composable
private fun HomeCollectionRowSectionContent(
    collection: Collection,
    modifier: Modifier,
    sectionPadding: Dp,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)?,
    gridExpanded: Boolean?,
    onToggleExpanded: () -> Unit,
) {
    // A grid shelf: its title opens and closes it, and the folders wrap instead of scrolling.
    if (gridExpanded != null) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CollapsibleShelfHeader(
                title = collection.title,
                expanded = gridExpanded,
                onToggle = onToggleExpanded,
                horizontalPadding = sectionPadding,
            )
            AnimatedVisibility(
                visible = gridExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                ShelfGrid(
                    entries = collection.folders,
                    horizontalPadding = sectionPadding,
                    key = { folder -> "collection_${collection.id}_folder_${folder.id}" },
                ) { folder ->
                    CollectionFolderCard(
                        folder = folder,
                                    onClick = onFolderClick?.let { { it(collection.id, folder.id) } },
                    )
                }
            }
        }
        return
    }
    ShelfSection(
        title = collection.title,
        entries = collection.folders,
        modifier = modifier,
        headerHorizontalPadding = sectionPadding,
        rowContentPadding = PaddingValues(horizontal = sectionPadding),
        key = { folder -> "collection_${collection.id}_folder_${folder.id}" },
    ) { folder ->
        CollectionFolderCard(
            folder = folder,
            onClick = onFolderClick?.let { { it(collection.id, folder.id) } },
        )
    }
}

@Composable
private fun CollectionFolderCard(
    folder: CollectionFolder,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    // A folder takes the card style the catalogs use: a poster, or landscape when they are.
    val landscape = posterCardStyle.catalogLandscapeModeEnabled
    val cardWidth = if (landscape) landscapePosterWidth(posterCardStyle.widthDp) else posterCardStyle.widthDp.dp
    val aspectRatio = if (landscape) PosterLandscapeAspectRatio else PosterPortraitAspectRatio
    val imageUrl = folder.coverImageUrl?.takeIf(String::isNotBlank)

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shapeCorner = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
        // Its picture, or its title in the card's place when it has none.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            shape = shapeCorner,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl != null) {
                    SkeletonAsyncImage(
                        model = imageUrl,
                        contentDescription = folder.title,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = folder.title,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (onClick != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .posterCardClickable(onClick = onClick, onLongClick = null),
                    )
                }
            }
        }

        if (imageUrl != null) {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
