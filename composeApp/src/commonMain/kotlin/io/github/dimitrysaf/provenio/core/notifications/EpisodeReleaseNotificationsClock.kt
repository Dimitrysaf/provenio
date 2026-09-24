package io.github.dimitrysaf.provenio.core.notifications

internal expect object EpisodeReleaseNotificationsClock {
    fun isoDateFromEpochMs(epochMs: Long): String
}