package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.SingleChoiceBottomSheet
import com.nuvio.app.core.ui.SingleChoiceOption
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.details.MetaTrailer
import com.nuvio.app.features.details.youtubeThumbnailUrl
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailTrailersSection(
    trailers: List<MetaTrailer>,
    onTrailerClick: (MetaTrailer) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    if (trailers.isEmpty()) return

    val trailerLabel = stringResource(Res.string.detail_tab_trailer)
    val grouped = remember(trailers, trailerLabel) {
        trailers.groupBy { trailer -> trailer.type.ifBlank { trailerLabel } }
    }
    val initialCategory = remember(grouped) {
        grouped.keys.firstOrNull { it.equals(trailerLabel, ignoreCase = true) } ?: grouped.keys.first()
    }
    var selectedCategory by remember(grouped) { mutableStateOf(initialCategory) }
    var categorySheetVisible by remember { mutableStateOf(false) }
    val selectedTrailers = grouped[selectedCategory].orEmpty()
    val hasCategoryChoice = grouped.size > 1
    val cornerRadius = rememberPosterCardStyleUiState().cornerRadiusDp.dp
    val trailersTitle = stringResource(Res.string.detail_trailers_title)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (hasCategoryChoice || showHeader) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasCategoryChoice) {
                    FilterChip(
                        selected = true,
                        onClick = { categorySheetVisible = true },
                        label = {
                            Text(
                                text = selectedCategory,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        },
                    )
                }
                if (showHeader) {
                    DetailSectionTitle(title = trailersTitle, fullWidth = false)
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizing = trailerSectionSizing(maxWidth.value)
            // A fresh carousel per category, so switching starts from the first trailer.
            val carouselState = key(selectedCategory) {
                rememberCarouselState(itemCount = { selectedTrailers.size })
            }
            HorizontalUncontainedCarousel(
                state = carouselState,
                itemWidth = sizing.cardWidth,
                modifier = Modifier
                    .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                    .fillMaxWidth(),
                itemSpacing = sizing.cardSpacing,
                contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
            ) { index ->
                val trailer = selectedTrailers[index]
                TrailerCard(
                    trailer = trailer,
                    cornerRadius = cornerRadius,
                    onClick = { onTrailerClick(trailer) },
                )
            }
        }
    }

    if (categorySheetVisible) {
        SingleChoiceBottomSheet(
            title = trailersTitle,
            options = grouped.map { (category, categoryTrailers) ->
                SingleChoiceOption(
                    value = category,
                    label = stringResource(Res.string.detail_trailer_category_count, category, categoryTrailers.size),
                )
            },
            isSelected = { it == selectedCategory },
            onSelected = { selectedCategory = it },
            onDismiss = { categorySheetVisible = false },
        )
    }
}

@Composable
private fun TrailerCard(
    trailer: MetaTrailer,
    cornerRadius: Dp,
    onClick: () -> Unit,
) {
    val title = trailer.displayName?.takeIf { it.isNotBlank() } ?: trailer.name

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(cornerRadius),
        ) {
            AsyncImage(
                model = trailer.youtubeThumbnailUrl(),
                contentDescription = title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        trailer.publishedAt?.take(4)?.takeIf { it.isNotBlank() }?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class TrailerSectionSizing(
    val cardWidth: Dp,
    val cardSpacing: Dp,
)

private fun trailerSectionSizing(maxWidthDp: Float): TrailerSectionSizing =
    when {
        maxWidthDp >= 1200f -> TrailerSectionSizing(cardWidth = 360.dp, cardSpacing = 16.dp)
        maxWidthDp >= 1024f -> TrailerSectionSizing(cardWidth = 320.dp, cardSpacing = 14.dp)
        maxWidthDp >= 768f -> TrailerSectionSizing(cardWidth = 300.dp, cardSpacing = 12.dp)
        else -> TrailerSectionSizing(cardWidth = 260.dp, cardSpacing = 12.dp)
    }
