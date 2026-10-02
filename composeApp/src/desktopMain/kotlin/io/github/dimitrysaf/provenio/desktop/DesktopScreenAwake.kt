package io.github.dimitrysaf.provenio.desktop

import co.touchlab.kermit.Logger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.util.concurrent.Executors

internal object DesktopScreenAwake {
    private val log = Logger.withTag("ScreenAwake")
    private val osName = System.getProperty("os.name").orEmpty().lowercase()
    private val isWindows = osName.contains("windows")
    private val isLinux = osName.contains("linux")
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "provenio-screen-awake").apply { isDaemon = true }
    }

    private var holders = 0
    private var connection: Pointer? = null
    private var portalRequest: String? = null
    private var screenSaverCookie: String? = null

    @Synchronized
    fun hold() {
        holders += 1
        if (holders == 1) worker.execute(::inhibit)
    }

    @Synchronized
    fun release() {
        if (holders == 0) return
        holders -= 1
        if (holders == 0) worker.execute(::uninhibit)
    }

    private fun inhibit() {
        runCatching {
            when {
                isWindows -> kernel32.SetThreadExecutionState(EsContinuous or EsSystemRequired or EsDisplayRequired)
                isLinux -> inhibitLinux()
                else -> Unit
            }
        }.onFailure { log.w(it) { "Could not keep the screen awake" } }
    }

    private fun uninhibit() {
        runCatching {
            when {
                isWindows -> kernel32.SetThreadExecutionState(EsContinuous)
                isLinux -> uninhibitLinux()
                else -> Unit
            }
        }.onFailure { log.w(it) { "Could not let the screen sleep again" } }
    }

    private fun inhibitLinux() {
        val bus = connection
            ?: DesktopDbus.gio.g_bus_get_sync(GBusTypeSession, null, null)?.also { connection = it }
            ?: return
        portalRequest = DesktopDbus.call(
            busName = "org.freedesktop.portal.Desktop",
            objectPath = "/org/freedesktop/portal/desktop",
            interfaceName = "org.freedesktop.portal.Inhibit",
            method = "Inhibit",
            arguments = "('', uint32 $PortalInhibitIdle, {'reason': <${InhibitReason.toGVariantString()}>})",
            sharedConnection = bus,
        )?.let { reply -> QuotedValue.find(reply)?.groupValues?.get(1) }
        if (portalRequest != null) return
        screenSaverCookie = DesktopDbus.call(
            busName = "org.freedesktop.ScreenSaver",
            objectPath = "/org/freedesktop/ScreenSaver",
            interfaceName = "org.freedesktop.ScreenSaver",
            method = "Inhibit",
            arguments = "(${AppName.toGVariantString()}, ${InhibitReason.toGVariantString()})",
            sharedConnection = bus,
        )?.let { reply -> CookieValue.find(reply)?.groupValues?.get(1) }
        if (screenSaverCookie == null) log.w { "No screen saver service to keep the screen awake" }
    }

    private fun uninhibitLinux() {
        val bus = connection ?: return
        portalRequest?.let { request ->
            DesktopDbus.call(
                busName = "org.freedesktop.portal.Desktop",
                objectPath = request,
                interfaceName = "org.freedesktop.portal.Request",
                method = "Close",
                arguments = "()",
                sharedConnection = bus,
            )
        }
        screenSaverCookie?.let { cookie ->
            DesktopDbus.call(
                busName = "org.freedesktop.ScreenSaver",
                objectPath = "/org/freedesktop/ScreenSaver",
                interfaceName = "org.freedesktop.ScreenSaver",
                method = "UnInhibit",
                arguments = "(uint32 $cookie,)",
                sharedConnection = bus,
            )
        }
        portalRequest = null
        screenSaverCookie = null
        connection = null
        DesktopDbus.gobject.g_object_unref(bus)
    }

    private val kernel32: Kernel32 by lazy { Native.load("kernel32", Kernel32::class.java) }

    @Suppress("FunctionName")
    private interface Kernel32 : Library {
        fun SetThreadExecutionState(flags: Int): Int
    }

    private val EsContinuous = 0x80000000.toInt()
    private const val EsSystemRequired = 0x00000001
    private const val EsDisplayRequired = 0x00000002
    private const val PortalInhibitIdle = 8
    private const val AppName = "Provenio"
    private const val InhibitReason = "Playing a video"
    private val QuotedValue = Regex("'([^']+)'")
    private val CookieValue = Regex("uint32 (\\d+)")
}
