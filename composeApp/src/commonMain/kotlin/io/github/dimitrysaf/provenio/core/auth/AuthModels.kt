package io.github.dimitrysaf.provenio.core.auth

sealed interface AuthState {
    data object Loading : AuthState
    data class Authenticated(
        val userId: String,
        val email: String?,
        val isAnonymous: Boolean,
    ) : AuthState
}

val AuthState.userId: String?
    get() = (this as? AuthState.Authenticated)?.userId

val AuthState.isAnonymous: Boolean
    get() = (this as? AuthState.Authenticated)?.isAnonymous == true
