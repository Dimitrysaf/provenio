package io.github.dimitrysaf.provenio.core.membership

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Without an account there is no membership to look up, so the overview stays inactive.
object MembershipOverviewRepository {
    private val _state = MutableStateFlow(MembershipOverviewState(overview = MembershipOverview(), isLoading = false))
    val state: StateFlow<MembershipOverviewState> = _state.asStateFlow()

    fun ensureStarted() = Unit

    fun refresh() = Unit
}
