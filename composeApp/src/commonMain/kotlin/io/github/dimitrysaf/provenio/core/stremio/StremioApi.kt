package io.github.dimitrysaf.provenio.core.stremio

import io.github.dimitrysaf.provenio.core.addons.httpRequestRaw
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal class StremioApiException(message: String) : Exception(message)

@Serializable
internal data class StremioUser(
    @SerialName("_id") val id: String = "",
    val email: String = "",
)

@Serializable
internal data class StremioAuth(
    val authKey: String,
    val user: StremioUser = StremioUser(),
)

@Serializable
internal data class StremioAddonDescriptor(
    val transportUrl: String = "",
)

@Serializable
internal data class StremioLibraryItem(
    @SerialName("_id") val id: String,
    val name: String = "",
    val type: String = "",
    val poster: String? = null,
    val posterShape: String? = null,
    val removed: Boolean = false,
    val temp: Boolean = false,
    @SerialName("_ctime") val createdAt: String? = null,
    @SerialName("_mtime") val modifiedAt: String? = null,
    val state: StremioLibraryItemState = StremioLibraryItemState(),
)

/** Times are in milliseconds; older Stremio versions saved some of them as fractions. */
@Serializable
internal data class StremioLibraryItemState(
    val lastWatched: String? = null,
    val timeOffset: Double = 0.0,
    val duration: Double = 0.0,
    val timesWatched: Double = 0.0,
    val flaggedWatched: Double = 0.0,
    @SerialName("video_id") val videoId: String? = null,
    val watched: String? = null,
)

/** Stremio's account service: signing in, and reading the add-ons and library kept with an account. */
internal object StremioApi {
    private const val BaseUrl = "https://api.strem.io/api/"
    private const val MaxResponseBytes = 32 * 1024 * 1024
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun login(email: String, password: String): StremioAuth =
        json.decodeFromJsonElement(
            call(
                "login",
                buildJsonObject {
                    put("type", "Login")
                    put("email", email)
                    put("password", password)
                    put("facebook", false)
                },
            ),
        )

    suspend fun user(authKey: String): StremioUser =
        json.decodeFromJsonElement(
            call(
                "getUser",
                buildJsonObject {
                    put("type", "GetUser")
                    put("authKey", authKey)
                },
            ),
        )

    suspend fun addons(authKey: String): List<StremioAddonDescriptor> {
        val result = call(
            "addonCollectionGet",
            buildJsonObject {
                put("type", "AddonCollectionGet")
                put("authKey", authKey)
                put("update", true)
            },
        )
        return (result.jsonObject["addons"] as? JsonArray).orEmpty().decodeEach()
    }

    suspend fun library(authKey: String): List<StremioLibraryItem> {
        val result = call(
            "datastoreGet",
            buildJsonObject {
                put("authKey", authKey)
                put("collection", "libraryItem")
                putJsonArray("ids") {}
                put("all", true)
            },
        )
        return (result as? JsonArray).orEmpty().decodeEach()
    }

    // An item Stremio saved in a shape this does not read is left out, rather than failing the rest.
    private inline fun <reified T> List<JsonElement>.decodeEach(): List<T> =
        mapNotNull { element -> runCatching { json.decodeFromJsonElement<T>(element) }.getOrNull() }

    private suspend fun call(method: String, body: JsonObject): JsonElement {
        val response = httpRequestRaw(
            method = "POST",
            url = BaseUrl + method,
            headers = mapOf("Content-Type" to "application/json"),
            body = body.toString(),
            maxResponseBodyBytes = MaxResponseBytes,
        )
        val root = runCatching { json.parseToJsonElement(response.body).jsonObject }.getOrNull()
            ?: throw StremioApiException("Stremio answered $method with HTTP ${response.status}")
        (root["error"] as? JsonObject)?.let { error ->
            throw StremioApiException(error["message"]?.jsonPrimitive?.contentOrNull ?: "Stremio refused $method")
        }
        return root["result"] ?: throw StremioApiException("Stremio answered $method with HTTP ${response.status}")
    }
}
