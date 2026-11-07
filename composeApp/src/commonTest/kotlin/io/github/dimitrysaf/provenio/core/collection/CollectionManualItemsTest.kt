package io.github.dimitrysaf.provenio.core.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollectionManualItemsTest {
    private val ironMan = CollectionManualItem(id = "tt0371746", type = "movie", name = "Iron Man")
    private val thor = CollectionManualItem(id = "tt0800369", type = "movie", name = "Thor")

    @Test
    fun `the first title adds the hand-made source and the last one removes it`() {
        val empty = CollectionFolder(id = "mcu", title = "MCU")

        val one = empty.withManualItemToggled(ironMan)
        assertTrue(one.hasManualItem(ironMan.id))
        assertEquals(1, one.resolvedSources.count { it.isManual })

        val two = one.withManualItemToggled(thor)
        assertEquals(listOf(ironMan, thor), two.resolvedSources.single { it.isManual }.items)

        val none = two.withManualItemToggled(ironMan).withManualItemToggled(thor)
        assertFalse(none.hasManualItem(ironMan.id))
        assertTrue(none.resolvedSources.none { it.isManual })
    }

    @Test
    fun `the page lists the newest title first`() {
        val folder = CollectionFolder(id = "mcu", title = "MCU")
            .withManualItemToggled(ironMan)
            .withManualItemToggled(thor)
        val page = folder.resolvedSources.single { it.isManual }.manualPage()
        assertEquals(listOf("Thor", "Iron Man"), page.items.map { it.name })
    }
}
