package io.github.dimitrysaf.provenio.shell.screens.player

import io.github.dimitrysaf.provenio.core.addons.AddonResource
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import io.github.dimitrysaf.provenio.core.addons.enabledAddons
import io.github.dimitrysaf.provenio.core.playback.AddonSubtitle
import io.github.dimitrysaf.provenio.core.playback.SubtitleLanguageMatching
import io.github.dimitrysaf.provenio.core.playback.SubtitleLanguageOption
import io.github.dimitrysaf.provenio.core.playback.DeviceLanguagePreferences
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState
import io.github.dimitrysaf.provenio.core.playback.preferredSubtitleTargetsForSettings

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
