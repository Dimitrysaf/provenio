package com.nuvio.app.shell.screens.library

import com.nuvio.app.shell.components.NuvioToastController
import com.nuvio.app.core.tracking.TrackingLibraryTab
import com.nuvio.app.core.tracking.TrackingMembershipApplyResult
import com.nuvio.app.core.tracking.TrackingProviderRegistry
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.tracking_list_status_rewritten
import org.jetbrains.compose.resources.getString
import com.nuvio.app.core.library.statusTitle

internal suspend fun showTrackingMembershipRewriteFeedback(result: TrackingMembershipApplyResult) {
    val rewrite = result.rewrites.firstOrNull() ?: return
    val providerName = TrackingProviderRegistry.authProvider(rewrite.providerId)
        ?.descriptor
        ?.displayName
        ?: rewrite.providerId.storageId.replaceFirstChar { char -> char.titlecase() }
    val tabs = TrackingProviderRegistry.libraryProvider(rewrite.providerId)?.snapshot()?.tabs.orEmpty()
    val requestedTitle = tabs.statusTitle(rewrite.requestedListKey, providerName)
    val resolvedTitle = tabs.statusTitle(rewrite.resolvedListKey, providerName)
    NuvioToastController.show(
        getString(
            Res.string.tracking_list_status_rewritten,
            providerName,
            resolvedTitle,
            requestedTitle,
        ),
    )
}
