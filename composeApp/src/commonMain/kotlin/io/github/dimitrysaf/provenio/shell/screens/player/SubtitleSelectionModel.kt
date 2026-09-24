package io.github.dimitrysaf.provenio.shell.screens.player

import io.github.dimitrysaf.provenio.core.playback.AddonSubtitle
import io.github.dimitrysaf.provenio.core.playback.SubtitleOffLanguageKey
import io.github.dimitrysaf.provenio.core.playback.SubtitleSelectionOption
import io.github.dimitrysaf.provenio.core.playback.SubtitleTrack
import io.github.dimitrysaf.provenio.core.playback.subtitleLanguageKey

internal fun selectedSubtitleLanguageKey(
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    selectedAddonSubtitle: AddonSubtitle?,
): String {
    selectedAddonSubtitle?.let { return subtitleLanguageKey(it.language) }
    return subtitleTracks
        .firstOrNull { it.index == selectedSubtitleIndex }
        ?.subtitleLanguageKey()
        ?: subtitleTracks.firstOrNull { it.isSelected }?.subtitleLanguageKey()
        ?: SubtitleOffLanguageKey
}

internal fun selectedSubtitleOptionId(
    subtitleTracks: List<SubtitleTrack>,
    selectedSubtitleIndex: Int,
    selectedAddonSubtitle: AddonSubtitle?,
): String? {
    selectedAddonSubtitle?.let { subtitle -> return SubtitleSelectionOption.Addon(subtitle).id }
    return subtitleTracks
        .firstOrNull { it.index == selectedSubtitleIndex }
        ?.let { SubtitleSelectionOption.BuiltIn(it) }
        ?.id
        ?: subtitleTracks
            .firstOrNull { it.isSelected }
            ?.let { SubtitleSelectionOption.BuiltIn(it) }
            ?.id
}
