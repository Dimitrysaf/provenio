package io.github.dimitrysaf.provenio.core.tracking

import io.github.dimitrysaf.provenio.core.library.LibrarySourceMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrackingSourcesTest {
    @Test
    fun `stored legacy source names retain their meaning`() {
        assertEquals(WatchProgressSource.TRAKT, WatchProgressSource.fromStorage("TRAKT"))
        assertEquals(WatchProgressSource.ACCOUNT_SYNC, WatchProgressSource.fromStorage("ACCOUNT_SYNC"))
        assertEquals(LibrarySourceMode.TRAKT, librarySourceModeFromStorage("TRAKT"))
    }

    @Test
    fun `remote watch source falls back when its provider is disconnected`() {
        assertEquals(
            WatchProgressSource.ACCOUNT_SYNC,
            effectiveWatchProgressSource(WatchProgressSource.SIMKL) { false },
        )
        assertEquals(
            WatchProgressSource.SIMKL,
            effectiveWatchProgressSource(WatchProgressSource.SIMKL) { it == TrackingProviderId.SIMKL },
        )
    }

    @Test
    fun `remote library source falls back to local when disconnected`() {
        assertEquals(
            LibrarySourceMode.LOCAL,
            effectiveLibrarySourceMode(LibrarySourceMode.SIMKL) { false },
        )
        assertEquals(
            LibrarySourceMode.SIMKL,
            effectiveLibrarySourceMode(LibrarySourceMode.SIMKL) { it == TrackingProviderId.SIMKL },
        )
    }

    @Test
    fun `local sources have no remote provider`() {
        assertNull(WatchProgressSource.ACCOUNT_SYNC.providerId)
        assertNull(LibrarySourceMode.LOCAL.providerId)
    }
}
