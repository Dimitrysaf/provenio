package io.github.dimitrysaf.provenio.core.collection

import androidx.compose.runtime.Immutable
import io.github.dimitrysaf.provenio.core.catalog.CatalogPage
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.PosterShape
import kotlinx.serialization.Serializable

/** A title put in a folder by hand: enough of it to draw its poster and open its page. */
@Immutable
@Serializable
data class CollectionManualItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val releaseInfo: String? = null,
)

fun MetaPreview.toManualItem(): CollectionManualItem =
    CollectionManualItem(id = id, type = type, name = name, poster = poster, releaseInfo = releaseInfo)

/** The hand-made source's titles as one page, newest first, the way a list someone keeps reads. */
internal fun CollectionSource.manualPage(): CatalogPage {
    val previews = items.orEmpty().asReversed().map { item ->
        MetaPreview(
            id = item.id,
            type = item.type,
            name = item.name,
            poster = item.poster,
            posterShape = PosterShape.Poster,
            releaseInfo = item.releaseInfo,
        )
    }
    return CatalogPage(items = previews, rawItemCount = previews.size, nextSkip = null)
}

/** Whether the folder holds [itemId] among the titles put in it by hand. */
fun CollectionFolder.hasManualItem(itemId: String): Boolean =
    resolvedSources.any { source -> source.isManual && source.items.orEmpty().any { it.id == itemId } }

/** The folder with [item] put in by hand, or taken out when it already was. */
internal fun CollectionFolder.withManualItemToggled(item: CollectionManualItem): CollectionFolder {
    val sources = resolvedSources
    val manual = sources.firstOrNull { it.isManual }
    val items = manual?.items.orEmpty()
    val nextItems = if (items.any { it.id == item.id }) items.filterNot { it.id == item.id } else items + item
    val nextSources = when {
        manual == null -> sources + CollectionSource(provider = "manual", items = nextItems)
        // The last title out takes the hand-made source with it, so the folder has no empty tab.
        nextItems.isEmpty() -> sources.filterNot { it.isManual }
        else -> sources.map { if (it.isManual) it.copy(items = nextItems) else it }
    }
    return copy(sources = nextSources, catalogSources = nextSources.mapNotNull { it.addonCatalogSource() })
}

object CollectionManualItems {
    /** Puts [item] in the folder by hand, or takes it out when it is there, and saves the collection. */
    fun toggle(collectionId: String, folderId: String, item: CollectionManualItem) {
        val collection = CollectionRepository.getCollection(collectionId) ?: return
        CollectionRepository.updateCollection(
            collection.copy(
                folders = collection.folders.map { folder ->
                    if (folder.id == folderId) folder.withManualItemToggled(item) else folder
                },
            ),
        )
    }
}
