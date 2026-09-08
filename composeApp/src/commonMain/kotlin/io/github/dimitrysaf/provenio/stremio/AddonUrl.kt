package io.github.dimitrysaf.provenio.stremio

import io.ktor.http.encodeURLParameter

/**
 * Builds addon protocol URLs.
 *
 * Routes are `/{resource}/{type}/{id}.json`, with an optional extra segment giving
 * `/{resource}/{type}/{id}/{extraArgs}.json` where `extraArgs` is a query string encoded
 * into that path segment — `search=game%20of%20thrones&skip=100`.
 *
 * https://github.com/Stremio/stremio-addon-sdk/blob/master/docs/protocol.md
 */
object AddonUrl {

    /**
     * Reduces anything that identifies an addon to its base URL.
     *
     * Accepts the `stremio://` scheme that install links use, and a transport URL pointing
     * straight at `manifest.json`, which is how addons are normally shared.
     */
    fun base(addonUrl: String): String {
        var url = addonUrl.trim()
        if (url.startsWith("stremio://", ignoreCase = true)) {
            url = "https://" + url.removePrefix("stremio://").removePrefix("STREMIO://")
        }
        url = url.removeSuffix("/")
        if (url.endsWith("/manifest.json", ignoreCase = true)) {
            url = url.dropLast("/manifest.json".length)
        }
        return url
    }

    fun manifest(addonUrl: String): String = "${base(addonUrl)}/manifest.json"

    /**
     * `type` and `id` are passed through unencoded: ids come from the addon itself and
     * carry meaningful separators — an episode id is `tt0108778:1:1` — which percent
     * encoding would corrupt.
     */
    fun resource(
        addonUrl: String,
        resource: String,
        type: String,
        id: String,
        extra: Map<String, String> = emptyMap(),
    ): String {
        val path = "${base(addonUrl)}/$resource/$type/$id"
        val encoded = encodeExtra(extra)
        return if (encoded.isEmpty()) "$path.json" else "$path/$encoded.json"
    }

    /** Encodes extra args as a query string for use inside a single path segment. */
    fun encodeExtra(extra: Map<String, String>): String =
        extra.entries.joinToString("&") { (key, value) ->
            "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
        }
}
