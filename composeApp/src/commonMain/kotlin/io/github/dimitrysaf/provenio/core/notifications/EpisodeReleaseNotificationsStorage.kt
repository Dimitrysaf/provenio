package io.github.dimitrysaf.provenio.core.notifications

internal expect object EpisodeReleaseNotificationsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}