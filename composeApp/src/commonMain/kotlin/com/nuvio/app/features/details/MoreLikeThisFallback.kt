package com.nuvio.app.features.details

import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.stableKey

private const val MORE_LIKE_THIS_FALLBACK_LIMIT = 20

/** Home catalog titles standing in when no service recommended any: same genre first, otherwise as listed. */
internal fun moreLikeThisFallback(
    meta: MetaDetails,
    sections: List<HomeCatalogSection>,
): List<MetaPreview> {
    val candidates = sections
        .flatMap { it.items }
        .filter { it.id != meta.id && it.type.equals(meta.type, ignoreCase = true) }
        .distinctBy { it.stableKey() }
    val genres = meta.genres.map { it.lowercase() }.toSet()
    val sameGenre = candidates.filter { item -> item.genres.any { it.lowercase() in genres } }
    return sameGenre.ifEmpty { candidates }.take(MORE_LIKE_THIS_FALLBACK_LIMIT)
}
