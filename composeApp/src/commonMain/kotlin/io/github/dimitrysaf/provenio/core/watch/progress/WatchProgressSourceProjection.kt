package io.github.dimitrysaf.provenio.core.watch.progress

import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource

// Local sync has priority: with a provider as the source, Provenio's own progress replaces the provider's entry for the same item, and the provider only fills in what Provenio lacks.
internal fun projectWatchProgressSourceEntries(
    source: WatchProgressSource,
    accountEntries: Collection<WatchProgressEntry>,
    providerEntries: Collection<WatchProgressEntry>,
): List<WatchProgressEntry> {
    if (source.providerId == null) return accountEntries.toList()
    val merged = providerEntries.associateByTo(linkedMapOf()) { it.resolvedProgressKey() }
    accountEntries.forEach { local -> merged[local.resolvedProgressKey()] = local }
    return merged.values.toList()
}
