package io.github.dimitrysaf.provenio.core.watch.progress

import io.github.dimitrysaf.provenio.core.tracking.WatchProgressSource
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchProgressSourceProjectionTest {
    @Test
    fun `remote source excludes every Provenio progress entry`() {
        val accountEntries = listOf(
            entry(parentMetaId = "shared", updatedAt = 200L),
            entry(parentMetaId = "provenio-only", updatedAt = 300L),
        )
        val providerEntries = listOf(
            entry(parentMetaId = "shared", updatedAt = 100L),
        )

        listOf(WatchProgressSource.TRAKT, WatchProgressSource.SIMKL).forEach { source ->
            val projected = projectWatchProgressSourceEntries(
                source = source,
                accountEntries = accountEntries,
                providerEntries = providerEntries,
            )

            assertEquals(providerEntries, projected)
        }
    }

    @Test
    fun `Provenio source excludes every provider progress entry`() {
        val accountEntries = listOf(entry(parentMetaId = "provenio"))
        val providerEntries = listOf(entry(parentMetaId = "provider"))

        val projected = projectWatchProgressSourceEntries(
            source = WatchProgressSource.ACCOUNT_SYNC,
            accountEntries = accountEntries,
            providerEntries = providerEntries,
        )

        assertEquals(accountEntries, projected)
    }

    private fun entry(
        parentMetaId: String,
        updatedAt: Long = 1L,
    ): WatchProgressEntry = WatchProgressEntry(
        contentType = "series",
        parentMetaId = parentMetaId,
        parentMetaType = "series",
        videoId = "$parentMetaId:1:1",
        title = parentMetaId,
        seasonNumber = 1,
        episodeNumber = 1,
        lastPositionMs = 10L,
        durationMs = 100L,
        lastUpdatedEpochMs = updatedAt,
    )
}
