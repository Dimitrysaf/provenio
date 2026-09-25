package io.github.dimitrysaf.provenio.core.auth

import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The device's own user: there are no accounts, so everything belongs to one local identity. */
object AuthRepository {
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    fun initialize() {
        if (_state.value is AuthState.Authenticated) return
        // A device that used to sign in keeps its profiles by adopting the ID they were saved under.
        val userId = ProfileRepository.storedUserId()
            ?: AuthStorage.loadAnonymousUserId()
            ?: Uuid.random().toString()
        AuthStorage.saveAnonymousUserId(userId)
        _state.value = AuthState.Authenticated(userId = userId, email = null, isAnonymous = true)
    }
}
