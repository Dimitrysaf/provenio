package io.github.dimitrysaf.provenio.core.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SeasonSourcesTest {

    @Test
    fun seasonPacksAreTheReleasesThatNameTheSeasonAlone() {
        assertTrue(stream(name = "FAMILY_GUY_S01.mp4").isSeasonPack(1))
        assertTrue(stream(description = "Family.Guy.S01.COMPLETE.1080p\nFamily.Guy.S01E01.mkv").isSeasonPack(1))
        assertTrue(stream(title = "Show Season 1 1080p").isSeasonPack(1))
        assertTrue(stream(title = "Show.S01-S05.1080p").isSeasonPack(3))
        assertTrue(stream(title = "Show Complete Series").isSeasonPack(4))
    }

    @Test
    fun singleEpisodesAndOtherSeasonsAreNotPacks() {
        assertFalse(stream(name = "Family.Guy.S01E01.1080p").isSeasonPack(1))
        assertFalse(stream(name = "Family Guy S01 E01").isSeasonPack(1))
        assertFalse(stream(name = "Show.1x01.720p").isSeasonPack(1))
        assertFalse(stream(name = "Family.Guy.S02.1080p").isSeasonPack(1))
        assertFalse(stream(name = "Show Season 1 Episode 3").isSeasonPack(1))
        assertFalse(stream(name = "Torrentio\n4k DTS 5.1").isSeasonPack(1))
    }

    @Test
    fun theSameTorrentIsTheMatchForAnotherEpisode() {
        val template = stream(name = "pack", infoHash = "AB".repeat(20), fileIdx = 0)
        val match = stream(name = "other name", infoHash = "ab".repeat(20), fileIdx = 4)
        val otherTorrent = stream(name = "x", infoHash = "cd".repeat(20))
        assertEquals(match, matchEpisodeStream(template, listOf(otherTorrent, match)))
    }

    @Test
    fun theSameReleaseNameWithAnotherEpisodeNumberIsTheMatch() {
        val template = stream(name = "Torrentio\n1080p", description = "Family.Guy.S01E01.1080p.WEB-DL.x264-GROUP\n👤 120 💾 1.2 GB")
        val same = stream(name = "Torrentio\n1080p", description = "Family.Guy.S01E02.1080p.WEB-DL.x264-GROUP\n👤 80 💾 1.1 GB")
        val other = stream(name = "Torrentio\n720p", description = "Family.Guy.S01E02.720p.HDTV.x265-OTHER\n👤 5 💾 300 MB")
        assertEquals(same, matchEpisodeStream(template, listOf(other, same)))
    }

    @Test
    fun nothingCloseEnoughIsNoMatch() {
        val template = stream(description = "Family.Guy.S01E01.1080p.WEB-DL.x264-GROUP")
        assertNull(matchEpisodeStream(template, listOf(stream(description = "Another.Show.S01E02.480p.CAM"))))
    }

    private fun stream(
        name: String? = null,
        title: String? = null,
        description: String? = null,
        infoHash: String? = null,
        fileIdx: Int? = null,
    ) = StreamItem(
        name = name,
        title = title,
        description = description,
        infoHash = infoHash,
        fileIdx = fileIdx,
        addonName = "Addon",
        addonId = "addon",
    )
}
