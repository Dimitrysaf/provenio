package com.nuvio.app.shell.screens.player

import com.nuvio.app.core.addons.AddonRepository
import com.nuvio.app.core.addons.AddonResource
import com.nuvio.app.core.addons.buildAddonResourceUrl
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.addons.fetchAddonResponseText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_addon_subtitle_display_format
import org.jetbrains.compose.resources.getString
import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.SubtitleAddonRequest
import com.nuvio.app.core.playback.normalizeLanguageCode

internal suspend fun loadAddonSubtitles(
    requests: List<SubtitleAddonRequest>,
    onLoaded: (SubtitleAddonRequest, List<AddonSubtitle>) -> Unit = { _, _ -> },
): List<AddonSubtitle> = supervisorScope {
    requests.map { request ->
        async {
            val subtitles = try {
                withTimeoutOrNull(10_000L) {
                    parseAddonSubtitles(fetchAddonResponseText(request.url), request)
                }.orEmpty()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emptyList()
            }
            currentCoroutineContext().ensureActive()
            onLoaded(request, subtitles)
            subtitles
        }
    }.awaitAll().flatten()
}

private suspend fun parseAddonSubtitles(response: String, request: SubtitleAddonRequest): List<AddonSubtitle> {
    val subtitles = Json.parseToJsonElement(response).jsonObject["subtitles"]?.jsonArray.orEmpty()
    return subtitles.mapIndexedNotNull { index, element ->
        val obj = element as? JsonObject ?: return@mapIndexedNotNull null
        val url = obj.stringValue("url") ?: return@mapIndexedNotNull null
        val language = listOf("lang", "language", "languageCode", "locale", "label")
            .firstNotNullOfOrNull(obj::stringValue) ?: "unknown"
        AddonSubtitle(
            id = obj.stringValue("id") ?: "${request.addonId}_$index",
            url = url,
            language = normalizeLanguageCode(language) ?: language,
            display = getString(
                Res.string.player_addon_subtitle_display_format,
                getLanguageLabelForCode(language),
                request.addonName,
            ),
            addonName = request.addonName,
        )
    }
}

private fun JsonObject.stringValue(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
