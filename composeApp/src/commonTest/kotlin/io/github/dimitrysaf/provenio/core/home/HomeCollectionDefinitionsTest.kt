package io.github.dimitrysaf.provenio.core.home

import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.core.collection.CollectionFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCollectionDefinitionsTest {
    @Test
    fun `visible home collections ignore duplicate IDs and empty collections`() {
        val visibleFolder = CollectionFolder(id = "folder", title = "Folder")
        val collections = listOf(
            Collection(id = "duplicate", title = "First", folders = listOf(visibleFolder)),
            Collection(id = "empty", title = "Empty"),
            Collection(id = "duplicate", title = "Second", folders = listOf(visibleFolder)),
            Collection(id = "other", title = "Other", folders = listOf(visibleFolder)),
        )

        val result = visibleCollectionsWithUniqueIds(collections)

        assertEquals(listOf("duplicate", "other"), result.map(Collection::id))
        assertEquals("First", result.first().title)
    }
}
