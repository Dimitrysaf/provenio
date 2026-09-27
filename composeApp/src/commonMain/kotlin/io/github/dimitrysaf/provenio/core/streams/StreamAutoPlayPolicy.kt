package io.github.dimitrysaf.provenio.core.streams

import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsUiState

object StreamAutoPlayPolicy {
    fun isEffectivelyEnabled(settings: PlayerSettingsUiState): Boolean {
        if (settings.streamReuseLastLinkEnabled) return true
        if (settings.streamAutoPlayReuseBingeGroup && settings.streamAutoPlayPreferBingeGroup) return true

        return when (settings.streamAutoPlayMode) {
            StreamAutoPlayMode.MANUAL -> false
            StreamAutoPlayMode.FIRST_STREAM -> true
            StreamAutoPlayMode.REGEX_MATCH -> isRegexSelectionConfigured(settings.streamAutoPlayRegex)
        }
    }

    // The rule the streams request uses to go straight to playback: auto-play on, or a remembered binge group for this title.
    fun usesDirectAutoPlay(settings: PlayerSettingsUiState, parentMetaId: String?): Boolean {
        val autoPlayOn = when (settings.streamAutoPlayMode) {
            StreamAutoPlayMode.MANUAL -> false
            StreamAutoPlayMode.FIRST_STREAM -> true
            StreamAutoPlayMode.REGEX_MATCH -> isRegexSelectionConfigured(settings.streamAutoPlayRegex)
        }
        if (autoPlayOn) return true
        return settings.streamAutoPlayPreferBingeGroup &&
            settings.streamAutoPlayReuseBingeGroup &&
            parentMetaId?.let { BingeGroupCacheRepository.get(it) } != null
    }

    fun isRegexSelectionConfigured(regexPattern: String): Boolean {
        val pattern = regexPattern.trim()
        if (pattern.isEmpty() || !pattern.any { it.isLetterOrDigit() }) return false
        return runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }.isSuccess
    }
}
