package io.github.dimitrysaf.provenio.stremio

/**
 * The outcome of an addon request.
 *
 * Addons are third-party servers of wildly varying quality: they go offline, answer with
 * an HTML error page, or return JSON that does not match the protocol. Returning a result
 * rather than throwing keeps those three cases distinguishable at the call site, so the
 * UI can say which one happened instead of reporting an empty catalog.
 */
sealed interface AddonResult<out T> {

    data class Success<T>(val value: T) : AddonResult<T>

    /** The addon answered, but not with 2xx. */
    data class HttpError(val code: Int, val description: String? = null) : AddonResult<Nothing>

    /** The addon answered with 2xx, but the body was not the expected shape. */
    data class ParseError(val cause: Throwable) : AddonResult<Nothing>

    /** The addon could not be reached at all, including timeouts. */
    data class NetworkError(val cause: Throwable) : AddonResult<Nothing>
}

fun <T> AddonResult<T>.getOrNull(): T? = (this as? AddonResult.Success)?.value

inline fun <T, R> AddonResult<T>.map(transform: (T) -> R): AddonResult<R> = when (this) {
    is AddonResult.Success -> AddonResult.Success(transform(value))
    is AddonResult.HttpError -> this
    is AddonResult.ParseError -> this
    is AddonResult.NetworkError -> this
}
