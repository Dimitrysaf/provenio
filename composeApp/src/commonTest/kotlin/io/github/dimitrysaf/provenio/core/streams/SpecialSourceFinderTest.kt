package io.github.dimitrysaf.provenio.core.streams

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecialSourceFinderTest {
    @Test
    fun readsVideosFromAYouTubeSearchResponse() {
        val response = Json.parseToJsonElement(
            """
            {"contents":{"twoColumnSearchResultsRenderer":{"primaryContents":{"sectionListRenderer":{"contents":[
              {"itemSectionRenderer":{"contents":[
                {"videoRenderer":{"videoId":"abc123","title":{"runs":[{"text":"The Mentalist Revealed"}]},
                  "ownerText":{"runs":[{"text":"Some Channel"}]},"lengthText":{"simpleText":"21:40"}}},
                {"adSlotRenderer":{}},
                {"videoRenderer":{"videoId":"def456","title":{"runs":[{"text":"Mentalist "},{"text":"S01E01 Pilot"}]}}}
              ]}}
            ]}}}}}
            """.trimIndent(),
        )

        val videos = parseYouTubeSearch(response)

        assertEquals(listOf("abc123", "def456"), videos.map { it.videoId })
        assertEquals("The Mentalist Revealed", videos.first().title)
        assertEquals("Some Channel · 21:40", videos.first().detail)
        assertEquals("Mentalist S01E01 Pilot", videos[1].title)
        assertNull(videos[1].detail)
    }

    @Test
    fun keepsOnlyResultsNamingTheSeriesAndTheSpecial() {
        val results = listOf(
            FoundVideo("a", "The Mentalist Revealed (2009)", null),
            FoundVideo("b", "The Mentalist Season 1 Episode 1", null),
            FoundVideo("c", "Mentalist revealed - behind the scenes", null),
            FoundVideo("d", "Revealed: a documentary", null),
        )

        val matches = matchingSearchResults(results, seriesName = "The Mentalist", specialTitle = "The Mentalist Revealed")

        assertEquals(listOf("a", "c"), matches.map { it.videoId })
    }

    @Test
    fun skipsTitlesThatSayNothingAboutTheSpecial() {
        assertTrue(isGenericEpisodeTitle("Episode 3"))
        assertTrue(isGenericEpisodeTitle("Special"))
        assertTrue(isGenericEpisodeTitle(""))
        assertFalse(isGenericEpisodeTitle("The Mentalist Revealed"))
    }

    @Test
    fun searchesWithoutRepeatingTheSeriesName() {
        assertEquals("The Mentalist Revealed", youTubeSearchQuery("The Mentalist", "The Mentalist Revealed"))
        assertEquals("Doctor Who The Day of the Doctor", youTubeSearchQuery("Doctor Who", "The Day of the Doctor"))
    }

    @Test
    fun addsSpecialStreamsToAnAddonWithoutDuplicates() {
        val existing = StreamItem(name = "A", url = "https://example.com/a.mkv", addonName = "Addon", addonId = "addon:x")
        val group = AddonStreamGroup(addonName = "Addon", addonId = "addon:x", streams = listOf(existing), error = "failed")

        val merged = group.withSpecialStreams(
            listOf(existing, existing.copy(name = "B", url = "https://example.com/b.mkv")),
        )

        assertEquals(listOf("A", "B"), merged.streams.map { it.name })
        assertNull(merged.error)
        assertEquals(group, group.withSpecialStreams(emptyList()))
    }

    @Test
    fun picksTheSpecialsOwnImdbEntry() {
        val response = Json.parseToJsonElement(
            """
            {"d":[
              {"id":"tt1196946","l":"The Mentalist","q":"TV series"},
              {"id":"nm0000001","l":"The Mentalist Revealed"},
              {"id":"tt1500000","l":"The Mentalist Revealed","q":"TV special"}
            ]}
            """.trimIndent(),
        )

        val match = matchingImdbSuggestion(
            suggestions = parseImdbSuggestions(response),
            seriesId = "tt1196946",
            seriesName = "The Mentalist",
            specialTitle = "The Mentalist Revealed",
        )

        assertEquals("tt1500000", match?.id)
        assertNull(
            matchingImdbSuggestion(
                suggestions = listOf(ImdbSuggestion("tt9", "Something Else")),
                seriesId = "tt1196946",
                seriesName = "The Mentalist",
                specialTitle = "The Mentalist Revealed",
            ),
        )
    }
}
