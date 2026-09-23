package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MetaScreenSectionLayoutTest {

    private val groupedEpisodes = MetaScreenSectionItem(
        key = MetaScreenSectionKey.EPISODES,
        title = "Episodes",
        description = "Episode list",
        enabled = true,
        order = 0,
        tabGroup = 3,
    )

    @Test
    fun `episodes render outside a multi-section tab group`() {
        assertNull(groupedEpisodes.tabGroupForRendering())
    }

    @Test
    fun `other list sections retain their configured tab group`() {
        val groupedCast = groupedEpisodes.copy(key = MetaScreenSectionKey.CAST)

        assertEquals(3, groupedCast.tabGroupForRendering())
    }
}
