package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.core.addons.AddonManifest
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.plugins.PluginsUiState

internal fun AddonManifest.supportsStream(type: String, videoId: String): Boolean =
    resources.any { resource ->
        resource.name == "stream" &&
            resource.types.contains(type) &&
            (resource.idPrefixes.isEmpty() || resource.idPrefixes.any { videoId.startsWith(it) })
    }

internal fun hasCompatiblePlaybackSource(
    addons: List<ManagedAddon>,
    plugins: PluginsUiState,
    type: String,
    videoId: String,
): Boolean = addons.any { it.enabled && it.manifest?.supportsStream(type, videoId) == true } ||
    (plugins.pluginsEnabled && plugins.scrapers.any { it.enabled && it.supportsType(type) })
