package io.github.dimitrysaf.provenio.core.tracking.simkl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimklListIdTest {
    @Test
    fun `list links and bare ids give the list id`() {
        assertEquals(2707L, SimklPublicListSourceResolver.parseListId("https://simkl.com/5/list/2707/marvel-cinematic-universe-mcu-movies"))
        assertEquals(2707L, SimklPublicListSourceResolver.parseListId("simkl.com/list/2707"))
        assertEquals(2707L, SimklPublicListSourceResolver.parseListId(" 2707 "))
    }

    @Test
    fun `anything else gives no id`() {
        assertNull(SimklPublicListSourceResolver.parseListId("https://simkl.com/movies/123/iron-man"))
        assertNull(SimklPublicListSourceResolver.parseListId("marvel"))
        assertNull(SimklPublicListSourceResolver.parseListId("0"))
    }
}
