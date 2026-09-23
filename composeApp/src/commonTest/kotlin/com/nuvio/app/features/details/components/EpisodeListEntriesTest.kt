package com.nuvio.app.features.details.components

import com.nuvio.app.core.metadata.MetaVideo
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun `completed seasons are flagged on their row`() {
        val entries = buildEpisodeListEntries(grouped, expandedSeasons = emptySet(), completedSeasons = setOf(2))

        assertEquals(listOf(false, true, false), entries.map { (it as EpisodeListEntry.Season).completed })
    }

    @Test
    fun `nothing watched opens the first season`() {
        val summary = summarize(watched = emptySet())

        assertEquals(1, summary.defaultSeason)
        assertTrue(summary.completedSeasons.isEmpty())
    }

    @Test
    fun `a finished season opens the next one`() {
        val summary = summarize(watched = setOf("s1e1", "s1e2"))

        assertEquals(2, summary.defaultSeason)
        assertEquals(setOf(1), summary.completedSeasons)
    }

    @Test
    fun `a partly watched season stays open`() {
        val summary = summarize(watched = setOf("s1e1"))

        assertEquals(1, summary.defaultSeason)
    }

    @Test
    fun `the season being watched wins`() {
        val summary = summarize(watched = setOf("s2e1"), inProgress = mapOf("s1e2" to 10L, "s1e1" to 5L))

        assertEquals(1, summary.defaultSeason)
        assertEquals(setOf(2), summary.completedSeasons)
    }

    @Test
    fun `nothing opens once the last season is finished`() {
        val summary = summarize(watched = setOf("s1e1", "s1e2", "s2e1"))

        assertNull(summary.defaultSeason)
    }

    private fun summarize(
        watched: Set<String>,
        inProgress: Map<String, Long> = emptyMap(),
    ): EpisodeSeasonSummary =
        summarizeEpisodeSeasons(grouped, todayIsoDate = "2026-01-01") { episode ->
            EpisodeWatchState(
                isWatched = episode.id in watched,
                inProgress = inProgress[episode.id]?.let { updatedAt -> progress(episode.id, updatedAt) },
            )
        }

    private fun progress(videoId: String, updatedAt: Long): WatchProgressEntry =
        WatchProgressEntry(
            contentType = "series",
            parentMetaId = "tt1",
            parentMetaType = "series",
            videoId = videoId,
            title = "Show",
            lastPositionMs = 60_000L,
            durationMs = 120_000L,
            lastUpdatedEpochMs = updatedAt,
        )

    private fun episode(season: Int, number: Int): MetaVideo =
        MetaVideo(
            id = "s${season}e$number",
            title = "Episode $number",
            season = season,
            episode = number,
        )
}
