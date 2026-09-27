package io.github.dimitrysaf.provenio.core.p2p

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
import provenio.composeapp.generated.resources.p2p_engine_channel_name
import provenio.composeapp.generated.resources.p2p_engine_notification_text

private const val EngineChannelId = "provenio_torrent_engine"
private const val EngineNotificationId = 0x5032
private const val ActionStart = "io.github.dimitrysaf.provenio.engine.START"
private const val Tag = "P2pEngineService"

// Keeps the process in the foreground while a torrent streams, so Android neither freezes the engine nor cuts its network in the background.
class P2pEngineService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                EngineNotificationId,
                P2pEngineKeepAlive.buildNotification(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
        }.onFailure { Log.w(Tag, "Could not keep the engine in the foreground", it) }.isSuccess
        if (!started || !P2pEngineKeepAlive.isActive(P2pStreamingEngine.state.value)) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

// Starts the service while the engine connects or streams and stops it when the engine goes idle.
internal object P2pEngineKeepAlive {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        ensureChannel(appContext)
        scope.launch {
            P2pStreamingEngine.state
                .map { isActive(it) }
                .distinctUntilChanged()
                .collect { active ->
                    val intent = Intent(appContext, P2pEngineService::class.java).setAction(ActionStart)
                    if (active) {
                        runCatching { ContextCompat.startForegroundService(appContext, intent) }
                            .onFailure { Log.w(Tag, "Could not start the engine service", it) }
                    } else {
                        appContext.stopService(intent)
                    }
                }
        }
    }

    fun isActive(state: P2pStreamingState): Boolean =
        state is P2pStreamingState.Connecting || state is P2pStreamingState.Streaming

    fun buildNotification(context: Context): Notification =
        NotificationCompat.Builder(context, EngineChannelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("Provenio")
            .setContentText(runBlocking { getString(Res.string.p2p_engine_notification_text) })
            .setOngoing(true)
            .setSilent(true)
            .build()

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(EngineChannelId) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                EngineChannelId,
                runBlocking { getString(Res.string.p2p_engine_channel_name) },
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) },
        )
    }
}
