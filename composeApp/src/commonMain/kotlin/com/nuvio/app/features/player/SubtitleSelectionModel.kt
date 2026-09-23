package com.nuvio.app.features.player

import com.nuvio.app.core.playback.AddonSubtitle
import com.nuvio.app.core.playback.SubtitleOffLanguageKey
import com.nuvio.app.core.playback.SubtitleSelectionOption
import com.nuvio.app.core.playback.SubtitleTrack
import com.nuvio.app.core.playback.subtitleLanguageKey

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
