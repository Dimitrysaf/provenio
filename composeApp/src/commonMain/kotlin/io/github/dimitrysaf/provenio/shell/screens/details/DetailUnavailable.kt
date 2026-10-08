package io.github.dimitrysaf.provenio.shell.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSection
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.shell.screens.details.components.DetailSectionSkeleton

/** What is known about a title without asking its add-ons: what the device already holds. */
internal data class DetailPlaceholder(
    val name: String,
    val subtitle: String?,
    val images: List<String>,
)

/**
 * Builds the title's placeholder from the library, watch progress and the Home rows, in that
 * order. Without any of them the ID stands in for the name, since it is all that is known.
 */
internal fun detailPlaceholderFor(
    type: String,
    id: String,
    libraryItems: List<LibraryItem>,
    progressEntries: List<WatchProgressEntry>,
    homeSections: List<HomeCatalogSection>,
): DetailPlaceholder {
    val libraryItem = libraryItems.firstOrNull { it.id == id && it.type == type }
    val progressEntry = progressEntries
        .filter { it.parentMetaId == id }
        .maxByOrNull { it.lastUpdatedEpochMs }
    val preview = homeSections.firstNotNullOfOrNull { section ->
        section.items.firstOrNull { it.id == id && it.type == type }
    }

    val name = listOfNotNull(libraryItem?.name, progressEntry?.title, preview?.name)
        .firstOrNull(String::isNotBlank)
    val images = listOfNotNull(
        libraryItem?.banner,
        progressEntry?.background,
        preview?.banner,
        libraryItem?.poster,
        progressEntry?.poster,
        preview?.poster,
    ).filter(String::isNotBlank).distinct()

    return DetailPlaceholder(
        name = name ?: id,
        subtitle = listOfNotNull(libraryItem?.releaseInfo, preview?.releaseInfo)
            .firstOrNull(String::isNotBlank),
        images = images,
    )
}

/**
 * The Details page for a title whose details could not load: the page's own layout, with the
 * artwork and name the device already has and skeletons where the add-on's details would go, so
 * the error dialog sits over the page it is about instead of replacing it.
 */
@Composable
internal fun DetailUnavailablePage(
    type: String,
    id: String,
    placeholder: DetailPlaceholder,
    placeholderIcon: ImageVector,
    onBack: () -> Unit,
) {
    DetailPage(
        pageKey = "meta-unavailable-$type:$id",
        name = placeholder.name,
        subtitle = placeholder.subtitle,
        images = placeholder.images,
        deceased = false,
        onBack = onBack,
        placeholderIcon = placeholderIcon,
        infoItems = { metrics ->
            listOf(MetaScreenSectionKey.OVERVIEW, MetaScreenSectionKey.DETAILS).forEach { key ->
                item(key = "meta-unavailable-${key.name}") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = metrics.contentMaxWidth,
                    ) {
                        DetailSectionSkeleton(key = key, horizontalScrollPadding = metrics.horizontalPadding)
                    }
                }
            }
        },
        railItems = { metrics ->
            listOf(MetaScreenSectionKey.CAST, MetaScreenSectionKey.MORE_LIKE_THIS).forEach { key ->
                item(key = "meta-unavailable-${key.name}") {
                    DetailSectionContainer(
                        horizontalPadding = metrics.horizontalPadding,
                        contentMaxWidth = Dp.Unspecified,
                    ) {
                        DetailSectionSkeleton(key = key, horizontalScrollPadding = metrics.horizontalPadding)
                    }
                }
            }
        },
    )
}
