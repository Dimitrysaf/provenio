package io.github.dimitrysaf.provenio.core.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmbeddedStreamsTest {
    @Test
    fun namesKnownSourcesAndFallsBackToTheHost() {
        assertEquals("YouTube", embeddedSourceName("https://www.youtube.com/watch?v=abc"))
        assertEquals("YouTube", embeddedSourceName("https://youtu.be/abc"))
        assertEquals("YouTube", embeddedSourceName("https://m.youtube.com/watch?v=abc"))
        assertEquals("Vimeo", embeddedSourceName("https://player.vimeo.com/video/1"))
        assertEquals("Internet Archive", embeddedSourceName("https://archive.org/download/x/x.mp4"))
        assertEquals("bbc.co.uk", embeddedSourceName("https://www.bbc.co.uk:443/iplayer/episode/1"))
        assertNull(embeddedSourceName("magnet:?xt=urn:btih:abc"))
        assertNull(embeddedSourceName(null))
    }

    @Test
    fun groupsStreamsBySource() {
        val groups = embeddedStreamGroups(
            listOf(
                stream("YouTube", "https://youtu.be/a"),
                stream("Vimeo", "https://vimeo.com/1"),
                stream("YouTube", "https://youtu.be/b"),
            ),
        )

        assertEquals(listOf("YouTube", "Vimeo"), groups.map { it.addonName })
        assertEquals(2, groups.first().streams.size)
        assertTrue(groups.all { it.isEmbeddedSource })
        assertTrue(groups.first().streams.all { it.addonId == groups.first().addonId })
    }

    @Test
    fun embeddedSourcesStayFirstInTheList() {
        val embedded = embeddedStreamGroups(listOf(stream("YouTube", "https://youtu.be/a")))
        val addon = AddonStreamGroup(addonName = "Torrentio", addonId = "addon:torrentio", streams = emptyList())

        val ordered = StreamAutoPlaySelector.orderAddonStreams(listOf(addon) + embedded, listOf("Torrentio"))

        assertEquals(listOf("YouTube", "Torrentio"), ordered.map { it.addonName })
    }

    @Test
    fun specialsAutoPlayTheEmbeddedSource() {
        val groups = embeddedStreamGroups(listOf(stream("YouTube", "https://youtu.be/a"))) +
            AddonStreamGroup(addonName = "Torrentio", addonId = "addon:torrentio", streams = emptyList(), isLoading = true)

        assertEquals(1, groups.embeddedAutoPlayStreams(season = 0).size)
        assertTrue(groups.embeddedAutoPlayStreams(season = 1).isEmpty())
    }

    @Test
    fun regularEpisodesFallBackToEmbeddedOnlyWhenNothingElseFound() {
        val embedded = embeddedStreamGroups(listOf(stream("YouTube", "https://youtu.be/a")))
        val emptyAddon = AddonStreamGroup(addonName = "Torrentio", addonId = "addon:torrentio", streams = emptyList())
        val addonWithStream = emptyAddon.copy(streams = listOf(stream("Torrentio", "https://example.com/a.mkv")))

        assertEquals(1, (embedded + emptyAddon).embeddedAutoPlayStreams(season = 1).size)
        assertTrue((embedded + addonWithStream).embeddedAutoPlayStreams(season = 1).isEmpty())
    }

    private fun stream(addonName: String, url: String) = StreamItem(
        name = addonName,
        url = url,
        addonName = addonName,
        addonId = EmbeddedSourceAddonId,
    )
}
