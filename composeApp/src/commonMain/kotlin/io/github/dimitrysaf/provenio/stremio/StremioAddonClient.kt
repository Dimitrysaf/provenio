package io.github.dimitrysaf.provenio.stremio

import io.github.dimitrysaf.provenio.stremio.model.CatalogResponse
import io.github.dimitrysaf.provenio.stremio.model.Manifest
import io.github.dimitrysaf.provenio.stremio.model.MetaResponse
import io.github.dimitrysaf.provenio.stremio.model.StreamResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Client for the Stremio addon protocol: plain HTTP/JSON, documented at
 * https://github.com/Stremio/stremio-addon-sdk. No native/Rust dependency involved.
 */
class StremioAddonClient(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
) {
    suspend fun fetchManifest(addonBaseUrl: String): Manifest =
        httpClient.get("${baseUrl(addonBaseUrl)}/manifest.json").body()

    suspend fun fetchCatalog(
        addonBaseUrl: String,
        type: String,
        id: String,
        extra: String? = null,
    ): CatalogResponse = httpClient.get(resourceUrl(addonBaseUrl, "catalog", type, id, extra)).body()

    suspend fun fetchMeta(addonBaseUrl: String, type: String, id: String): MetaResponse =
        httpClient.get(resourceUrl(addonBaseUrl, "meta", type, id, null)).body()

    suspend fun fetchStreams(addonBaseUrl: String, type: String, id: String): StreamResponse =
        httpClient.get(resourceUrl(addonBaseUrl, "stream", type, id, null)).body()

    private fun baseUrl(url: String) = url.trimEnd('/')

    private fun resourceUrl(addonBaseUrl: String, resource: String, type: String, id: String, extra: String?): String {
        val path = "${baseUrl(addonBaseUrl)}/$resource/$type/$id"
        return if (extra != null) "$path/$extra.json" else "$path.json"
    }
}
