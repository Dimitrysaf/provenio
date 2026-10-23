package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.core.addons.AddonManifest
import io.github.dimitrysaf.provenio.core.addons.AddonResource
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackAvailabilityTest {
    @Test
    fun `empty setup and catalog only addons do not enable playback`() {
        assertFalse(available())
        assertFalse(available(addons = listOf(addon(resource = "catalog"))))
        assertFalse(available(addons = listOf(addon(resource = "meta"))))
    }

    @Test
    fun `a compatible stream addon enables playback`() {
        assertTrue(available(addons = listOf(addon())))
        assertFalse(available(addons = listOf(addon().copy(enabled = false))))
        assertFalse(available(addons = listOf(addon().copy(manifest = null))))
    }

    @Test
    fun `stream addons must support the requested type and id`() {
        val addons = listOf(addon())
        assertFalse(available(addons = addons, type = "series"))
        assertFalse(available(addons = addons, videoId = "kitsu:123"))
        assertTrue(available(addons = listOf(addon(prefixes = emptyList())), videoId = "kitsu:123"))
    }

    private fun available(
        addons: List<ManagedAddon> = emptyList(),
        type: String = "movie",
        videoId: String = "tt123",
    ): Boolean = hasCompatiblePlaybackSource(addons, type, videoId)

    private fun addon(
        resource: String = "stream",
        prefixes: List<String> = listOf("tt"),
    ): ManagedAddon = ManagedAddon(
        manifestUrl = "https://example.com/manifest.json",
        manifest = AddonManifest(
            id = "test",
            name = "Test",
            description = "",
            version = "1.0.0",
            resources = listOf(AddonResource(resource, listOf("movie"), prefixes)),
            types = listOf("movie"),
            transportUrl = "https://example.com",
        ),
    )
}
