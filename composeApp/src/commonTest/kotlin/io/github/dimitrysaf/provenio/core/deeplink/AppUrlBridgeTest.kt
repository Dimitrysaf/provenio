package io.github.dimitrysaf.provenio.core.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppUrlBridgeTest {

    @Test
    fun `parses existing notification meta deeplink`() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            parseAppDeepLink("provenio://meta?type=series&id=tt0944947"),
        )
    }

    @Test
    fun `parses direct provenio addon install deeplink`() {
        assertEquals(
            AppDeepLink.AddonInstall("https://free.nebulapro.xyz/sports/i/free/manifest.json"),
            parseAppDeepLink("provenio://free.nebulapro.xyz/sports/i/free/manifest.json"),
        )
    }

    @Test
    fun `parses stremio addon install deeplink`() {
        assertEquals(
            AppDeepLink.AddonInstall("https://free.nebulapro.xyz/sports/i/free/manifest.json"),
            parseAppDeepLink("stremio://free.nebulapro.xyz/sports/i/free/manifest.json"),
        )
    }

    @Test
    fun `parses direct imdb detail deeplink`() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            parseAppDeepLink("provenio://series/tt0944947"),
        )
    }

    @Test
    fun `parses provider imdb detail deeplink`() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tt0944947"),
            parseAppDeepLink("provenio://imdb/series/tt0944947"),
        )
    }

    @Test
    fun `parses provider tmdb detail deeplink`() {
        assertEquals(
            AppDeepLink.Meta(type = "series", id = "tmdb:1399"),
            parseAppDeepLink("provenio://tmdb/tv/1399"),
        )
    }

    @Test
    fun `does not treat reserved auth link as addon install`() {
        assertNull(parseAppDeepLink("provenio://auth/trakt?code=abc"))
    }

    @Test
    fun `does not treat non-host stremio link as addon install`() {
        assertNull(parseAppDeepLink("stremio://detail/series/tt0944947"))
    }
}
