package io.github.dimitrysaf.provenio.core.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamSortOrderTest {
    private fun stream(name: String, description: String? = null, videoSize: Long? = null) = StreamItem(
        name = name,
        description = description,
        addonName = "Addon",
        addonId = "addon",
        behaviorHints = StreamBehaviorHints(videoSize = videoSize),
    )

    @Test
    fun sizeComesFromTheHintBeforeTheText() {
        assertEquals(2_000L, stream("a", "💾 5 GB", videoSize = 2_000L).sizeBytes())
    }

    @Test
    fun sizeIsReadFromTheDescription() {
        assertEquals((1.5 * 1024 * 1024 * 1024).toLong(), stream("a", "1080p 💾 1,5 GB").sizeBytes())
        assertEquals(700L * 1024 * 1024, stream("a", "700 MiB").sizeBytes())
        assertNull(stream("a", "1080p x265 320 kbps").sizeBytes())
    }

    @Test
    fun ordersEachGroupBySizeWithUnknownSizesLast() {
        val group = AddonStreamGroup(
            addonName = "Addon",
            addonId = "addon",
            streams = listOf(stream("mid", "2 GB"), stream("none"), stream("big", "10 GB"), stream("small", "500 MB")),
        )
        val largest = listOf(group).sortedByOrder(StreamSortOrder.LARGEST_FIRST).single().streams.map { it.name }
        val smallest = listOf(group).sortedByOrder(StreamSortOrder.SMALLEST_FIRST).single().streams.map { it.name }
        val unchanged = listOf(group).sortedByOrder(StreamSortOrder.DEFAULT).single().streams.map { it.name }

        assertEquals(listOf("big", "mid", "small", "none"), largest)
        assertEquals(listOf("small", "mid", "big", "none"), smallest)
        assertEquals(listOf("mid", "none", "big", "small"), unchanged)
    }

    @Test
    fun seedsAreReadFromTheDescription() {
        assertEquals(523, stream("a", "👤 523 💾 2.1 GB ⚙️ ThePirateBay").seeds())
        assertEquals(12, stream("a", "👥 12").seeds())
        assertEquals(40, stream("a", "1080p Seeders: 40").seeds())
        assertNull(stream("The Bad Seed 2018", "1080p 💾 2 GB").seeds())
    }

    @Test
    fun ordersEachGroupBySeedsWithUnknownSeedsLast() {
        val group = AddonStreamGroup(
            addonName = "Addon",
            addonId = "addon",
            streams = listOf(stream("mid", "👤 50"), stream("none"), stream("most", "👤 900"), stream("few", "👤 3")),
        )
        val most = listOf(group).sortedByOrder(StreamSortOrder.MOST_SEEDS).single().streams.map { it.name }
        val fewest = listOf(group).sortedByOrder(StreamSortOrder.FEWEST_SEEDS).single().streams.map { it.name }

        assertEquals(listOf("most", "mid", "few", "none"), most)
        assertEquals(listOf("few", "mid", "most", "none"), fewest)
    }

    @Test
    fun peersAreReadFromTheDescription() {
        assertEquals(8, stream("a", "1080p Peers: 8").peers())
        assertEquals(15, stream("a", "Seeders: 40 Leechers: 15").peers())
        assertNull(stream("Peer Gynt", "1080p 💾 2 GB").peers())
    }

    @Test
    fun ordersEachGroupByPeersWithUnknownPeersLast() {
        val group = AddonStreamGroup(
            addonName = "Addon",
            addonId = "addon",
            streams = listOf(stream("mid", "Peers: 50"), stream("none"), stream("most", "Peers: 900"), stream("few", "Peers: 3")),
        )
        val most = listOf(group).sortedByOrder(StreamSortOrder.MOST_PEERS).single().streams.map { it.name }
        val fewest = listOf(group).sortedByOrder(StreamSortOrder.FEWEST_PEERS).single().streams.map { it.name }

        assertEquals(listOf("most", "mid", "few", "none"), most)
        assertEquals(listOf("few", "mid", "most", "none"), fewest)
    }
}
