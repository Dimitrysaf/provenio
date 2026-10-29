package io.github.dimitrysaf.provenio.core.party

import kotlin.io.encoding.Base64

internal const val PARTY_SECRET_SIZE = 32
private const val LINK_PREFIX = "provenio://party"
private const val LINK_VERSION = "2"

/** Where a room listens and the secret that lets a guest in, as an invite link carries them. */
internal data class PartyInvite(
    val endpoints: List<PartyEndpoint>,
    val secret: ByteArray,
)

internal data class PartyEndpoint(val host: String, val port: Int) {
    // IPv6 addresses go in brackets, so their colons are not read as the port's.
    override fun toString(): String = if (':' in host) "[$host]:$port" else "$host:$port"
}

// provenio://party?v=2&a=<host:port,[v6]:port>&k=<secret>
internal fun buildPartyLink(endpoints: List<PartyEndpoint>, secret: ByteArray): String =
    "$LINK_PREFIX?v=$LINK_VERSION&a=${endpoints.joinToString(",")}&k=${encodePartyBytes(secret)}"

internal fun parsePartyInvite(input: String): PartyInvite? {
    // The link may come pasted inside a message, so it is picked out of the text around it.
    val text = input.trim()
    val start = text.indexOf(LINK_PREFIX, ignoreCase = true).takeIf { it >= 0 } ?: return null
    val link = text.substring(start).takeWhile { !it.isWhitespace() }
    val parameters = link.substringAfter('?', "")
        .split('&')
        .mapNotNull { part ->
            val separator = part.indexOf('=').takeIf { it > 0 } ?: return@mapNotNull null
            part.substring(0, separator).lowercase() to percentDecode(part.substring(separator + 1))
        }
        .toMap()
    if (parameters["v"] != LINK_VERSION) return null
    val endpoints = parameters["a"].orEmpty()
        .split(',')
        .mapNotNull { parseEndpoint(it.trim()) }
        .takeIf { it.isNotEmpty() }
        ?: return null
    val secret = parameters["k"]
        ?.let { runCatching { decodePartyBytes(it) }.getOrNull() }
        ?.takeIf { it.size == PARTY_SECRET_SIZE }
        ?: return null
    return PartyInvite(endpoints = endpoints, secret = secret)
}

private fun parseEndpoint(value: String): PartyEndpoint? {
    val (host, port) = if (value.startsWith('[')) {
        val end = value.indexOf(']').takeIf { it > 1 } ?: return null
        value.substring(1, end) to value.substring(end + 1).removePrefix(":")
    } else {
        value.substringBeforeLast(':', "") to value.substringAfterLast(':', "")
    }
    val portNumber = port.toIntOrNull()?.takeIf { it in 1..65_535 } ?: return null
    if (host.isEmpty() || !host.all { it.isLetterOrDigit() || it in ".:-%" }) return null
    return PartyEndpoint(host, portNumber)
}

// URL-safe and without padding, which chat apps tend to cut off or escape.
internal fun encodePartyBytes(bytes: ByteArray): String = Base64.UrlSafe.encode(bytes).trimEnd('=')

internal fun decodePartyBytes(value: String): ByteArray {
    val unpadded = value.trimEnd('=')
    return Base64.UrlSafe.decode(unpadded + "=".repeat((4 - unpadded.length % 4) % 4))
}

private fun percentDecode(value: String): String = buildString {
    var index = 0
    while (index < value.length) {
        val char = value[index]
        val decoded = if (char == '%' && index + 2 < value.length) value.substring(index + 1, index + 3).toIntOrNull(16) else null
        if (decoded != null) {
            append(decoded.toChar())
            index += 3
        } else {
            append(char)
            index += 1
        }
    }
}
