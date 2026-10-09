package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.provenio.core.i18n.localizedSeasonEpisodeCode
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.MetaVideo
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.ShapedArtworkImage
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailAdditionalInfoSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailCastSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailMetaInfo
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailParentsGuideSection
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailProductionSection
import io.github.dimitrysaf.provenio.shell.screens.details.rememberDetailParentalWarnings
import io.github.dimitrysaf.provenio.shell.screens.details.rememberDetailSectionContent
import io.github.dimitrysaf.provenio.shell.screens.home.components.HeroMinSmallItemWidth
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.player_info_title

/**
 * What is playing, told with the Details page's own pieces: its picture carousel, then the
 * Overview, Parental Guide, Production, Cast and Details sections, in the order and with the
 * choices made for the Details page. Shows the episode when an episode is playing, with the
 * show's cast, production and details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerInfoSheet(
    title: String,
    meta: MetaDetails?,
    episode: MetaVideo?,
    poster: String?,
    background: String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState, onDismiss) } }
    val settings by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()

    val showName = meta?.name?.takeIf(String::isNotBlank) ?: title
    val heading = episode?.title?.takeIf(String::isNotBlank) ?: showName
    val subtitle = episode?.let {
        listOfNotNull(
            showName,
            localizedSeasonEpisodeCode(seasonNumber = it.season, episodeNumber = it.episode),
        ).joinToString(" · ")
    }
    val images = remember(episode, background, meta, poster) {
        val wide = (listOf(episode?.thumbnail, background, meta?.background) + meta?.extraArtwork.orEmpty())
            .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
        wide.ifEmpty { listOfNotNull(poster, meta?.poster).filter(String::isNotBlank) }
    }
    val sectionMeta = remember(meta, episode) { meta?.let { show -> episode?.let(show::asEpisode) ?: show } }
    val sections = settings.items.filter { it.enabled && it.key in PlayerInfoSectionKeys }

    ModalSheet(
        onDismissRequest = close,
        sheetState = sheetState,
    ) {
        SheetHeader(
            title = stringResource(Res.string.player_info_title),
            navigation = SheetNavigation.Close,
            onNavigate = close,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = safeBottomPadding(24.dp)),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (images.isNotEmpty()) {
                    PlayerInfoCarousel(images = images, contentDescription = heading)
                }
                Column(
                    modifier = Modifier.padding(horizontal = BottomSheetBodyMargin),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (sectionMeta != null) {
                PlayerInfoSections(
                    meta = sectionMeta,
                    keys = sections.map { it.key },
                    parentsGuideEnabled = sections.any { it.key == MetaScreenSectionKey.PARENTS_GUIDE },
                )
            }
        }
    }
}

/** The Details page sections that tell what is playing, without the ones that lead away from it. */
private val PlayerInfoSectionKeys = setOf(
    MetaScreenSectionKey.OVERVIEW,
    MetaScreenSectionKey.PARENTS_GUIDE,
    MetaScreenSectionKey.PRODUCTION,
    MetaScreenSectionKey.CAST,
    MetaScreenSectionKey.DETAILS,
)

/** The chosen Details page sections for [meta], each drawn by the Details page's own component. */
@Composable
private fun PlayerInfoSections(
    meta: MetaDetails,
    keys: List<MetaScreenSectionKey>,
    parentsGuideEnabled: Boolean,
) {
    val content = rememberDetailSectionContent(meta, emptyList())
    val parentalWarnings = rememberDetailParentalWarnings(meta, enabled = parentsGuideEnabled).warnings
    val sectionModifier = Modifier.padding(horizontal = BottomSheetBodyMargin)
    keys.forEach { key ->
        when (key) {
            MetaScreenSectionKey.OVERVIEW -> DetailMetaInfo(
                meta = meta,
                modifier = sectionModifier,
                horizontalScrollPadding = BottomSheetBodyMargin,
            )
            MetaScreenSectionKey.PARENTS_GUIDE -> if (parentalWarnings.isNotEmpty()) {
                DetailParentsGuideSection(warnings = parentalWarnings, modifier = sectionModifier)
            }
            MetaScreenSectionKey.PRODUCTION -> if (content.hasProduction) {
                DetailProductionSection(meta = meta, modifier = sectionModifier)
            }
            MetaScreenSectionKey.CAST -> if (meta.cast.isNotEmpty()) {
                DetailCastSection(
                    cast = meta.cast,
                    modifier = sectionModifier,
                    horizontalScrollPadding = BottomSheetBodyMargin,
                    metaId = meta.id,
                    metaType = meta.type,
                )
            }
            MetaScreenSectionKey.DETAILS -> if (content.hasAdditionalInfo) {
                DetailAdditionalInfoSection(meta = meta, modifier = sectionModifier)
            }
            else -> Unit
        }
    }
}

/** The playing title's pictures in the Details page's hero carousel, or the one picture there is. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerInfoCarousel(
    images: List<String>,
    contentDescription: String,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemWidth = maxWidth - BottomSheetBodyMargin * 2
        val height = itemWidth * (9f / 16f)
        if (images.size == 1) {
            Box(
                modifier = Modifier
                    .padding(horizontal = BottomSheetBodyMargin)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.extraLarge),
            ) {
                ShapedArtworkImage(
                    candidates = images,
                    contentDescription = contentDescription,
                    letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    showSkeleton = true,
                )
            }
        } else {
            val carouselState = rememberCarouselState(itemCount = { images.size })
            HorizontalCenteredHeroCarousel(
                state = carouselState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                itemSpacing = 8.dp,
                minSmallItemWidth = HeroMinSmallItemWidth,
                maxSmallItemWidth = 40.dp,
                contentPadding = PaddingValues(horizontal = BottomSheetBodyMargin),
            ) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                ) {
                    ShapedArtworkImage(
                        candidates = listOf(images[index]),
                        contentDescription = contentDescription,
                        letterboxColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        showSkeleton = true,
                    )
                }
            }
        }
    }
}

/**
 * The show seen as one of its episodes, for the Overview: the episode's own title, overview,
 * date, length and rating. Its cast, production and details stay the show's, and the show's
 * ratings from other sites are left out because they are not the episode's.
 */
private fun MetaDetails.asEpisode(episode: MetaVideo): MetaDetails = copy(
    name = episode.title.takeIf(String::isNotBlank) ?: name,
    type = "episode",
    description = episode.overview?.trim()?.takeIf(String::isNotBlank) ?: description,
    releaseInfo = episode.released?.takeIf(String::isNotBlank),
    lastAirDate = null,
    runtime = episode.runtime?.takeIf { it > 0 }?.let { "$it min" } ?: runtime,
    imdbRating = episode.rating?.takeIf { it > 0.0 }?.let { ((it * 10).toInt() / 10.0).toString() },
    externalRatings = emptyList(),
)
