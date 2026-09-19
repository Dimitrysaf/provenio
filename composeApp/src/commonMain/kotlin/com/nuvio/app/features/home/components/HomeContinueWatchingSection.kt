package com.nuvio.app.features.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.DisintegratingContainer
import com.nuvio.app.core.ui.DisintegrationRequest
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.PosterCardStyleUiState
import com.nuvio.app.core.ui.ScopedDisintegrationTracker
import com.nuvio.app.core.ui.landscapePosterHeightForWidth
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.cloud.CloudLibraryContentType
import com.nuvio.app.features.cloud.cloudLibraryDisplayArtworkUrl
import com.nuvio.app.features.tracking.WatchProgressSource
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import com.nuvio.app.features.watchprogress.WatchProgressCompletionPercentThreshold
import com.nuvio.app.features.watchprogress.continueWatchingItemKey
import com.nuvio.app.features.watchprogress.CurrentDateProvider
import com.nuvio.app.features.watchprogress.computeAirDateBadgeText
import kotlin.math.roundToInt
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val ContinueWatchingStatusBadgeShape = RoundedCornerShape(4.dp)
/**
 * Text on artwork, over the scrim.
 *
 * The scheme has no role for this: `onSurface` follows the theme and would turn dark in a light
 * one, while the scrim under it stays dark either way. Material's own media overlays are white
 * for the same reason.
 */
private val OnScrimColor = Color.White
private val OnScrimVariantColor = Color.White.copy(alpha = 0.72f)

/** How far artwork is blurred when the episode has not been watched yet. */
private val ContinueWatchingArtworkBlur = 18.dp
private const val ContinueWatchingLandscapeCardScale = 1.2f
internal val HomeContinueWatchingSectionBottomPadding = 12.dp

internal fun continueWatchingLandscapeCardWidth(basePosterWidthDp: Int): Dp =
    (landscapePosterWidth(basePosterWidthDp).value * ContinueWatchingLandscapeCardScale).dp

internal fun continueWatchingLandscapeCardHeight(basePosterWidthDp: Int): Dp =
    landscapePosterHeightForWidth(continueWatchingLandscapeCardWidth(basePosterWidthDp))

internal fun continueWatchingSectionHeightEstimate(
    style: ContinueWatchingSectionStyle,
    layout: ContinueWatchingLayout,
    basePosterWidthDp: Int,
): Dp {
    val headerHeight = NuvioTokens.Space.s40
    val headerToRowGap = NuvioTokens.Space.s8 + NuvioTokens.Space.s2
    val rowHeight = when (style) {
        ContinueWatchingSectionStyle.Card -> continueWatchingLandscapeCardHeight(basePosterWidthDp)
        ContinueWatchingSectionStyle.Wide -> layout.wideCardHeight
        ContinueWatchingSectionStyle.Poster -> layout.posterCardHeight + layout.posterTitleBlockHeight
    }
    return headerHeight + headerToRowGap + rowHeight + HomeContinueWatchingSectionBottomPadding
}

internal fun continueWatchingHeroViewportReserveHeight(
    style: ContinueWatchingSectionStyle,
    layout: ContinueWatchingLayout,
    basePosterWidthDp: Int,
): Dp {
    val bottomNavigationClearance = when (style) {
        ContinueWatchingSectionStyle.Card,
        ContinueWatchingSectionStyle.Wide -> NuvioTokens.Space.s24
        ContinueWatchingSectionStyle.Poster -> 0.dp
    }
    return continueWatchingSectionHeightEstimate(
        style = style,
        layout = layout,
        basePosterWidthDp = basePosterWidthDp,
    ) + bottomNavigationClearance
}

private fun continueWatchingProgressPercent(progressFraction: Float): Int =
    (progressFraction * 100f).roundToInt().coerceIn(1, 99)

@Composable
private fun localizedContinueWatchingMetaLine(item: ContinueWatchingItem): String =
    when {
        item.seasonNumber != null && item.episodeNumber != null ->
            stringResource(Res.string.compose_player_episode_code_full, item.seasonNumber, item.episodeNumber)
        item.isCloudLibraryItem() ->
            stringResource(Res.string.library_source_cloud)
        else ->
            stringResource(Res.string.media_movie)
    }

private fun ContinueWatchingItem.isCloudLibraryItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

private fun ContinueWatchingItem.continueWatchingArtworkUrl(
    useEpisodeThumbnails: Boolean,
): String? = when {
    isNextUp && useEpisodeThumbnails -> firstNonBlank(
        episodeThumbnail,
        poster,
        background,
        imageUrl,
    )
    isNextUp -> firstNonBlank(
        poster,
        background,
        episodeThumbnail,
        imageUrl,
    )
    useEpisodeThumbnails -> firstNonBlank(
        episodeThumbnail,
        poster,
        background,
        imageUrl,
    )
    else -> firstNonBlank(
        poster,
        background,
        episodeThumbnail,
        imageUrl,
    )
}

private fun ContinueWatchingItem.continueWatchingPosterArtworkUrl(
    useEpisodeThumbnails: Boolean,
): String? {
    if (seasonNumber == null || episodeNumber == null) {
        return continueWatchingArtworkUrl(useEpisodeThumbnails)
    }

    val normalizedEpisodeThumbnail = episodeThumbnail?.trim()?.takeIf { it.isNotBlank() }
    val nonEpisodeImageUrl = imageUrl
        ?.trim()
        ?.takeIf { it.isNotBlank() && it != normalizedEpisodeThumbnail }

    return firstNonBlank(
        poster,
        background,
        nonEpisodeImageUrl,
        if (useEpisodeThumbnails) episodeThumbnail else null,
        imageUrl,
    )
}

private fun ContinueWatchingItem.continueWatchingCardArtworkUrl(
    useEpisodeThumbnails: Boolean,
    preferBackdropForNextUp: Boolean,
): String? = when {
    isNextUp && preferBackdropForNextUp -> firstNonBlank(
        background,
        poster,
        episodeThumbnail,
        imageUrl,
    )
    isNextUp && useEpisodeThumbnails -> firstNonBlank(
        episodeThumbnail,
        background,
        poster,
        imageUrl,
    )
    isNextUp -> firstNonBlank(
        background,
        poster,
        episodeThumbnail,
        imageUrl,
    )
    useEpisodeThumbnails -> firstNonBlank(
        episodeThumbnail,
        background,
        poster,
        imageUrl,
    )
    else -> firstNonBlank(
        background,
        poster,
        episodeThumbnail,
        imageUrl,
    )
}

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { value -> !value.isNullOrBlank() }?.trim()

internal fun ContinueWatchingItem.shouldBlurContinueWatchingArtwork(
    blurUnwatchedEpisodes: Boolean,
    useEpisodeThumbnails: Boolean,
    artworkUrl: String?,
): Boolean {
    if (!blurUnwatchedEpisodes || !useEpisodeThumbnails) return false
    val thumbnail = episodeThumbnail?.trim()?.takeIf { it.isNotBlank() } ?: return false
    val artwork = artworkUrl?.trim()?.takeIf { it.isNotBlank() } ?: return false
    val isUnwatched = isNextUp || progressFraction < WatchProgressCompletionPercentThreshold / 100f
    return isUnwatched && artwork == thumbnail
}

@Composable
internal fun HomeContinueWatchingSection(
    items: List<ContinueWatchingItem>,
    style: ContinueWatchingSectionStyle,
    dataSourceKey: WatchProgressSource,
    useEpisodeThumbnails: Boolean = true,
    blurNextUp: Boolean = false,
    modifier: Modifier = Modifier,
    title: String? = null,
    sectionPadding: Dp? = null,
    layout: ContinueWatchingLayout? = null,
    listState: LazyListState = rememberLazyListState(),
    onItemClick: ((ContinueWatchingItem) -> Unit)? = null,
    onItemLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    disintegrationRequest: DisintegrationRequest<String>? = null,
) {
    if (items.isEmpty()) return

    if (sectionPadding != null && layout != null) {
        HomeContinueWatchingSectionContent(
            items = items,
            dataSourceKey = dataSourceKey,
            style = style,
            useEpisodeThumbnails = useEpisodeThumbnails,
            blurNextUp = blurNextUp,
            modifier = modifier.fillMaxWidth(),
            title = title,
            sectionPadding = sectionPadding,
            layout = layout,
            listState = listState,
            onItemClick = onItemClick,
            onItemLongPress = onItemLongPress,
            disintegrationRequest = disintegrationRequest,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeContinueWatchingSectionContent(
                items = items,
                dataSourceKey = dataSourceKey,
                style = style,
                useEpisodeThumbnails = useEpisodeThumbnails,
                blurNextUp = blurNextUp,
                modifier = Modifier.fillMaxWidth(),
                title = title,
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                layout = rememberContinueWatchingLayout(maxWidth.value, rememberPosterCardStyleUiState()),
                listState = listState,
                onItemClick = onItemClick,
                onItemLongPress = onItemLongPress,
                disintegrationRequest = disintegrationRequest,
            )
        }
    }
}

@Composable
private fun HomeContinueWatchingSectionContent(
    items: List<ContinueWatchingItem>,
    dataSourceKey: WatchProgressSource,
    style: ContinueWatchingSectionStyle,
    useEpisodeThumbnails: Boolean,
    blurNextUp: Boolean,
    modifier: Modifier,
    title: String?,
    sectionPadding: Dp,
    layout: ContinueWatchingLayout,
    listState: LazyListState,
    onItemClick: ((ContinueWatchingItem) -> Unit)?,
    onItemLongPress: ((ContinueWatchingItem) -> Unit)?,
    disintegrationRequest: DisintegrationRequest<String>?,
) {
    key(dataSourceKey) {
        val disintegration = remember {
            ScopedDisintegrationTracker<WatchProgressSource, String, ContinueWatchingItem>(
                itemKey = ::continueWatchingItemKey,
            )
        }
        val displayEntries = disintegration.sync(dataSourceKey, items, disintegrationRequest)

        NuvioShelfSection(
            title = title ?: stringResource(Res.string.compose_settings_page_continue_watching),
            entries = displayEntries,
            modifier = modifier,
            headerHorizontalPadding = sectionPadding,
            rowContentPadding = PaddingValues(horizontal = sectionPadding),
            itemSpacing = layout.itemGap,
            key = { entry -> entry.key },
            animatePlacement = true,
            state = listState,
        ) { entry ->
            val item = entry.item
            val onClick = if (entry.exiting) null else onItemClick?.let { { it(item) } }
            val onLongClick = if (entry.exiting) null else onItemLongPress?.let { { it(item) } }
            DisintegratingContainer(
                disintegrating = entry.exiting,
                onDisintegrated = { disintegration.onDisintegrated(entry.key) },
            ) {
                when (style) {
                    ContinueWatchingSectionStyle.Card -> ContinueWatchingCard(
                        item = item,
                        useEpisodeThumbnails = useEpisodeThumbnails,
                        blurNextUp = blurNextUp,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    ContinueWatchingSectionStyle.Wide -> ContinueWatchingWideCard(
                        item = item,
                        layout = layout,
                        useEpisodeThumbnails = useEpisodeThumbnails,
                        blurNextUp = blurNextUp,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    ContinueWatchingSectionStyle.Poster -> ContinueWatchingPosterCard(
                        item = item,
                        layout = layout,
                        useEpisodeThumbnails = useEpisodeThumbnails,
                        blurNextUp = blurNextUp,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                }
            }
        }
    }
}

/**
 * A miniature of one continue-watching style, for picking between them.
 *
 * Just the drawing. It used to paint its own tinted container and tint it again when selected,
 * which is a selection state invented from an alpha; now it sits in the leading slot of a list
 * row and the row shows selection the way the spec says to. The three miniatures are different
 * sizes, so the caller gives them one box to sit in and they centre inside it.
 */
@Composable
fun ContinueWatchingStylePreview(
    style: ContinueWatchingSectionStyle,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            ContinueWatchingSectionStyle.Card -> CardStylePreview()
            ContinueWatchingSectionStyle.Wide -> WideCardPreview()
            ContinueWatchingSectionStyle.Poster -> PosterCardPreview()
        }
    }
}

@Composable
private fun CardStylePreview() {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.60f to MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                            1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(5.dp)
                .width(26.dp)
                .height(9.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.80f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.55f)),
            )
            Box(
                modifier = Modifier
                    .width(58.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.75f)),
            )
        }
        LinearProgressIndicator(
            progress = { 0.55f },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 5.dp, vertical = 3.dp)
                .fillMaxWidth()
                .height(3.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = OnScrimColor.copy(alpha = 0.24f),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun WideCardPreview() {
    Row(
        modifier = Modifier
            .width(100.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
            )
            LinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun PosterCardPreview() {
    Column(
        modifier = Modifier
            .width(60.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        ) {
            LinearProgressIndicator(
                progress = { 0.45f },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = OnScrimColor.copy(alpha = 0.24f),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 6.dp, top = 1.dp)
                    .width(16.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
            )
        }
    }
}

private data class ContinueWatchingLandscapeCardMetrics(
    val width: Dp,
    val cornerRadius: Dp,
    val contentPadding: Dp,
    val textGap: Dp,
    val badgeInset: Dp,
    val progressHorizontalPadding: Dp,
    val progressBottomPadding: Dp,
    val progressHeight: Dp,
)

private fun continueWatchingLandscapeCardMetrics(
    basePosterWidthDp: Int,
    cornerRadiusDp: Int,
): ContinueWatchingLandscapeCardMetrics {
    val width = continueWatchingLandscapeCardWidth(basePosterWidthDp)
    return when {
        basePosterWidthDp <= 108 -> ContinueWatchingLandscapeCardMetrics(
            width = width,
            cornerRadius = cornerRadiusDp.dp,
            contentPadding = 8.dp,
            textGap = 1.dp,
            badgeInset = 6.dp,
            progressHorizontalPadding = 8.dp,
            progressBottomPadding = 3.dp,
            progressHeight = 3.dp,
        )
        basePosterWidthDp <= 120 -> ContinueWatchingLandscapeCardMetrics(
            width = width,
            cornerRadius = cornerRadiusDp.dp,
            contentPadding = 9.dp,
            textGap = 1.dp,
            badgeInset = 6.dp,
            progressHorizontalPadding = 8.dp,
            progressBottomPadding = 3.dp,
            progressHeight = 3.dp,
        )
        else -> ContinueWatchingLandscapeCardMetrics(
            width = width,
            cornerRadius = cornerRadiusDp.dp,
            contentPadding = 10.dp,
            textGap = 2.dp,
            badgeInset = 7.dp,
            progressHorizontalPadding = 9.dp,
            progressBottomPadding = 4.dp,
            progressHeight = 3.dp,
        )
    }
}

/**
 * The landscape card: artwork with the title over it.
 *
 * Material's media card, so the text sits on the scrim role rather than on a wash of the theme's
 * background colour — which was near-white in a light theme and took the white text with it.
 */
@Composable
private fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    useEpisodeThumbnails: Boolean,
    blurNextUp: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val cardMetrics = remember(posterCardStyle.widthDp, posterCardStyle.cornerRadiusDp) {
        continueWatchingLandscapeCardMetrics(
            basePosterWidthDp = posterCardStyle.widthDp,
            cornerRadiusDp = posterCardStyle.cornerRadiusDp,
        )
    }
    val todayIsoDate = CurrentDateProvider.todayIsoDate()
    val airDateText = if (item.progressFraction <= 0f && item.seasonNumber != null && item.episodeNumber != null) {
        computeAirDateBadgeText(item.released, todayIsoDate)
    } else {
        null
    }
    val preferBackdropForNextUp = item.isNextUp && airDateText != null && !item.isReleaseAlert
    val imageUrl = item.continueWatchingCardArtworkUrl(
        useEpisodeThumbnails = useEpisodeThumbnails,
        preferBackdropForNextUp = preferBackdropForNextUp,
    )
    val shouldBlurArtwork = item.shouldBlurContinueWatchingArtwork(
        blurUnwatchedEpisodes = blurNextUp,
        useEpisodeThumbnails = useEpisodeThumbnails,
        artworkUrl = imageUrl,
    )
    val episodeCode = if (item.seasonNumber != null && item.episodeNumber != null) {
        stringResource(Res.string.streams_episode_badge, item.seasonNumber, item.episodeNumber)
    } else {
        null
    }
    val episodeTitle = item.episodeTitle?.trim()?.takeIf { it.isNotBlank() } ?: airDateText
    val badgeText = continueWatchingCardBadgeText(item = item, airDateText = airDateText)
    val scrim = MaterialTheme.colorScheme.scrim

    Surface(
        modifier = Modifier
            .width(cardMetrics.width)
            .aspectRatio(PosterLandscapeAspectRatio)
            .posterCardClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                zoomImageUrl = imageUrl,
                zoomCornerRadius = cardMetrics.cornerRadius,
            ),
        shape = RoundedCornerShape(cardMetrics.cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null) {
                AsyncImage(
                    model = cloudLibraryDisplayArtworkUrl(imageUrl),
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (shouldBlurArtwork) Modifier.blur(ContinueWatchingArtworkBlur) else Modifier)
                        .drawWithContent {
                            drawContent()
                            val startY = size.height * 0.45f
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.6f to scrim.copy(alpha = 0.65f),
                                        1f to scrim.copy(alpha = 0.92f),
                                    ),
                                    startY = startY,
                                    endY = size.height,
                                ),
                                topLeft = Offset(0f, startY),
                                size = Size(size.width, size.height - startY),
                            )
                        },
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(cardMetrics.contentPadding),
                verticalArrangement = Arrangement.spacedBy(cardMetrics.textGap),
            ) {
                if (episodeCode != null) {
                    Text(
                        text = episodeCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = OnScrimColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnScrimColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episodeTitle != null) {
                    Text(
                        text = episodeTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnScrimVariantColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            ContinueWatchingBadge(
                item = item,
                text = badgeText,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(cardMetrics.badgeInset),
            )

            if (item.progressFraction > 0f) {
                LinearProgressIndicator(
                    progress = { item.progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = cardMetrics.progressHorizontalPadding,
                            vertical = cardMetrics.progressBottomPadding,
                        )
                        .fillMaxWidth()
                        .height(cardMetrics.progressHeight),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = OnScrimColor.copy(alpha = 0.24f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}

@Composable
private fun continueWatchingCardBadgeText(
    item: ContinueWatchingItem,
    airDateText: String?,
): String {
    if (item.progressFraction > 0f) {
        if (item.durationMs <= 0L) {
            return stringResource(
                Res.string.home_continue_watching_watched,
                "${continueWatchingProgressPercent(item.progressFraction)}%",
            )
        }
        val effectivePositionMs = when {
            item.resumePositionMs > 0L -> item.resumePositionMs
            else -> (item.durationMs * item.progressFraction.coerceIn(0f, 1f)).toLong()
        }
        val remainingMinutes = ((item.durationMs - effectivePositionMs).coerceAtLeast(0L) / 60_000L)
            .coerceAtLeast(1L)
        val hours = remainingMinutes / 60L
        val minutes = remainingMinutes % 60L
        return if (hours > 0L) {
            stringResource(Res.string.home_continue_watching_hours_minutes_left, hours, minutes)
        } else {
            stringResource(Res.string.home_continue_watching_minutes_left, remainingMinutes)
        }
    }

    return when {
        item.isReleaseAlert && item.isNewSeasonRelease -> stringResource(Res.string.cw_new_season)
        item.isReleaseAlert -> stringResource(Res.string.cw_new_episode)
        airDateText != null -> airDateText
        else -> stringResource(Res.string.home_continue_watching_up_next)
    }
}

/**
 * The wide card: Material's horizontal card, artwork at the leading edge and everything worth
 * knowing beside it.
 *
 * A filled container in the scheme's own tone, so it needs neither the translucent surface nor
 * the white hairline it used to draw to separate itself from the page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingWideCard(
    item: ContinueWatchingItem,
    layout: ContinueWatchingLayout,
    useEpisodeThumbnails: Boolean,
    blurNextUp: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val cornerRadius = rememberPosterCardStyleUiState().cornerRadiusDp.dp

    Surface(
        modifier = Modifier
            .width(layout.wideCardWidth)
            .height(layout.wideCardHeight)
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick,
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row {
            val artworkUrl = item.continueWatchingArtworkUrl(useEpisodeThumbnails)
            val shouldBlurArtwork = item.shouldBlurContinueWatchingArtwork(
                blurUnwatchedEpisodes = blurNextUp,
                useEpisodeThumbnails = useEpisodeThumbnails,
                artworkUrl = artworkUrl,
            )
            ArtworkPanel(
                imageUrl = artworkUrl,
                width = layout.widePosterStripWidth,
                blurred = shouldBlurArtwork,
                contentScale = if (item.isCloudLibraryItem()) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(layout.wideContentPadding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val wideMetaLine = localizedContinueWatchingMetaLine(item)
                val episodeTitle = item.episodeTitle?.trim()?.takeIf { it.isNotBlank() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = item.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.progressFraction <= 0f && item.seasonNumber != null && item.episodeNumber != null) {
                            ContinueWatchingBadge(
                                item = item,
                                text = upNextBadgeText(item),
                            )
                        }
                    }
                    Text(
                        text = wideMetaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (episodeTitle != null) {
                        Text(
                            text = episodeTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (item.progressFraction > 0f) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { item.progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(layout.progressHeight),
                            gapSize = 0.dp,
                            drawStopIndicator = {},
                        )
                        Text(
                            text = stringResource(
                                Res.string.home_continue_watching_watched,
                                "${continueWatchingProgressPercent(item.progressFraction)}%",
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The poster card: the artwork, and the least that has to be said under it.
 *
 * Progress runs along the bottom edge of the artwork rather than floating in a pill of its own,
 * which is where Material puts a determinate indicator that belongs to the thing above it.
 */
@Composable
private fun ContinueWatchingPosterCard(
    item: ContinueWatchingItem,
    layout: ContinueWatchingLayout,
    useEpisodeThumbnails: Boolean,
    blurNextUp: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val cornerRadius = rememberPosterCardStyleUiState().cornerRadiusDp.dp
    val imageUrl = item.continueWatchingPosterArtworkUrl(useEpisodeThumbnails)

    Column(
        modifier = Modifier.width(layout.posterCardWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.posterCardHeight)
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = imageUrl,
                    zoomCornerRadius = cornerRadius,
                ),
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val shouldBlurArtwork = item.shouldBlurContinueWatchingArtwork(
                    blurUnwatchedEpisodes = blurNextUp,
                    useEpisodeThumbnails = useEpisodeThumbnails,
                    artworkUrl = imageUrl,
                )
                if (imageUrl != null) {
                    AsyncImage(
                        model = cloudLibraryDisplayArtworkUrl(imageUrl),
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (shouldBlurArtwork) Modifier.blur(ContinueWatchingArtworkBlur) else Modifier),
                        contentScale = if (item.isCloudLibraryItem()) ContentScale.Fit else ContentScale.Crop,
                    )
                }
                if (item.progressFraction <= 0f && item.seasonNumber != null && item.episodeNumber != null) {
                    ContinueWatchingBadge(
                        item = item,
                        text = upNextBadgeText(item),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                }
                if (item.progressFraction > 0f) {
                    LinearProgressIndicator(
                        progress = { item.progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(layout.progressHeight),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = OnScrimColor.copy(alpha = 0.24f),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = item.title,
                modifier = Modifier
                    .weight(1f)
                    .height(layout.posterTitleBlockHeight),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.seasonNumber != null && item.episodeNumber != null) {
                Text(
                    text = stringResource(
                        Res.string.streams_episode_badge,
                        item.seasonNumber,
                        item.episodeNumber,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArtworkPanel(
    imageUrl: String?,
    width: Dp,
    blurred: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(width)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = cloudLibraryDisplayArtworkUrl(imageUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurred) Modifier.blur(18.dp) else Modifier),
                contentScale = contentScale,
            )
        }
    }
}

/**
 * What is new about this item, if anything.
 *
 * The container roles carry the distinction a pair of hardcoded blues and ambers used to: a new
 * season is the tertiary container, a new episode the primary one, and anything else the plain
 * raised tone. Each brings its own "on" colour, so the text is legible in either scheme.
 */
@Composable
private fun ContinueWatchingBadge(
    item: ContinueWatchingItem,
    text: String,
    modifier: Modifier = Modifier,
) {
    val container = when {
        item.isNewSeasonRelease -> MaterialTheme.colorScheme.tertiaryContainer
        item.isReleaseAlert -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    // Surface takes the matching "on" colour for a known container, so the label is legible in
    // either scheme without being told what colour to be.
    Surface(
        modifier = modifier,
        shape = ContinueWatchingStatusBadgeShape,
        color = container,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

/** What an item that has not been started yet is waiting on. */
@Composable
private fun upNextBadgeText(item: ContinueWatchingItem): String = when {
    item.isNewSeasonRelease && item.isReleaseAlert -> stringResource(Res.string.cw_new_season)
    item.isReleaseAlert -> stringResource(Res.string.cw_new_episode)
    else -> computeAirDateBadgeText(item.released, CurrentDateProvider.todayIsoDate())
        ?: stringResource(Res.string.home_continue_watching_up_next)
}

internal data class ContinueWatchingLayout(
    val itemGap: Dp,
    val wideCardWidth: Dp,
    val wideCardHeight: Dp,
    val widePosterStripWidth: Dp,
    val wideContentPadding: Dp,
    val posterCardWidth: Dp,
    val posterCardHeight: Dp,
    val progressHeight: Dp,
    val posterTitleBlockHeight: Dp,
)

internal fun rememberContinueWatchingLayout(
    maxWidthDp: Float,
    posterCardStyle: PosterCardStyleUiState = PosterCardStyleUiState(),
): ContinueWatchingLayout {
    val wideCardWidth = posterCardStyle.widthDp.dp * 2.1f
    val wideCardHeight = wideCardWidth * 0.4f
    val widePosterStripWidth = wideCardHeight * (2f / 3f)
    return when {
        maxWidthDp >= 1440f -> ContinueWatchingLayout(
            itemGap = 20.dp,
            wideCardWidth = wideCardWidth,
            wideCardHeight = wideCardHeight,
            widePosterStripWidth = widePosterStripWidth,
            wideContentPadding = 16.dp,
            posterCardWidth = posterCardStyle.widthDp.dp,
            posterCardHeight = posterCardStyle.heightDp.dp,
            progressHeight = 6.dp,
            posterTitleBlockHeight = 40.dp,
        )
        maxWidthDp >= 1024f -> ContinueWatchingLayout(
            itemGap = 18.dp,
            wideCardWidth = wideCardWidth,
            wideCardHeight = wideCardHeight,
            widePosterStripWidth = widePosterStripWidth,
            wideContentPadding = 14.dp,
            posterCardWidth = posterCardStyle.widthDp.dp,
            posterCardHeight = posterCardStyle.heightDp.dp,
            progressHeight = 5.dp,
            posterTitleBlockHeight = 40.dp,
        )
        maxWidthDp >= 768f -> ContinueWatchingLayout(
            itemGap = 16.dp,
            wideCardWidth = wideCardWidth,
            wideCardHeight = wideCardHeight,
            widePosterStripWidth = widePosterStripWidth,
            wideContentPadding = 12.dp,
            posterCardWidth = posterCardStyle.widthDp.dp,
            posterCardHeight = posterCardStyle.heightDp.dp,
            progressHeight = 4.dp,
            posterTitleBlockHeight = 38.dp,
        )
        else -> ContinueWatchingLayout(
            itemGap = 16.dp,
            wideCardWidth = wideCardWidth,
            wideCardHeight = wideCardHeight,
            widePosterStripWidth = widePosterStripWidth,
            wideContentPadding = 12.dp,
            posterCardWidth = posterCardStyle.widthDp.dp,
            posterCardHeight = posterCardStyle.heightDp.dp,
            progressHeight = 4.dp,
            posterTitleBlockHeight = 38.dp,
        )
    }
}
