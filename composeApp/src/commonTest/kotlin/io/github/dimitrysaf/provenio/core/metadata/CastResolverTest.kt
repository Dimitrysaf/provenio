package io.github.dimitrysaf.provenio.core.metadata

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CastResolverTest {
    @Test
    fun readsTmdbCastEntries() {
        val root = Json.parseToJsonElement(
            """
            {"cast":[
              {"id":6384,"name":"Keanu Reeves","original_name":"Keanu Reeves","character":"Neo"},
              {"id":2975,"name":"Laurence Fishburne"},
              {"name":"No Id"},
              {"id":0,"name":"Zero Id"}
            ]}
            """.trimIndent(),
        )

        val entries = parseTmdbCastEntries(root)

        assertEquals(listOf(6384, 2975), entries.map { it.id })
        assertNull(entries[1].originalName)
    }

    @Test
    fun matchesNamesIgnoringCaseAndPunctuation() {
        val byName = matchableNames(
            listOf(
                TmdbCastEntry(id = 1, name = "Carrie-Anne Moss", originalName = null),
                TmdbCastEntry(id = 2, name = "Gong Yoo", originalName = "공유"),
                TmdbCastEntry(id = 3, name = "carrie anne moss", originalName = null),
            ),
        )

        assertEquals(1, byName[normalizePersonName("Carrie Anne Moss")])
        assertEquals(2, byName[normalizePersonName("공유")])
        assertEquals(2, byName[normalizePersonName("GONG YOO")])
        assertNull(byName[normalizePersonName("Hugo Weaving")])
    }
}
