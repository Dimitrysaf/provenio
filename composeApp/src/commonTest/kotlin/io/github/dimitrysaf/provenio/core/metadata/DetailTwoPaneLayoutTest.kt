package io.github.dimitrysaf.provenio.core.metadata

import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.ACTIONS
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.CAST
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.COMMENTS
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.DETAILS
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.EPISODES
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.MORE_LIKE_THIS
import io.github.dimitrysaf.provenio.core.metadata.MetaScreenSectionKey.OVERVIEW
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailTwoPaneLayoutTest {

    @Test
    fun `episodes always take the side pane`() {
        assertEquals(EPISODES, detailSidePaneSection(listOf(ACTIONS, OVERVIEW, EPISODES, DETAILS, MORE_LIKE_THIS)))
    }

    @Test
    fun `more like this takes the side pane without episodes`() {
        assertEquals(MORE_LIKE_THIS, detailSidePaneSection(listOf(ACTIONS, OVERVIEW, COMMENTS, DETAILS, MORE_LIKE_THIS)))
    }

    @Test
    fun `the lowest remaining section climbs up`() {
        assertEquals(DETAILS, detailSidePaneSection(listOf(ACTIONS, OVERVIEW, CAST, COMMENTS, DETAILS)))
        assertEquals(COMMENTS, detailSidePaneSection(listOf(ACTIONS, OVERVIEW, CAST, COMMENTS)))
    }

    @Test
    fun `the header sections never move`() {
        assertNull(detailSidePaneSection(listOf(ACTIONS, OVERVIEW)))
    }
}
