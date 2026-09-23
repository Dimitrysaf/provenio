package com.nuvio.app.features.details.components

import com.nuvio.app.features.details.MetaVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpisodeListEntriesTest {

    private val grouped = mapOf(
        0 to listOf(episode(season = 0, number = 1)),
        2 to listOf(episode(season = 2, number = 1)),
        1 to listOf(episode(season = 1, number = 1), episode(season = 1, number = 2)),
    )

    @Test
    fun `every season starts collapsed with specials last`() {
        val entries = buildEpisodeListEntries(grouped, expandedSeasons = emptySet())

        assertEquals(listOf(1, 2, 0), entries.map { (it as EpisodeListEntry.Season).season })
        assertTrue(entries.all { it is EpisodeListEntry.Season && !it.expanded })
    }

    @Test
    fun `an expanded season lists its episodes right after its row`() {
        val entries = buildEpisodeListEntries(grouped, expandedSeasons = setOf(1))

        assertEquals(
            listOf("season-1", "episode-1-1-s1e1-0", "episode-1-2-s1e2-1", "season-2", "season-0"),
            entries.map { it.key },
        )
        assertEquals(2, (entries.first() as EpisodeListEntry.Season).episodeCount)
    }

    @Test
    fun `seasons without episodes are left out`() {
        val entries = buildEpisodeListEntries(mapOf(1 to emptyList()), expandedSeasons = setOf(1))

        assertTrue(entries.isEmpty())
    }

    private fun episode(season: Int, number: Int): MetaVideo =
        MetaVideo(
            id = "s${season}e$number",
            title = "Episode $number",
            season = season,
            episode = number,
        )
}
