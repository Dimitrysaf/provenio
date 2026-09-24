package io.github.dimitrysaf.provenio.core.watch.progress

import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource

internal fun projectWatchProgressSourceEntries(
    source: WatchProgressSource,
    accountEntries: Collection<WatchProgressEntry>,
    providerEntries: Collection<WatchProgressEntry>,
): List<WatchProgressEntry> = if (source.providerId == null) {
    accountEntries.toList()
} else {
    providerEntries.toList()
}
