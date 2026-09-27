package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState

// Before the request has decided, it predicts with the same rule the request uses, so the loading artwork never flashes ahead of the sheet.
internal fun StreamsUiState.shouldShowAutoPlayLoading(
    expectedRequestToken: String,
    settings: PlayerSettingsUiState,
    manualSelection: Boolean,
    parentMetaId: String?,
    hasReusableLink: Boolean,
): Boolean =
    if (requestToken == expectedRequestToken && autoPlayDecided) {
        showDirectAutoPlayOverlay
    } else {
        !manualSelection && (hasReusableLink || StreamAutoPlayPolicy.usesDirectAutoPlay(settings, parentMetaId))
    }

internal fun StreamsUiState.shouldUseLandscapeAutoPlayLoading(
    expectedRequestToken: String,
    manualSelection: Boolean,
): Boolean =
    !manualSelection &&
        requestToken == expectedRequestToken &&
        autoPlayDecided &&
        isDirectAutoPlayFlow &&
        showDirectAutoPlayOverlay

internal fun List<AddonStreamGroup>.areAutoPlaySourcesLoaded(
    source: StreamAutoPlaySource,
    installedAddonIds: Set<String>,
): Boolean = none { group ->
    group.isLoading && when (source) {
        StreamAutoPlaySource.ALL_SOURCES -> true
        StreamAutoPlaySource.INSTALLED_ADDONS_ONLY -> group.addonId in installedAddonIds
        StreamAutoPlaySource.ENABLED_PLUGINS_ONLY -> group.addonId !in installedAddonIds
    }
}
