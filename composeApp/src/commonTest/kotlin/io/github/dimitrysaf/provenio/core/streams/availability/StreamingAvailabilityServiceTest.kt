package io.github.dimitrysaf.provenio.core.streams.availability

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamingAvailabilityServiceTest {
    private val normalize: (String) -> String = { it.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim() }

    @Test
    fun prefersImdbIdsAndFallsBackToTmdb() {
        assertEquals("tt0903747", availabilityShowId("tt0903747:1:2", tmdbId = "1396", isSeries = true))
        assertEquals("tv/1396", availabilityShowId("tmdb:1396", tmdbId = "1396", isSeries = true))
        assertEquals("movie/603", availabilityShowId("tmdb:603", tmdbId = "603", isSeries = false))
        assertNull(availabilityShowId("kitsu:1", tmdbId = null, isSeries = true))
    }

    @Test
    fun readsOptionsWithDeepLinksAndAddons() {
        val options = parseAvailabilityOptions(
            Json.parseToJsonElement(
                """
                [
                  {"service":{"id":"apple","name":"Apple TV","imageSet":{"lightThemeImage":"l.svg","darkThemeImage":"d.svg"}},
                   "type":"buy","link":"https://tv.apple.com/x","quality":"uhd","price":{"formatted":"${'$'}19.99"}},
                  {"service":{"id":"netflix","name":"Netflix","imageSet":{"lightThemeImage":"nl.svg","darkThemeImage":"nd.svg"}},
                   "type":"subscription","link":"https://www.netflix.com/title/1","videoLink":"https://www.netflix.com/watch/2","quality":"hd"},
                  {"service":{"id":"prime","name":"Prime Video"},"type":"addon","link":"https://amazon/x",
                   "addon":{"name":"Starz","imageSet":{"lightThemeImage":"sl.svg"}}},
                  {"service":{"id":"x","name":"Unknown"},"type":"lease","link":"https://x"},
                  {"service":{"id":"y","name":"No Link"},"type":"free"}
                ]
                """.trimIndent(),
            ) as kotlinx.serialization.json.JsonArray,
        )

        assertEquals(listOf("Netflix", "Prime Video", "Apple TV"), options.map { it.serviceName })
        assertEquals("https://www.netflix.com/watch/2", options[0].link)
        assertEquals("HD", options[0].quality)
        assertEquals("nd.svg", options[0].darkLogo)
        assertEquals("Starz", options[1].addonName)
        assertEquals("sl.svg", options[1].lightLogo)
        assertEquals("$19.99", options[2].price)
    }

    @Test
    fun picksTheEpisodeAndFallsBackToItsSeason() {
        val show = series()

        val episode = matchAvailability(show, "US", season = 2, episode = 1, specialTitle = null, normalize = normalize)!!
        assertEquals(AvailabilityScope.Episode, episode.scope)
        assertEquals("https://netflix/s2e1", episode.options.single().link)

        val season = matchAvailability(show, "us", season = 2, episode = 9, specialTitle = null, normalize = normalize)!!
        assertEquals(AvailabilityScope.Season, season.scope)
        assertEquals("https://netflix/s2", season.options.single().link)

        assertNull(matchAvailability(show, "gb", season = 2, episode = 1, specialTitle = null, normalize = normalize))
        assertNull(matchAvailability(show, "us", season = 5, episode = 1, specialTitle = null, normalize = normalize))
    }

    @Test
    fun findsSpecialsByTitle() {
        val show = series()

        val special = matchAvailability(show, "us", season = 0, episode = 4, specialTitle = "The Christmas Special!", normalize = normalize)!!
        assertEquals(AvailabilityScope.Episode, special.scope)
        assertEquals("https://youtube/special", special.options.single().link)

        assertNull(matchAvailability(show, "us", season = 0, episode = 4, specialTitle = "Unknown", normalize = normalize))
        assertNull(matchAvailability(show, "us", season = 0, episode = 4, specialTitle = null, normalize = normalize))
    }

    @Test
    fun usesTheTitleForMovies() {
        val movie = Json.parseToJsonElement(
            """{"streamingOptions":{"us":[{"service":{"name":"Max"},"type":"subscription","link":"https://max/m"}]}}""",
        ) as JsonObject

        val match = matchAvailability(movie, "US", season = null, episode = null, specialTitle = null, normalize = normalize)!!

        assertEquals(AvailabilityScope.Title, match.scope)
        assertEquals("Max", match.options.single().serviceName)
    }

    private fun series(): JsonObject = Json.parseToJsonElement(
        """
        {"streamingOptions":{},"seasons":[
          {"title":"Season 1","streamingOptions":{},"episodes":[
            {"title":"Pilot","streamingOptions":{}}
          ]},
          {"title":"Season 2","streamingOptions":{"us":[{"service":{"name":"Netflix"},"type":"subscription","link":"https://netflix/s2"}]},
           "episodes":[
            {"title":"Return","streamingOptions":{"us":[{"service":{"name":"Netflix"},"type":"subscription","link":"https://netflix/s2e1"}]}},
            {"title":"The Christmas Special","streamingOptions":{"us":[{"service":{"name":"YouTube"},"type":"free","link":"https://youtube/special"}]}}
          ]}
        ]}
        """.trimIndent(),
    ) as JsonObject
}
