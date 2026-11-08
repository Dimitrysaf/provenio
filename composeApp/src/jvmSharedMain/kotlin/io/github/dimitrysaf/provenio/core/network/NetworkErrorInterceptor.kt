package io.github.dimitrysaf.provenio.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Turns the runtime errors some devices throw while setting up TLS back into the [IOException] they
 * wrap, so callers that handle failed requests handle these too instead of crashing.
 */
class NetworkErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        try {
            chain.proceed(chain.request())
        } catch (error: RuntimeException) {
            throw error.cause as? IOException ?: error
        }
}
