package io.github.dimitrysaf.provenio.core.home

import io.github.dimitrysaf.provenio.core.watch.progress.CachedNextUpItem
import io.github.dimitrysaf.provenio.core.watch.progress.parseReleaseDateToEpochMs

internal class CachedNextUpRelease(val item: CachedNextUpItem) {
    private val dependsOnTimeZone = item.released?.trim()?.let { value ->
        'T' in value &&
            !value.endsWith('Z') &&
            value.indexOf('+', startIndex = 10) < 0 &&
            value.indexOf('-', startIndex = 10) < 0
    } == true
    private val cachedEpochMs = if (dependsOnTimeZone) null else parseReleaseDateToEpochMs(item.released)

    fun epochMs(): Long? =
        if (dependsOnTimeZone) parseReleaseDateToEpochMs(item.released) else cachedEpochMs
}
