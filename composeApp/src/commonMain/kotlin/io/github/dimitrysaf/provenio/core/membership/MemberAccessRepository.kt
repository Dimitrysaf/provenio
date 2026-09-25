package io.github.dimitrysaf.provenio.core.membership

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The supporter themes need no server, so every copy of the app has them unlocked.
private val LocalAccess = MemberAccess(
    entitlements = CosmeticEntitlements(
        setOf(
            CosmeticEntitlement.GOLD_THEME,
            CosmeticEntitlement.JADE_THEME,
            CosmeticEntitlement.ROSE_GOLD_THEME,
            CosmeticEntitlement.ARCTIC_BLUE_THEME,
            CosmeticEntitlement.GRAPHITE_THEME,
        ),
    ),
)

object MemberAccessRepository {
    private val _access = MutableStateFlow(LocalAccess)
    val access: StateFlow<MemberAccess> = _access.asStateFlow()

    fun ensureStarted() = Unit

    fun clearLocalState() {
        _access.value = LocalAccess
        MemberAssetStorage.clearAccess()
        ProfileBackgroundRepository.invalidate()
    }
}
