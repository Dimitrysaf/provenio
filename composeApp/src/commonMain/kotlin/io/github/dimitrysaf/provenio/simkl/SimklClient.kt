package io.github.dimitrysaf.provenio.simkl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Client for the Simkl API.
 *
 * https://api.simkl.org
 */
class SimklClient(
    private val httpClient: HttpClient = defaultSimklHttpClient(),
) {
    /**
     * Why the last request failed.
     *
     * Both endpoints answer correctly from a plain HTTP client, so any failure here is
     * something about the request as the app makes it. Carrying the reason to the screen
     * beats guessing at it.
     */
    var lastFailure: String? = null
        private set

    suspend fun requestPin(): SimklPin? =
        getJson<SimklPin>(endpoint("/oauth/pin"))?.takeIf { it.userCode != null }

    /** The reason [requestPin] returned null, when it did. */
    fun lastRequestFailure(): String? = lastFailure

    suspend fun pollPin(userCode: String): PinStatus {
        val reply = getJson<SimklPinPoll>(endpoint("/oauth/pin/$userCode"))
            ?: return PinStatus.Unreachable(lastFailure)
        val token = reply.accessToken
        return when {
            token != null -> PinStatus.Authorized(token)
            // The documented pending body is result KO with an "Authorization pending"
            // message. Anything else with no token is a real failure.
            reply.message?.contains("pending", ignoreCase = true) == true -> PinStatus.Pending
            reply.result.equals("KO", ignoreCase = true) -> PinStatus.Pending
            else -> PinStatus.Rejected(reply.message)
        }
    }

    /**
     * client_id, app-name and app-version go on every request as query parameters, which
     * is unusual but is what Simkl requires.
     */
    private fun endpoint(path: String, extra: Map<String, String> = emptyMap()): String {
        val params = LinkedHashMap<String, String>()
        params["client_id"] = SimklConfig.ClientId
        params["app-name"] = SimklConfig.AppName
        params["app-version"] = SimklConfig.AppVersion
        params.putAll(extra)
        val query = params.entries.joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
        return "$BaseUrl$path?$query"
    }

    private suspend inline fun <reified T> getJson(
        url: String,
        accessToken: String? = null,
    ): T? {
        lastFailure = null
        val text = try {
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, SimklConfig.userAgent)
                if (accessToken != null) {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
            // A pending PIN is answered with a non-2xx by some deployments, so the body is
            // still worth reading before giving up on it.
            if (!response.status.isSuccess() && response.status.value >= 500) {
                lastFailure = "Simkl answered ${response.status.value}."
                return null
            }
            response.bodyAsText()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            lastFailure = failure::class.simpleName + ": " + (failure.message ?: "no detail")
            return null
        }

        return try {
            simklJson.decodeFromString<T>(text)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            lastFailure = "Unreadable reply: " + text.take(120)
            null
        }
    }

    companion object {
        const val BaseUrl = "https://api.simkl.com"

        val simklJson: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}

private const val ConnectTimeoutMillis = 10_000L
private const val RequestTimeoutMillis = 20_000L

fun defaultSimklHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        connectTimeoutMillis = ConnectTimeoutMillis
        requestTimeoutMillis = RequestTimeoutMillis
        socketTimeoutMillis = RequestTimeoutMillis
    }
}
