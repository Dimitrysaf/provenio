package io.github.dimitrysaf.provenio.simkl

import io.github.dimitrysaf.provenio.data.SimklStore
import io.github.dimitrysaf.provenio.data.createDatabaseDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Simkl sign in and the resulting token.
 *
 * Sign in is the PIN flow: ask Simkl for a short code, show it, and poll until the user has
 * entered it on simkl.com. Nothing here needs a redirect URI or a client secret, so the
 * same code works on both platforms.
 */
object SimklRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client = SimklClient()

    private val _authState = MutableStateFlow<SimklAuthState>(SimklAuthState.SignedOut)
    val authState: StateFlow<SimklAuthState> = _authState.asStateFlow()

    var accessToken: String? = null
        private set

    private var store: SimklStore? = null
    private var loaded = false
    private var pollJob: Job? = null

    fun load() {
        if (loaded) return
        loaded = true
        store = runCatching { SimklStore(createDatabaseDriver()) }.getOrNull()
        accessToken = store?.let { runCatching { it.token() }.getOrNull() }
        if (accessToken != null) _authState.value = SimklAuthState.SignedIn
    }

    fun signIn() {
        if (!SimklConfig.isConfigured) {
            _authState.value = SimklAuthState.Error("This build has no Simkl client id.")
            return
        }
        pollJob?.cancel()
        _authState.value = SimklAuthState.Starting

        pollJob = scope.launch {
            val pin = client.requestPin()
            val code = pin?.userCode
            if (pin == null || code == null) {
                _authState.value = SimklAuthState.Error("Simkl did not issue a code.")
                return@launch
            }

            val interval = (pin.pollIntervalSeconds ?: DefaultPollSeconds).coerceAtLeast(1)
            var remaining = pin.expiresInSeconds ?: DefaultExpirySeconds

            _authState.value = SimklAuthState.AwaitingUser(
                userCode = code,
                verificationPage = pin.verificationPage,
                secondsRemaining = remaining,
            )

            while (isActive && remaining > 0) {
                delay(interval * 1000L)
                remaining -= interval

                when (val status = client.pollPin(code)) {
                    is PinStatus.Authorized -> {
                        completeSignIn(status.accessToken)
                        return@launch
                    }
                    is PinStatus.Failed -> {
                        _authState.value = SimklAuthState.Error(
                            status.message ?: "Simkl refused the code.",
                        )
                        return@launch
                    }
                    PinStatus.Pending -> _authState.value = SimklAuthState.AwaitingUser(
                        userCode = code,
                        verificationPage = pin.verificationPage,
                        secondsRemaining = remaining.coerceAtLeast(0),
                    )
                }
            }

            if (isActive) {
                _authState.value = SimklAuthState.Error("The code expired. Try again.")
            }
        }
    }

    fun cancelSignIn() {
        pollJob?.cancel()
        pollJob = null
        _authState.value = if (accessToken != null) {
            SimklAuthState.SignedIn
        } else {
            SimklAuthState.SignedOut
        }
    }

    fun signOut() {
        pollJob?.cancel()
        pollJob = null
        accessToken = null
        store?.let { runCatching { it.clear() } }
        _authState.value = SimklAuthState.SignedOut
    }

    /** Called when a request comes back 401, which means the user revoked access. */
    fun onTokenRejected() = signOut()

    private fun completeSignIn(token: String) {
        accessToken = token
        store?.let { runCatching { it.save(token) } }
        _authState.value = SimklAuthState.SignedIn
    }

    private const val DefaultPollSeconds = 5
    private const val DefaultExpirySeconds = 900
}
