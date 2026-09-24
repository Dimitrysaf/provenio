package io.github.dimitrysaf.provenio.core.home

import io.github.dimitrysaf.provenio.core.collection.Collection
import provenio.composeapp.generated.resources.*

internal data class HomeCatalogPreference(
    val customTitle: String,
    val enabled: Boolean,
    val heroSourceEnabled: Boolean,
    val order: Int,
)

internal data class HomeCatalogSettingsSnapshot(
    val heroEnabled: Boolean,
    val showCatalogType: Boolean,
    val hideUnreleasedContent: Boolean,
    val preferences: Map<String, HomeCatalogPreference>,
)

internal fun visibleCollectionsWithUniqueIds(collections: List<Collection>): List<Collection> =
    collections
        .filter { collection -> collection.folders.isNotEmpty() }
        .distinctBy(Collection::id)
