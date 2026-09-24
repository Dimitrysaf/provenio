package io.github.dimitrysaf.provenio.core.notifications

// Scheduled release notifications need a background service, which desktop does not have yet.
internal actual object EpisodeReleaseNotificationPlatform {
    actual suspend fun notificationsAuthorized(): Boolean = false

    actual suspend fun requestAuthorization(): Boolean = false

    actual suspend fun scheduleEpisodeReleaseNotifications(requests: List<EpisodeReleaseNotificationRequest>) = Unit

    actual suspend fun clearScheduledEpisodeReleaseNotifications() = Unit

    actual suspend fun showTestNotification(request: EpisodeReleaseNotificationRequest) = Unit
}
