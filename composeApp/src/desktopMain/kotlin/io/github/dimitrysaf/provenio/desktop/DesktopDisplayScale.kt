package io.github.dimitrysaf.provenio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import io.github.dimitrysaf.provenio.core.settings.ThemeSettingsRepository
import java.awt.GraphicsEnvironment
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object DesktopDisplayScale {
    private val isLinux: Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("linux")

    private val x11: X11? by lazy {
        if (!isLinux) {
            null
        } else {
            listOf("libX11.so.6", "X11").firstNotNullOfOrNull { name ->
                runCatching { Native.load(name, X11::class.java) }.getOrNull()
            }
        }
    }

    private val _desktopScale = MutableStateFlow<Float?>(null)

    /** The scale the Linux desktop asks X11 apps to draw at, or null where the toolkit already applies it. */
    val desktopScale: StateFlow<Float?> = _desktopScale.asStateFlow()

    private var watching = false

    private var monitorsChanged: SignalCallback? = null

    fun start() {
        if (!isLinux || watching) return
        watching = true
        _desktopScale.value = readDesktopScale()
        Thread(::watchDesktopScale, "display-scale").apply { isDaemon = true }.start()
        Thread(::watchGnomeMonitors, "display-scale-gnome").apply { isDaemon = true }.start()
    }

    fun toolkitScale(): Float = runCatching {
        GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .defaultTransform
            .scaleX
            .toFloat()
    }.getOrDefault(1f).takeIf { it > 0f } ?: 1f

    fun initialWindowSize(size: DpSize): DpSize {
        ThemeSettingsRepository.ensureLoaded()
        val target = ThemeSettingsRepository.displayScale.value.scale ?: desktopScale.value ?: return size
        val extra = target / toolkitScale()
        if (abs(extra - 1f) < ScaleTolerance) return size
        val bounds = runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        }.getOrNull()
        val width = (size.width.value * extra).let { width ->
            bounds?.let { width.coerceAtMost(it.width * MaxWindowFraction) } ?: width
        }
        val height = (size.height.value * extra).let { height ->
            bounds?.let { height.coerceAtMost(it.height * MaxWindowFraction) } ?: height
        }
        return DpSize(width.dp, height.dp)
    }

    private fun readDesktopScale(): Float? {
        val scale = envScale("PROVENIO_SCALE")
            ?: gnomeScale()
            ?: xftDpi()?.let { it / BaseDpi }
            ?: envScale("GDK_SCALE")?.let { it * (envScale("GDK_DPI_SCALE") ?: 1f) }
            ?: envScale("QT_SCALE_FACTOR")
            ?: return null
        return scale.takeIf { it.isFinite() && it > 0f }?.coerceIn(MinScale, MaxScale)
    }

    // Blocks on the X server's event queue and wakes only when the desktop rewrites its resources, such as Xft.dpi.
    private fun watchDesktopScale() {
        val library = x11 ?: return
        runCatching {
            val display = library.XOpenDisplay(null) ?: return
            val root = library.XDefaultRootWindow(display)
            val resourceManager = library.XInternAtom(display, "RESOURCE_MANAGER", false)
            library.XSelectInput(display, root, NativeLong(PropertyChangeMask))
            val event = Memory(XEventSize)
            while (true) {
                library.XNextEvent(display, event)
                if (event.getInt(0) != PropertyNotify) continue
                if (event.getNativeLong(PropertyEventAtomOffset).toLong() != resourceManager.toLong()) continue
                _desktopScale.value = readDesktopScale()
            }
        }
    }

    // GNOME's own display configuration: the scale of the primary monitor, fractional scales included.
    private fun gnomeScale(): Float? {
        val state = DesktopDbus.call(
            busName = MutterDisplayConfig,
            objectPath = MutterDisplayConfigPath,
            interfaceName = MutterDisplayConfig,
            method = "GetCurrentState",
            arguments = "()",
        ) ?: return null
        val logicalMonitors = Regex("""\(-?\d+, -?\d+, ([0-9]+(?:\.[0-9]+)?), (?:uint32 )?\d+, (true|false), \[""")
            .findAll(state)
            .map { match -> match.groupValues[1].toFloatOrNull() to (match.groupValues[2] == "true") }
            .toList()
        val scale = logicalMonitors.firstOrNull { it.second }?.first ?: logicalMonitors.firstOrNull()?.first
        return scale?.takeIf { it > 0f }
    }

    // Waits on GNOME's MonitorsChanged signal, which Mutter sends whenever a display's scale or layout changes.
    private fun watchGnomeMonitors() {
        runCatching {
            val glib = DesktopDbus.glib
            val gio = DesktopDbus.gio
            val context = glib.g_main_context_new()
            glib.g_main_context_push_thread_default(context)
            val connection = gio.g_bus_get_sync(GBusTypeSession, null, null) ?: return
            val callback = object : SignalCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    signalName: String?,
                    parameters: Pointer?,
                    userData: Pointer?,
                ) {
                    _desktopScale.value = readDesktopScale()
                }
            }
            monitorsChanged = callback
            gio.g_dbus_connection_signal_subscribe(
                connection,
                MutterDisplayConfig,
                MutterDisplayConfig,
                "MonitorsChanged",
                MutterDisplayConfigPath,
                null,
                0,
                callback,
                null,
                null,
            )
            while (true) glib.g_main_context_iteration(context, true)
        }
    }

    private fun xftDpi(): Float? {
        val library = x11 ?: return null
        return runCatching {
            val display = library.XOpenDisplay(null) ?: return null
            try {
                val resources = library.XResourceManagerString(display) ?: return null
                Regex("""(?m)^Xft\.dpi:\s*([0-9]+(?:\.[0-9]+)?)""")
                    .find(resources)
                    ?.groupValues
                    ?.get(1)
                    ?.toFloatOrNull()
                    ?.takeIf { it > 0f }
            } finally {
                library.XCloseDisplay(display)
            }
        }.getOrNull()
    }

    private fun envScale(name: String): Float? =
        System.getenv(name)?.trim()?.toFloatOrNull()?.takeIf { it > 0f }

    private interface X11 : Library {
        fun XOpenDisplay(name: String?): Pointer?
        fun XCloseDisplay(display: Pointer): Int
        fun XResourceManagerString(display: Pointer): String?
        fun XDefaultRootWindow(display: Pointer): NativeLong
        fun XInternAtom(display: Pointer, name: String, onlyIfExists: Boolean): NativeLong
        fun XSelectInput(display: Pointer, window: NativeLong, eventMask: NativeLong): Int
        fun XNextEvent(display: Pointer, event: Pointer): Int
    }

    private const val MutterDisplayConfig = "org.gnome.Mutter.DisplayConfig"
    private const val MutterDisplayConfigPath = "/org/gnome/Mutter/DisplayConfig"
    private const val BaseDpi = 96f
    private const val MinScale = 0.5f
    private const val MaxScale = 4f
    private const val ScaleTolerance = 0.01f
    private const val MaxWindowFraction = 0.9f
    private const val PropertyNotify = 28
    private const val PropertyChangeMask = 1L shl 22
    private const val XEventSize = 192L
    private val PropertyEventAtomOffset: Long = run {
        val long = NativeLong.SIZE.toLong()
        val pointer = Native.POINTER_SIZE.toLong()
        fun align(offset: Long, size: Long) = (offset + size - 1) / size * size
        val serial = align(4, long)
        val sendEvent = serial + long
        val display = align(sendEvent + 4, pointer)
        val window = display + pointer
        window + long
    }
}

@Composable
internal fun ProvideDesktopDisplayScale(content: @Composable () -> Unit) {
    val option by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.displayScale
    }.collectAsState()
    val desktopScale by DesktopDisplayScale.desktopScale.collectAsState()
    val base = LocalDensity.current
    val scale = option.scale ?: desktopScale
    if (scale == null || abs(scale - base.density) < 0.01f) {
        content()
        return
    }
    CompositionLocalProvider(
        LocalDensity provides Density(density = scale, fontScale = base.fontScale),
        content = content,
    )
}
