package io.github.dimitrysaf.provenio.core.network

import kotlinx.coroutines.TimeoutCancellationException

/**
 * Thrown when a server answers with a non-success status, so callers can tell a missing title (404)
 * from a busy server (429) or a broken one (5xx) instead of only seeing the message text.
 */
class HttpStatusException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)

/** Thrown when a server answers successfully but says it has nothing for the requested item. */
class NoContentException(message: String) : IllegalStateException(message)

/** Thrown when a server answers successfully with an empty body. */
class EmptyResponseException(message: String) : IllegalStateException(message)

/** Why something could not be loaded. Each kind means one thing, so the screen can say exactly that. */
enum class LoadFailureKind {
    /** HTTP 404/410, or an answer saying it has no entry: the source has no entry for this item. */
    NotFound,

    /** The source answered with an empty body. */
    EmptyResponse,

    /** HTTP 429: the source is limiting how often it may be asked. */
    RateLimited,

    /** HTTP 401/407: credentials or an API key are missing, wrong or expired. */
    Unauthorized,

    /** HTTP 403: the source understood the request but won't allow it. */
    Forbidden,

    /** HTTP 451: the source may not provide this for legal reasons. */
    LegalBlock,

    /** Other HTTP 4xx: the source couldn't process the request. */
    BadRequest,

    /** HTTP 500 and other 5xx: the source's server failed while handling the request. */
    ServerError,

    /** HTTP 503: the source's server is overloaded or down for maintenance. */
    Unavailable,

    /** HTTP 502/504 and Cloudflare 520–530: the source's hosting got no answer from its server. */
    GatewayError,

    /** No answer arrived in time (HTTP 408, or our own time limit). */
    Timeout,

    /** The server's address could not be looked up. */
    DnsFailure,

    /** The certificate or encryption could not be verified. */
    SecureConnectionFailed,

    /** A connection to the server could not be opened. */
    Network,

    /** The answer could not be read as the expected data. */
    InvalidResponse,

    Unknown,
}

/**
 * One source's failure. [sourceName] is shown to the user, so it is the add-on or service name.
 * [details] is the full technical record for the verbose view: what was requested, the result, and
 * the whole exception with its stack trace.
 */
data class LoadFailure(
    val sourceName: String,
    val kind: LoadFailureKind,
    val statusCode: Int? = null,
    val details: String? = null,
)

fun Throwable.toLoadFailureKind(): LoadFailureKind {
    if (this is TimeoutCancellationException) return LoadFailureKind.Timeout
    if (this is NoContentException) return LoadFailureKind.NotFound
    if (this is EmptyResponseException) return LoadFailureKind.EmptyResponse
    if (this is HttpStatusException) {
        return when (statusCode) {
            404, 410 -> LoadFailureKind.NotFound
            429 -> LoadFailureKind.RateLimited
            401, 407 -> LoadFailureKind.Unauthorized
            403 -> LoadFailureKind.Forbidden
            451 -> LoadFailureKind.LegalBlock
            408 -> LoadFailureKind.Timeout
            503 -> LoadFailureKind.Unavailable
            502, 504, in 520..530 -> LoadFailureKind.GatewayError
            in 500..599 -> LoadFailureKind.ServerError
            in 400..499 -> LoadFailureKind.BadRequest
            else -> LoadFailureKind.Unknown
        }
    }
    // Platform network exceptions are not visible from common code, so match them by name.
    val name = this::class.simpleName.orEmpty()
    return when {
        name.contains("Timeout", ignoreCase = true) -> LoadFailureKind.Timeout
        name.contains("UnknownHost", ignoreCase = true) -> LoadFailureKind.DnsFailure
        name.contains("SSL", ignoreCase = true) ||
            name.contains("Certificate", ignoreCase = true) ||
            name.contains("Handshake", ignoreCase = true) -> LoadFailureKind.SecureConnectionFailed
        name.contains("Connect", ignoreCase = true) ||
            name.contains("NoRouteToHost", ignoreCase = true) ||
            name.contains("Socket", ignoreCase = true) ||
            name.contains("IOException", ignoreCase = true) ||
            name.contains("Darwin", ignoreCase = true) -> LoadFailureKind.Network
        name.contains("Serialization", ignoreCase = true) ||
            name.contains("JsonDecoding", ignoreCase = true) ||
            this is IllegalArgumentException -> LoadFailureKind.InvalidResponse
        else -> LoadFailureKind.Unknown
    }
}

/**
 * [request] says what was asked for, shown in the verbose view. Pass a URL through [redactUrl], or
 * describe the request without its URL when the URL holds a key.
 */
fun Throwable.toLoadFailure(sourceName: String, request: String? = null): LoadFailure {
    val kind = toLoadFailureKind()
    val statusCode = (this as? HttpStatusException)?.statusCode
    return LoadFailure(
        sourceName = sourceName,
        kind = kind,
        statusCode = statusCode,
        details = buildFailureDetails(sourceName, request, kind, statusCode) + "\n\n" + stackTraceToString(),
    )
}

/** A failure without an exception, such as our own time limit running out. */
fun loadFailureWithoutException(
    sourceName: String,
    kind: LoadFailureKind,
    request: String?,
    note: String,
): LoadFailure =
    LoadFailure(
        sourceName = sourceName,
        kind = kind,
        details = buildFailureDetails(sourceName, request, kind, statusCode = null) + "\n\n" + note,
    )

private fun buildFailureDetails(
    sourceName: String,
    request: String?,
    kind: LoadFailureKind,
    statusCode: Int?,
): String =
    buildString {
        append("Source: ").append(sourceName)
        if (request != null) append("\nRequest: ").append(request)
        append("\nResult: ").append(kind.name)
        if (statusCode != null) append(" (HTTP ").append(statusCode).append(')')
    }

/**
 * Add-on URLs often carry the user's configuration, debrid keys included, in the path or query, so
 * the verbose view keeps only the origin and the last [keepSegments] path segments.
 */
fun redactUrl(url: String, keepSegments: Int = 3): String {
    val bare = url.substringBefore('?').substringBefore('#')
    val schemeEnd = bare.indexOf("://").takeIf { it >= 0 }?.plus(3) ?: 0
    val hostEnd = bare.indexOf('/', schemeEnd).takeIf { it >= 0 } ?: bare.length
    val scheme = bare.substring(0, schemeEnd)
    val host = bare.substring(schemeEnd, hostEnd).substringAfterLast('@')
    val segments = bare.substring(hostEnd).split('/').filter { it.isNotEmpty() }
    val path = if (segments.size <= keepSegments) {
        segments.joinToString("/")
    } else {
        "…/" + segments.takeLast(keepSegments).joinToString("/")
    }
    return "$scheme$host/$path"
}

/**
 * The failure to tell the user about when several sources failed. A source that is busy or down
 * may still have the item, so those come first; "not found" comes last, because saying an item
 * doesn't exist when one source was only rate-limited would be wrong.
 */
fun List<LoadFailure>.mostActionable(): LoadFailure? =
    minByOrNull { failure -> HeadlinePriority.indexOf(failure.kind) }

private val HeadlinePriority = listOf(
    LoadFailureKind.RateLimited,
    LoadFailureKind.Unavailable,
    LoadFailureKind.GatewayError,
    LoadFailureKind.Timeout,
    LoadFailureKind.DnsFailure,
    LoadFailureKind.Network,
    LoadFailureKind.SecureConnectionFailed,
    LoadFailureKind.ServerError,
    LoadFailureKind.Unauthorized,
    LoadFailureKind.Forbidden,
    LoadFailureKind.LegalBlock,
    LoadFailureKind.BadRequest,
    LoadFailureKind.InvalidResponse,
    LoadFailureKind.EmptyResponse,
    LoadFailureKind.Unknown,
    LoadFailureKind.NotFound,
)
