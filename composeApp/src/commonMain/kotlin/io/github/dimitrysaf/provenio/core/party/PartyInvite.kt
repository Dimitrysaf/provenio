package io.github.dimitrysaf.provenio.core.party

import kotlin.io.encoding.Base64

internal const val PARTY_CODE_LENGTH = 6
internal const val PARTY_SECRET_SIZE = 32
private const val LINK_PREFIX = "provenio://party"
private const val LINK_VERSION = "1"

/** What someone typed, scanned or opened to join a room. */
internal sealed interface PartyJoinTarget {
    /** Just the code: the room is found by listening for it on this network, and the host lets them in. */
    data class Code(val code: String) : PartyJoinTarget

    /**
     * A link, which says where the room listens. With the room's secret it enters straight away;
     * without one it waits for the host, like a code.
     */
    data class Link(
        val code: String,
        val port: Int,
        val addresses: List<String>,
        val secret: ByteArray?,
    ) : PartyJoinTarget
}

// provenio://party?v=1&c=<code>&p=<port>&a=<address,address>&k=<secret>
internal fun buildPartyLink(code: String, port: Int, addresses: List<String>, secret: ByteArray): String =
    "$LINK_PREFIX?v=$LINK_VERSION&c=$code&p=$port&a=${addresses.joinToString(",")}&k=${encodePartyBytes(secret)}"

internal fun parsePartyJoinTarget(input: String): PartyJoinTarget? {
    val text = input.trim()
    val digits = text.filterNot { it == ' ' || it == '-' }
    if (digits.isPartyCode()) return PartyJoinTarget.Code(digits)

    // The link may come pasted inside a message, so it is picked out of the text around it.
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
    val code = parameters["c"]?.takeIf { it.isPartyCode() } ?: return null
    val port = parameters["p"]?.toIntOrNull()?.takeIf { it in 1..65_535 } ?: return null
    val addresses = parameters["a"].orEmpty()
        .split(',')
        .map { it.trim().removeSurrounding("[", "]") }
        .filter { address -> address.isNotEmpty() && address.all { it.isLetterOrDigit() || it in ".:-%" } }
        .takeIf { it.isNotEmpty() }
        ?: return null
    val secret = parameters["k"]?.let { value ->
        runCatching { decodePartyBytes(value) }.getOrNull()?.takeIf { it.size == PARTY_SECRET_SIZE } ?: return null
    }
    return PartyJoinTarget.Link(code = code, port = port, addresses = addresses, secret = secret)
}

internal fun String.isPartyCode(): Boolean = length == PARTY_CODE_LENGTH && all { it in '0'..'9' }

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
