package io.github.dimitrysaf.provenio.core.watch.watched

import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderId
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class WatchedItemsStore {
    private val lock = SynchronizedObject()
    private val accountItems = mutableMapOf<String, WatchedItem>()
    private val providerItems = mutableMapOf<TrackingProviderId, MutableMap<String, WatchedItem>>()
    private val dirtyAccountKeys = mutableSetOf<String>()
    private val dirtyProviderKeys = mutableMapOf<TrackingProviderId, MutableSet<String>>()

    fun <T> read(
        block: (
            accountItems: Map<String, WatchedItem>,
            providerItems: Map<TrackingProviderId, Map<String, WatchedItem>>,
            dirtyAccountKeys: Set<String>,
            dirtyProviderKeys: Map<TrackingProviderId, Set<String>>,
        ) -> T,
    ): T = synchronized(lock) {
        block(accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys)
    }

    fun <T> update(
        block: (
            accountItems: MutableMap<String, WatchedItem>,
            providerItems: MutableMap<TrackingProviderId, MutableMap<String, WatchedItem>>,
            dirtyAccountKeys: MutableSet<String>,
            dirtyProviderKeys: MutableMap<TrackingProviderId, MutableSet<String>>,
        ) -> T,
    ): T = synchronized(lock) {
        block(accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys)
    }
}
