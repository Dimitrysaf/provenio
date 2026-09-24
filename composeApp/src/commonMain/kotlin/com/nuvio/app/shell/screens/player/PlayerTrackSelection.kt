package com.nuvio.app.shell.screens.player

import com.nuvio.app.core.addons.AddonResource
import com.nuvio.app.core.addons.ManagedAddon
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.SubtitleLanguageMatching
import com.nuvio.app.core.playback.SubtitleLanguageOption
import com.nuvio.app.core.playback.DeviceLanguagePreferences
import com.nuvio.app.core.playback.PlayerSettingsUiState
import com.nuvio.app.core.playback.preferredSubtitleTargetsForSettings

internal fun filterAddonSubtitlesForSettings(
    subtitles: List<AddonSubtitle>,
    settings: PlayerSettingsUiState,
): List<AddonSubtitle> {
    val shouldFilter = settings.subtitleStyle.showOnlyPreferredLanguages
    if (!shouldFilter) return subtitles

    val targets = preferredSubtitleTargetsForSettings(settings)
    if (targets.isEmpty()) return emptyList()

    return subtitles.filter { subtitle ->
        targets.any { target ->
            SubtitleLanguageMatching.matchesLanguageCode(subtitle.language, target)
        }
    }
}
