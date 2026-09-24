package com.nuvio.app.core.streams

import com.nuvio.app.core.playback.PlayerSettingsUiState

internal fun StreamsUiState.shouldShowAutoPlayLoading(
    expectedRequestToken: String,
    settings: PlayerSettingsUiState,
    manualSelection: Boolean,
): Boolean =
    if (requestToken == expectedRequestToken && autoPlayDecided) {
        showDirectAutoPlayOverlay
    } else {
        !manualSelection && StreamAutoPlayPolicy.isEffectivelyEnabled(settings)
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
