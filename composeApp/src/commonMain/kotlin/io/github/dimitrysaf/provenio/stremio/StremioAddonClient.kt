package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.AddonCatalogResponse
import io.github.dimitrysaf.provenio.stremio.model.CatalogResponse
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.stremio.model.MetaResponse
import io.github.dimitrysaf.provenio.stremio.model.StreamResponse
import io.github.dimitrysaf.provenio.stremio.model.SubtitlesResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Client for the Stremio addon protocol: plain HTTP/JSON, no native dependency.
 *
 * https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md
 */
class StremioAddonClient(
    private val httpClient: HttpClient = defaultAddonHttpClient(),
) {
    suspend fun fetchManifest(addonUrl: String): AddonResult<Manifest> =
        requestJson(AddonUrl.manifest(addonUrl))

    suspend fun fetchCatalog(
        addonUrl: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap(),
    ): AddonResult<CatalogResponse> =
        requestJson(AddonUrl.resource(addonUrl, "catalog", type, id, extra))

    suspend fun fetchMeta(addonUrl: String, type: String, id: String): AddonResult<MetaResponse> =
        requestJson(AddonUrl.resource(addonUrl, "meta", type, id))

    suspend fun fetchStreams(
        addonUrl: String,
        type: String,
        id: String,
    ): AddonResult<StreamResponse> = requestJson(AddonUrl.resource(addonUrl, "stream", type, id))

    suspend fun fetchSubtitles(
        addonUrl: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap(),
    ): AddonResult<SubtitlesResponse> =
        requestJson(AddonUrl.resource(addonUrl, "subtitles", type, id, extra))

    suspend fun fetchAddonCatalog(
        addonUrl: String,
        type: String,
        id: String,
    ): AddonResult<AddonCatalogResponse> =
        requestJson(AddonUrl.resource(addonUrl, "addon_catalog", type, id))

    /**
     * Bodies are read as text and parsed explicitly rather than through content
     * negotiation: addons commonly serve valid JSON under `text/plain` or `text/html`,
     * which a content-type-driven decoder rejects.
     */
    private suspend inline fun <reified T> requestJson(url: String): AddonResult<T> {
        val text = try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                return AddonResult.HttpError(response.status.value, response.status.description)
            }
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            return AddonResult.NetworkError(failure)
        }

        return try {
            AddonResult.Success(addonJson.decodeFromString<T>(text))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            AddonResult.ParseError(failure)
        }
    }

    companion object {
        /** Lenient by design — addons are third-party and frequently sloppy. */
        val addonJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}

private const val ConnectTimeoutMillis = 10_000L
private const val RequestTimeoutMillis = 20_000L
private const val SocketTimeoutMillis = 20_000L

/** A dead addon must not hang the UI, so every request is bounded. */
fun defaultAddonHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        connectTimeoutMillis = ConnectTimeoutMillis
        requestTimeoutMillis = RequestTimeoutMillis
        socketTimeoutMillis = SocketTimeoutMillis
    }
}
