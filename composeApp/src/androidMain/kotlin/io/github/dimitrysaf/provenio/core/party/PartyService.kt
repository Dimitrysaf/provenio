package io.github.dimitrysaf.provenio.core.party

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.dimitrysaf.provenio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.watch_party_notification_channel
import provenio.composeapp.generated.resources.watch_party_notification_text
import provenio.composeapp.generated.resources.watch_party_title

private const val PartyChannelId = "provenio_watch_party"
private const val PartyNotificationId = 0x5051
private const val ActionStart = "io.github.dimitrysaf.provenio.party.START"
private const val Tag = "PartyService"

// Keeps the process in the foreground while this device hosts or attends a room, so Android
// neither freezes the room's connections nor cuts its network in the background.
class PartyService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                PartyNotificationId,
                PartyKeepAlive.buildNotification(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        }.onFailure { Log.w(Tag, "Could not keep the watch party in the foreground", it) }.isSuccess
        if (!started || PartyRepository.uiState.value.phase == PartyPhase.Idle) stopSelf()
        return START_NOT_STICKY
    }

    // Swiping Provenio away ends the room; nobody would be left to run it.
    override fun onTaskRemoved(rootIntent: Intent?) {
        PartyRepository.leave()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

// Starts the service while a room is open or on its way, and stops it when it closes.
internal object PartyKeepAlive {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        ensureChannel(appContext)
        scope.launch {
            PartyRepository.uiState
                .map { it.phase != PartyPhase.Idle }
                .distinctUntilChanged()
                .collect { active ->
                    val intent = Intent(appContext, PartyService::class.java).setAction(ActionStart)
                    if (active) {
                        runCatching { ContextCompat.startForegroundService(appContext, intent) }
                            .onFailure { Log.w(Tag, "Could not start the watch party service", it) }
                    } else {
                        appContext.stopService(intent)
                    }
                }
        }
    }

    fun buildNotification(context: Context): Notification =
        NotificationCompat.Builder(context, PartyChannelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(runBlocking { getString(Res.string.watch_party_title) })
            .setContentText(runBlocking { getString(Res.string.watch_party_notification_text) })
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(PartyChannelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                PartyChannelId,
                runBlocking { getString(Res.string.watch_party_notification_channel) },
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }
}
