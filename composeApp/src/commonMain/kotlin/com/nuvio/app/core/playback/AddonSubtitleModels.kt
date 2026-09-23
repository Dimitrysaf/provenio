package com.nuvio.app.core.playback

import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.AddonResource
import com.nuvio.app.core.addons.buildAddonResourceUrl
import com.nuvio.app.core.addons.enabledAddons
import kotlinx.serialization.Serializable

@Serializable
data class SubtitleAddonRequest(
    val url: String,
    val addonId: String,
    val addonName: String,
)

internal fun addonSubtitleRequests(type: String, videoId: String): List<SubtitleAddonRequest> {
    val requestType = canonicalSubtitleType(type)
    return AddonRepository.uiState.value.addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        if (manifest.resources.none { resource ->
                (resource.name.equals("subtitles", true) || resource.name.equals("subtitle", true)) &&
                    resource.supportsSubtitleType(requestType, videoId)
            }) return@mapNotNull null
        SubtitleAddonRequest(
            url = buildAddonResourceUrl(manifest.transportUrl, "subtitles", requestType, videoId),
            addonId = manifest.id,
            addonName = addon.displayTitle,
        )
    }
}

private fun canonicalSubtitleType(type: String): String =
    if (type.equals("tv", ignoreCase = true)) "series" else type.lowercase()

private fun AddonResource.supportsSubtitleType(type: String, videoId: String): Boolean {
    val canonical = canonicalSubtitleType(type)
    val typeMatches = types.isEmpty() || types.any { canonicalSubtitleType(it).equals(canonical, ignoreCase = true) }
    return typeMatches && (idPrefixes.isEmpty() || idPrefixes.any { videoId.startsWith(it) })
}
