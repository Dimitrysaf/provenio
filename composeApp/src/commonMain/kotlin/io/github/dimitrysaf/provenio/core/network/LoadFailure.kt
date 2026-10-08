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

/** Thrown when a server answers successfully but has nothing for the requested item. */
class NoContentException(message: String) : IllegalStateException(message)

/** Why something could not be loaded, in terms a user can act on. */
enum class LoadFailureKind {
    /** The source answered, but does not have this item (yet). */
    NotFound,

    /** The source is getting too many requests and asked us to slow down. */
    RateLimited,

    /** The source refused the request, usually a configuration or key problem. */
    Refused,

    /** The source's server is failing. */
    ServerError,

    /** The source did not answer in time. */
    Timeout,

    /** The source could not be reached at all. */
    Network,

    /** The source answered with something that could not be read. */
    InvalidResponse,

    Unknown,
}

/** One source's failure: [sourceName] is shown to the user, so it is the add-on or service name. */
data class LoadFailure(
    val sourceName: String,
    val kind: LoadFailureKind,
    val statusCode: Int? = null,
)

fun Throwable.toLoadFailureKind(): LoadFailureKind {
    if (this is TimeoutCancellationException) return LoadFailureKind.Timeout
    if (this is NoContentException) return LoadFailureKind.NotFound
    if (this is HttpStatusException) {
        return when (statusCode) {
            404, 410 -> LoadFailureKind.NotFound
            429 -> LoadFailureKind.RateLimited
            401, 403 -> LoadFailureKind.Refused
            408 -> LoadFailureKind.Timeout
            in 500..599 -> LoadFailureKind.ServerError
            else -> LoadFailureKind.Unknown
        }
    }
    // Platform network exceptions are not visible from common code, so match them by name.
    val name = this::class.simpleName.orEmpty()
    return when {
        name.contains("Timeout", ignoreCase = true) -> LoadFailureKind.Timeout
        name.contains("UnknownHost", ignoreCase = true) ||
            name.contains("Connect", ignoreCase = true) ||
            name.contains("NoRouteToHost", ignoreCase = true) ||
            name.contains("SSL", ignoreCase = true) ||
            name.contains("IOException", ignoreCase = true) ||
            name.contains("Darwin", ignoreCase = true) -> LoadFailureKind.Network
        name.contains("Serialization", ignoreCase = true) ||
            name.contains("JsonDecoding", ignoreCase = true) ||
            this is IllegalArgumentException -> LoadFailureKind.InvalidResponse
        else -> LoadFailureKind.Unknown
    }
}

fun Throwable.toLoadFailure(sourceName: String): LoadFailure =
    LoadFailure(
        sourceName = sourceName,
        kind = toLoadFailureKind(),
        statusCode = (this as? HttpStatusException)?.statusCode,
    )

/**
 * The failure to tell the user about when several sources failed. A source that is busy or down
 * may still have the item, so those come before "not found": saying a title doesn't exist when one
 * add-on was only rate-limited would be wrong.
 */
fun List<LoadFailure>.mostActionable(): LoadFailure? =
    minByOrNull { failure ->
        when (failure.kind) {
            LoadFailureKind.RateLimited -> 0
            LoadFailureKind.Network -> 1
            LoadFailureKind.Timeout -> 2
            LoadFailureKind.ServerError -> 3
            LoadFailureKind.Refused -> 4
            LoadFailureKind.InvalidResponse -> 5
            LoadFailureKind.Unknown -> 6
            LoadFailureKind.NotFound -> 7
        }
    }
