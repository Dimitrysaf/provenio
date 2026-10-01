package io.github.dimitrysaf.provenio.core.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamSizeOrderTest {
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
        val largest = listOf(group).sortedBySize(StreamSizeOrder.LARGEST_FIRST).single().streams.map { it.name }
        val smallest = listOf(group).sortedBySize(StreamSizeOrder.SMALLEST_FIRST).single().streams.map { it.name }
        val unchanged = listOf(group).sortedBySize(StreamSizeOrder.DEFAULT).single().streams.map { it.name }

        assertEquals(listOf("big", "mid", "small", "none"), largest)
        assertEquals(listOf("small", "mid", "big", "none"), smallest)
        assertEquals(listOf("mid", "none", "big", "small"), unchanged)
    }
}
