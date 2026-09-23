package com.nuvio.app.features.player

import com.nuvio.app.core.addons.AddonResource
import com.nuvio.app.core.addons.ManagedAddon
import com.nuvio.app.core.addons.enabledAddons
import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.SubtitleLanguageMatching
import com.nuvio.app.core.playback.SubtitleLanguageOption

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

internal fun preferredSubtitleTargetsForSettings(settings: PlayerSettingsUiState): List<String> {
    return resolvePreferredSubtitleLanguageTargets(
        preferredSubtitleLanguage = settings.preferredSubtitleLanguage,
        secondaryPreferredSubtitleLanguage = settings.secondaryPreferredSubtitleLanguage,
        deviceLanguages = DeviceLanguagePreferences.preferredLanguageCodes(),
    ).filterNot { it == SubtitleLanguageOption.FORCED }
}
