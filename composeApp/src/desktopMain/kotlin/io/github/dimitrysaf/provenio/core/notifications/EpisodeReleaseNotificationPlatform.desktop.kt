package io.github.dimitrysaf.provenio.core.notifications

import io.github.dimitrysaf.provenio.desktop.DesktopDbus
import io.github.dimitrysaf.provenio.desktop.toGVariantString
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.prefs.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ShownKey = "episode_release_notifications_shown"
private const val CheckIntervalMs = 15 * 60 * 1_000L
private const val AppName = "Provenio"

internal actual object EpisodeReleaseNotificationPlatform {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences: Preferences = Preferences.userRoot().node("io/github/dimitrysaf/provenio")
    private var scheduled: List<EpisodeReleaseNotificationRequest> = emptyList()
    private var job: Job? = null
    private var trayIcon: TrayIcon? = null

    private val osName = System.getProperty("os.name").orEmpty().lowercase()
    private val isLinux = osName.contains("linux")
    private val isFlatpak = System.getenv("FLATPAK_ID") != null || File("/.flatpak-info").exists()

    actual suspend fun notificationsAuthorized(): Boolean = isLinux || SystemTray.isSupported()

    actual suspend fun requestAuthorization(): Boolean = notificationsAuthorized()

    actual suspend fun scheduleEpisodeReleaseNotifications(requests: List<EpisodeReleaseNotificationRequest>) {
        scheduled = requests
        if (job?.isActive == true) {
            scope.launch { showDue() }
            return
        }
        job = scope.launch {
            while (true) {
                showDue()
                delay(CheckIntervalMs)
            }
        }
    }

    actual suspend fun clearScheduledEpisodeReleaseNotifications() {
        job?.cancel()
        job = null
        scheduled = emptyList()
    }

    actual suspend fun showTestNotification(request: EpisodeReleaseNotificationRequest) {
        withContext(Dispatchers.IO) { show(request) }
    }

    private fun showDue() {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val shown = preferences.get(ShownKey, "").split('\n').filter { it.isNotBlank() }.toMutableSet()
        val due = scheduled.filter { request ->
            val date = runCatching { LocalDate.parse(request.releaseDateIso) }.getOrNull() ?: return@filter false
            val releaseTime = date.atTime(EpisodeReleaseNotificationHour, EpisodeReleaseNotificationMinute)
            request.requestId !in shown && !now.isBefore(releaseTime) && !date.isBefore(now.toLocalDate().minusDays(1))
        }
        if (due.isEmpty()) return
        due.forEach { request ->
            show(request)
            shown += request.requestId
        }
        val keep = shown.toList().takeLast(500)
        preferences.put(ShownKey, keep.joinToString("\n"))
    }

    private fun show(request: EpisodeReleaseNotificationRequest) {
        if (isLinux && showOnLinux(request)) return
        showInTray(request)
    }

    private fun showOnLinux(request: EpisodeReleaseNotificationRequest): Boolean {
        val title = request.notificationTitle.toGVariantString()
        val body = request.notificationBody.toGVariantString()
        val viaPortal = {
            DesktopDbus.call(
                busName = "org.freedesktop.portal.Desktop",
                objectPath = "/org/freedesktop/portal/desktop",
                interfaceName = "org.freedesktop.portal.Notification",
                method = "AddNotification",
                arguments = "(${request.requestId.toGVariantString()}, {'title': <$title>, 'body': <$body>})",
            ) != null
        }
        val viaNotificationServer = {
            DesktopDbus.call(
                busName = "org.freedesktop.Notifications",
                objectPath = "/org/freedesktop/Notifications",
                interfaceName = "org.freedesktop.Notifications",
                method = "Notify",
                arguments = "(${AppName.toGVariantString()}, uint32 0, 'io.github.dimitrysaf.Provenio', $title, $body, @as [], @a{sv} {}, -1)",
            ) != null
        }
        return if (isFlatpak) viaPortal() || viaNotificationServer() else viaNotificationServer() || viaPortal()
    }

    private fun trayImage(): java.awt.Image {
        val resource = EpisodeReleaseNotificationPlatform::class.java.classLoader
            ?.getResource("composeResources/provenio.composeapp.generated.resources/drawable/app_icon_arctic_blue.png")
        return if (resource != null) Toolkit.getDefaultToolkit().getImage(resource) else Toolkit.getDefaultToolkit().createImage(ByteArray(0))
    }

    private fun showInTray(request: EpisodeReleaseNotificationRequest) {
        if (!SystemTray.isSupported()) return
        runCatching {
            val icon = trayIcon ?: TrayIcon(trayImage(), AppName).also {
                it.isImageAutoSize = true
                SystemTray.getSystemTray().add(it)
                trayIcon = it
            }
            icon.displayMessage(request.notificationTitle, request.notificationBody, TrayIcon.MessageType.INFO)
        }
    }
}
