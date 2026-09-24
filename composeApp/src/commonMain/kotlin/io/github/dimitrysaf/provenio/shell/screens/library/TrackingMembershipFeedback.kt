package io.github.dimitrysaf.provenio.shell.screens.library

import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.core.tracking.TrackingLibraryTab
import io.github.dimitrysaf.provenio.core.tracking.TrackingMembershipApplyResult
import io.github.dimitrysaf.provenio.core.tracking.TrackingProviderRegistry
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.tracking_list_status_rewritten
import org.jetbrains.compose.resources.getString
import io.github.dimitrysaf.provenio.core.library.statusTitle

internal suspend fun showTrackingMembershipRewriteFeedback(result: TrackingMembershipApplyResult) {
    val rewrite = result.rewrites.firstOrNull() ?: return
    val providerName = TrackingProviderRegistry.authProvider(rewrite.providerId)
        ?.descriptor
        ?.displayName
        ?: rewrite.providerId.storageId.replaceFirstChar { char -> char.titlecase() }
    val tabs = TrackingProviderRegistry.libraryProvider(rewrite.providerId)?.snapshot()?.tabs.orEmpty()
    val requestedTitle = tabs.statusTitle(rewrite.requestedListKey, providerName)
    val resolvedTitle = tabs.statusTitle(rewrite.resolvedListKey, providerName)
    ToastController.show(
        getString(
            Res.string.tracking_list_status_rewritten,
            providerName,
            resolvedTitle,
            requestedTitle,
        ),
    )
}
