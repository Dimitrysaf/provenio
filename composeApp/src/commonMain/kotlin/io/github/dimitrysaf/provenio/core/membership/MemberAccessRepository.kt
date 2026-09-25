package io.github.dimitrysaf.provenio.core.membership

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Supporter access used to come from the account server; a local app has none to verify.
object MemberAccessRepository {
    private val _access = MutableStateFlow(MemberAccess.None)
    val access: StateFlow<MemberAccess> = _access.asStateFlow()

    fun ensureStarted() = Unit

    fun clearLocalState() {
        _access.value = MemberAccess.None
        MemberAssetStorage.clearAccess()
        ProfileBackgroundRepository.invalidate()
    }
}
