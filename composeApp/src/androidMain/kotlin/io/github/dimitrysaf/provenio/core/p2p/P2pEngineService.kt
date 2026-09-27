package io.github.dimitrysaf.provenio.core.p2p

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import io.github.dimitrysaf.provenio.core.settings.AppIconPlatform
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
import provenio.composeapp.generated.resources.p2p_engine_notification_connecting
import provenio.composeapp.generated.resources.p2p_engine_notification_stop
import provenio.composeapp.generated.resources.p2p_engine_notification_streaming
import provenio.composeapp.generated.resources.p2p_engine_notification_title

private const val EngineChannelId = "provenio_torrent_engine"
private const val EngineNotificationId = 0x5032
private const val ActionStart = "io.github.dimitrysaf.provenio.engine.START"
private const val ActionStop = "io.github.dimitrysaf.provenio.engine.STOP"
private const val Tag = "P2pEngineService"

// Keeps the process in the foreground while a torrent streams, so Android neither freezes the engine nor cuts its network in the background.
class P2pEngineService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ActionStop) {
            P2pStreamingEngine.shutdown()
            stopSelf()
            return START_NOT_STICKY
        }
        val started = runCatching {
            ServiceCompat.startForeground(
                this,
                EngineNotificationId,
                P2pEngineKeepAlive.buildNotification(this, P2pStreamingEngine.state.value),
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

// Starts the service while the engine connects or streams, keeps its notification current and stops it when the engine goes idle.
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
        scope.launch {
            P2pStreamingEngine.state
                .map { notificationText(it) }
                .distinctUntilChanged()
                .collect {
                    val state = P2pStreamingEngine.state.value
                    if (!isActive(state)) return@collect
                    runCatching {
                        appContext.getSystemService(NotificationManager::class.java)
                            ?.notify(EngineNotificationId, buildNotification(appContext, state))
                    }
                }
        }
    }

    fun isActive(state: P2pStreamingState): Boolean =
        state is P2pStreamingState.Connecting || state is P2pStreamingState.Streaming

    fun buildNotification(context: Context, state: P2pStreamingState): Notification {
        val openApp = PendingIntent.getActivity(
            context,
            EngineNotificationId,
            Intent().apply {
                component = AppIconPlatform.currentLauncherComponent(context)
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            context,
            EngineNotificationId + 1,
            Intent(context, P2pEngineService::class.java).setAction(ActionStop),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, EngineChannelId)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle(runBlocking { getString(Res.string.p2p_engine_notification_title) })
            .setContentText(notificationText(state))
            .setContentIntent(openApp)
            .addAction(0, runBlocking { getString(Res.string.p2p_engine_notification_stop) }, stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun notificationText(state: P2pStreamingState): String = runBlocking {
        when (state) {
            is P2pStreamingState.Streaming -> getString(
                Res.string.p2p_engine_notification_streaming,
                formatP2pSpeed(state.downloadSpeed),
                state.peers,
            )
            else -> getString(Res.string.p2p_engine_notification_connecting)
        }
    }

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
