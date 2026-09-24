package io.github.dimitrysaf.provenio.core.watch.watched

import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchedItemsStoreTest {
    @Test
    fun `concurrent updates publish coherent item snapshots`() = runBlocking {
        val store = WatchedItemsStore()

        coroutineScope {
            repeat(4) { writer ->
                launch(Dispatchers.Default) {
                    repeat(500) { index ->
                        val key = "$writer:$index"
                        val item = WatchedItem(
                            id = key,
                            type = "movie",
                            name = key,
                            markedAtEpochMs = index.toLong(),
                        )
                        store.update { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
                            accountItems[key] = item
                            providerItems
                                .getOrPut(TrackingProviderId.TRAKT, ::mutableMapOf)[key] = item
                            dirtyAccountKeys += key
                            dirtyProviderKeys
                                .getOrPut(TrackingProviderId.TRAKT, ::mutableSetOf) += key
                        }
                    }
                }
            }
            repeat(4) {
                launch(Dispatchers.Default) {
                    repeat(500) {
                        store.read { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
                            val accountKeys = accountItems.keys.toSet()
                            assertEquals(accountKeys, providerItems[TrackingProviderId.TRAKT].orEmpty().keys)
                            assertEquals(accountKeys, dirtyAccountKeys)
                            assertEquals(accountKeys, dirtyProviderKeys[TrackingProviderId.TRAKT])
                        }
                    }
                }
            }
        }

        store.read { accountItems, providerItems, dirtyAccountKeys, dirtyProviderKeys ->
            assertEquals(2_000, accountItems.size)
            assertEquals(accountItems.keys, providerItems[TrackingProviderId.TRAKT].orEmpty().keys)
            assertEquals(accountItems.keys, dirtyAccountKeys)
            assertEquals(accountItems.keys, dirtyProviderKeys[TrackingProviderId.TRAKT])
        }
    }
}
