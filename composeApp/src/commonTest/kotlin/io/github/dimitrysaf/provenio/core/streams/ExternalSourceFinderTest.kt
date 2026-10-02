package io.github.dimitrysaf.provenio.core.streams

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalSourceFinderTest {
    @Test
    fun asksForMoviesAndEpisodesOnly() {
        assertEquals(
            ExternalSourceRequest(type = "movie", metaId = "tt0063350", season = null, episode = null),
            externalSourceRequest("movie", "tt0063350", null, null, null),
        )
        assertEquals(
            ExternalSourceRequest(type = "series", metaId = "tt0436992", season = 0, episode = 3),
            externalSourceRequest("series", "tt0436992:0:3", "tt0436992", 0, 3),
        )
        assertNull(externalSourceRequest("series", "tt0436992", "tt0436992", null, null))
        assertNull(externalSourceRequest("tv", "channel:1", null, null, null))
    }

    @Test
    fun readsTheRegionsOffersInDisplayOrder() {
        val root = Json.parseToJsonElement(
            """
            {"id":1,"results":{
              "GB":{"link":"https://www.themoviedb.org/tv/1/watch?locale=GB",
                "buy":[{"provider_id":2,"provider_name":"Apple TV","logo_path":"/apple.jpg","display_priority":4}],
                "flatrate":[
                  {"provider_id":9,"provider_name":"Prime Video","logo_path":"/prime.jpg","display_priority":3},
                  {"provider_id":8,"provider_name":"Netflix","logo_path":"/netflix.jpg","display_priority":1}
                ],
                "ads":[{"provider_id":73,"provider_name":"Tubi","display_priority":20}]
              },
              "US":{"flatrate":[{"provider_id":15,"provider_name":"Hulu","display_priority":1}]}
            }}
            """.trimIndent(),
        )

        val availability = parseWatchProviders(root, "gb")!!

        assertEquals("https://www.themoviedb.org/tv/1/watch?locale=GB", availability.link)
        assertEquals(listOf("Tubi", "Netflix", "Prime Video", "Apple TV"), availability.offers.map { it.providerName })
        assertEquals(
            listOf(WatchOfferKind.Ads, WatchOfferKind.Subscription, WatchOfferKind.Subscription, WatchOfferKind.Buy),
            availability.offers.map { it.kind },
        )
        assertEquals("/netflix.jpg", availability.offers[1].logoPath)
        assertNull(availability.offers.first().logoPath)
        assertNull(parseWatchProviders(root, "DE"))
    }

    @Test
    fun findsTheSpecialOnTmdbByItsTitle() {
        val root = Json.parseToJsonElement(
            """
            {"episodes":[
              {"episode_number":1,"name":"Behind the Scenes"},
              {"episode_number":2,"name":"The Abominable Bride"},
              {"episode_number":3}
            ]}
            """.trimIndent(),
        )
        val episodes = parseTmdbSeasonEpisodes(root)

        assertEquals(2, episodes.size)
        assertEquals(2, matchingSpecialEpisode(episodes, "the abominable bride!"))
        assertNull(matchingSpecialEpisode(episodes, "Abominable"))
        assertNull(matchingSpecialEpisode(episodes, ""))
    }

    @Test
    fun readsTmdbVideos() {
        val root = Json.parseToJsonElement(
            """
            {"results":[
              {"key":"abc","site":"YouTube","name":"Official Trailer","type":"Trailer"},
              {"key":"123","site":"Vimeo","name":"Clip"},
              {"site":"YouTube","name":"No key"}
            ]}
            """.trimIndent(),
        )

        val videos = parseTmdbVideos(root)

        assertEquals(listOf("abc", "123"), videos.map { it.key })
        assertEquals("Trailer", videos.first().type)
        assertNull(videos[1].type)
    }

    @Test
    fun matchesArchiveMoviesByExactTitleAndYear() {
        val root = Json.parseToJsonElement(
            """
            {"response":{"docs":[
              {"identifier":"night_of_the_living_dead","title":"Night of the Living Dead (1968)","year":"1968"},
              {"identifier":"notld_remake","title":"Night of the Living Dead","year":1990},
              {"identifier":"notld_review","title":"Night of the Living Dead - a review","year":"1968"},
              {"identifier":"notld_nodate","title":"Night Of The Living Dead"}
            ]}}
            """.trimIndent(),
        )
        val items = parseArchiveSearch(root)

        val matches = matchingArchiveItems(items, "Night of the Living Dead", year = 1968, exactTitle = true)

        assertEquals(listOf("night_of_the_living_dead", "notld_nodate"), matches.map { it.identifier })
    }

    @Test
    fun keepsYearsThatArePartOfTheTitle() {
        val items = listOf(ArchiveItem(identifier = "a", title = "Blade Runner 2049", year = 2017))

        assertEquals(1, matchingArchiveItems(items, "Blade Runner 2049", year = 2017, exactTitle = true).size)
    }

    @Test
    fun matchesArchiveSpecialsByTheirWords() {
        val items = listOf(
            ArchiveItem(identifier = "a", title = "A Charlie Brown Christmas (TV Special)", year = 1965),
            ArchiveItem(identifier = "b", title = "Charlie Brown's All Stars", year = 1966),
        )

        val matches = matchingArchiveItems(items, "A Charlie Brown Christmas", year = null, exactTitle = false)

        assertEquals(listOf("a"), matches.map { it.identifier })
    }

    @Test
    fun picksTheFullQualityMp4() {
        val root = Json.parseToJsonElement(
            """
            {"metadata":{"identifier":"x"},"files":[
              {"name":"movie_512kb.mp4","format":"512Kb MPEG4","size":"1000"},
              {"name":"movie.ogv","format":"Ogg Video","size":"9000"},
              {"name":"movie.mp4","format":"h.264","size":"5000"}
            ]}
            """.trimIndent(),
        )

        val file = pickArchiveVideo(root)!!

        assertEquals("movie.mp4", file.name)
        assertEquals(5000L, file.size)
    }

    @Test
    fun skipsRestrictedArchiveItems() {
        val dark = Json.parseToJsonElement("""{"is_dark":true,"files":[{"name":"a.mp4"}]}""")
        val restricted = Json.parseToJsonElement(
            """{"metadata":{"access-restricted-item":"true"},"files":[{"name":"a.mp4"}]}""",
        )
        val noVideo = Json.parseToJsonElement("""{"files":[{"name":"a.ogv"}]}""")

        assertNull(pickArchiveVideo(dark))
        assertNull(pickArchiveVideo(restricted))
        assertNull(pickArchiveVideo(noVideo))
    }

    @Test
    fun externalSourcesBecomeTheirOwnGroupsAfterEmbeddedOnes() {
        val groups = embeddedStreamGroups(
            listOf(
                StreamItem(name = "Clip", url = "https://youtu.be/a", addonName = "YouTube", addonId = EmbeddedSourceAddonId),
                StreamItem(name = "Subscription", externalUrl = "https://tmdb/watch", addonName = "Netflix", addonId = EmbeddedSourceAddonId),
                StreamItem(name = "Rent", externalUrl = "https://tmdb/watch", addonName = "Apple TV", addonId = EmbeddedSourceAddonId),
                StreamItem(name = "Buy", externalUrl = "https://tmdb/watch", addonName = "Apple TV", addonId = EmbeddedSourceAddonId),
            ),
        )

        assertEquals(listOf("YouTube", "Netflix", "Apple TV"), groups.map { it.addonName })
        assertEquals(2, groups.last().streams.size)
        assertTrue(groups.all { it.isEmbeddedSource })
    }
}
