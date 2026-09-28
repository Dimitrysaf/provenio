package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Pointer
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

private const val ItemPath = "/StatusNotifierItem"
private const val MenuPath = "/MenuBar"
private const val ItemInterface = "org.kde.StatusNotifierItem"
private const val MenuInterface = "com.canonical.dbusmenu"
private const val WatcherName = "org.kde.StatusNotifierWatcher"
private const val ShowItemId = 1
private const val QuitItemId = 2
private const val IconSize = 64

private val IntrospectionXml = """
<node>
  <interface name="$ItemInterface">
    <method name="Activate"><arg type="i" direction="in"/><arg type="i" direction="in"/></method>
    <method name="SecondaryActivate"><arg type="i" direction="in"/><arg type="i" direction="in"/></method>
    <method name="ContextMenu"><arg type="i" direction="in"/><arg type="i" direction="in"/></method>
    <method name="Scroll"><arg type="i" direction="in"/><arg type="s" direction="in"/></method>
    <property name="Category" type="s" access="read"/>
    <property name="Id" type="s" access="read"/>
    <property name="Title" type="s" access="read"/>
    <property name="Status" type="s" access="read"/>
    <property name="IconName" type="s" access="read"/>
    <property name="IconThemePath" type="s" access="read"/>
    <property name="IconPixmap" type="a(iiay)" access="read"/>
    <property name="Menu" type="o" access="read"/>
    <property name="ItemIsMenu" type="b" access="read"/>
  </interface>
  <interface name="$MenuInterface">
    <method name="GetLayout">
      <arg type="i" direction="in"/><arg type="i" direction="in"/><arg type="as" direction="in"/>
      <arg type="u" direction="out"/><arg type="(ia{sv}av)" direction="out"/>
    </method>
    <method name="GetGroupProperties">
      <arg type="ai" direction="in"/><arg type="as" direction="in"/>
      <arg type="a(ia{sv})" direction="out"/>
    </method>
    <method name="GetProperty">
      <arg type="i" direction="in"/><arg type="s" direction="in"/><arg type="v" direction="out"/>
    </method>
    <method name="Event">
      <arg type="i" direction="in"/><arg type="s" direction="in"/><arg type="v" direction="in"/><arg type="u" direction="in"/>
    </method>
    <method name="EventGroup">
      <arg type="a(isvu)" direction="in"/><arg type="ai" direction="out"/>
    </method>
    <method name="AboutToShow"><arg type="i" direction="in"/><arg type="b" direction="out"/></method>
    <method name="AboutToShowGroup">
      <arg type="ai" direction="in"/><arg type="ai" direction="out"/><arg type="ai" direction="out"/>
    </method>
    <signal name="LayoutUpdated"><arg type="u"/><arg type="i"/></signal>
    <signal name="ItemsPropertiesUpdated"><arg type="a(ia{sv})"/><arg type="a(ia{s})"/></signal>
    <property name="Version" type="u" access="read"/>
    <property name="TextDirection" type="s" access="read"/>
    <property name="Status" type="s" access="read"/>
    <property name="IconThemePath" type="as" access="read"/>
  </interface>
</node>
""".trimIndent()

private val MenuEventPattern = Regex("""\((-?\d+), '(\w+)'""")

internal object DesktopStatusNotifier {
    private val glib: GLib get() = DesktopDbus.glib
    private val gio: Gio get() = DesktopDbus.gio

    private var itemVTable: GDBusInterfaceVTable? = null
    private var menuVTable: GDBusInterfaceVTable? = null
    private var iconPixmap: Pointer? = null

    fun start(
        appId: String,
        title: String,
        icon: Image?,
        showLabel: String,
        quitLabel: String,
        onShow: () -> Unit,
        onQuit: () -> Unit,
    ): Boolean {
        if (!System.getProperty("os.name").orEmpty().lowercase().contains("linux")) return false
        val watcherPresent = DesktopDbus.call(
            busName = "org.freedesktop.DBus",
            objectPath = "/org/freedesktop/DBus",
            interfaceName = "org.freedesktop.DBus",
            method = "NameHasOwner",
            arguments = "(${WatcherName.toGVariantString()},)",
        )?.contains("true") == true
        if (!watcherPresent) return false

        val exported = CountDownLatch(1)
        val success = AtomicBoolean(false)
        Thread({
            runLoop(exported, success) { export(appId, title, icon, showLabel, quitLabel, onShow, onQuit) }
        }, "status-notifier").apply { isDaemon = true }.start()
        exported.await(3, TimeUnit.SECONDS)
        if (!success.get()) return false
        Thread({
            DesktopDbus.call(
                busName = WatcherName,
                objectPath = "/StatusNotifierWatcher",
                interfaceName = WatcherName,
                method = "RegisterStatusNotifierItem",
                arguments = "(${ItemPath.toGVariantString()},)",
                timeoutMilliseconds = 10_000,
            )
        }, "status-notifier-register").apply { isDaemon = true }.start()
        return true
    }

    private fun runLoop(exported: CountDownLatch, success: AtomicBoolean, export: () -> Boolean) {
        val context = runCatching { glib.g_main_context_new() }.getOrNull()
        if (context == null) {
            exported.countDown()
            return
        }
        glib.g_main_context_push_thread_default(context)
        success.set(runCatching { export() }.getOrDefault(false))
        exported.countDown()
        if (!success.get()) return
        while (true) glib.g_main_context_iteration(context, true)
    }

    private fun export(
        appId: String,
        title: String,
        icon: Image?,
        showLabel: String,
        quitLabel: String,
        onShow: () -> Unit,
        onQuit: () -> Unit,
    ): Boolean {
        val connection = gio.g_bus_get_sync(GBusTypeSession, null, null) ?: return false
        val nodeInfo = gio.g_dbus_node_info_new_for_xml(IntrospectionXml, null) ?: return false
        val itemInfo = gio.g_dbus_node_info_lookup_interface(nodeInfo, ItemInterface) ?: return false
        val menuInfo = gio.g_dbus_node_info_lookup_interface(nodeInfo, MenuInterface) ?: return false
        iconPixmap = icon?.let { parse(pixmapText(it)) }

        val showText = showLabel.toGVariantString()
        val quitText = quitLabel.toGVariantString()
        val menuItems = "<($ShowItemId, {'label': <$showText>}, @av [])>, <($QuitItemId, {'label': <$quitText>}, @av [])>"
        val layout = "(uint32 1, (0, {'children-display': <'submenu'>}, [$menuItems]))"
        val groupProperties = "([(0, {'children-display': <'submenu'>}), " +
            "($ShowItemId, {'label': <$showText>}), ($QuitItemId, {'label': <$quitText>})],)"

        fun dispatch(action: () -> Unit) = SwingUtilities.invokeLater { action() }
        fun onMenuEvent(id: Int, event: String) {
            if (event != "clicked") return
            when (id) {
                ShowItemId -> dispatch(onShow)
                QuitItemId -> dispatch(onQuit)
            }
        }

        val item = GDBusInterfaceVTable().apply {
            method_call = object : MethodCallCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    methodName: String?,
                    parameters: Pointer?,
                    invocation: Pointer?,
                    userData: Pointer?,
                ) {
                    if (methodName == "Activate") dispatch(onShow)
                    invocation?.let { reply(it, null) }
                }
            }
            get_property = object : GetPropertyCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    propertyName: String?,
                    error: Pointer?,
                    userData: Pointer?,
                ): Pointer? = when (propertyName) {
                    "Category" -> parse("'ApplicationStatus'")
                    "Id" -> parse(appId.toGVariantString())
                    "Title" -> parse(title.toGVariantString())
                    "Status" -> parse("'Active'")
                    "IconName" -> parse(appId.toGVariantString())
                    "IconThemePath" -> parse("''")
                    "IconPixmap" -> iconPixmap?.let(glib::g_variant_ref) ?: parse("@a(iiay) []")
                    "Menu" -> parse("objectpath '$MenuPath'")
                    "ItemIsMenu" -> parse("false")
                    else -> null
                }
            }
        }
        val menu = GDBusInterfaceVTable().apply {
            method_call = object : MethodCallCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    methodName: String?,
                    parameters: Pointer?,
                    invocation: Pointer?,
                    userData: Pointer?,
                ) {
                    val arguments = parameters?.let(DesktopDbus::printVariant).orEmpty()
                    val result = when (methodName) {
                        "GetLayout" -> layout
                        "GetGroupProperties" -> groupProperties
                        "GetProperty" -> "(<''>,)"
                        "Event" -> {
                            MenuEventPattern.find(arguments)?.let { match ->
                                onMenuEvent(match.groupValues[1].toInt(), match.groupValues[2])
                            }
                            null
                        }
                        "EventGroup" -> {
                            MenuEventPattern.findAll(arguments).forEach { match ->
                                onMenuEvent(match.groupValues[1].toInt(), match.groupValues[2])
                            }
                            "(@ai [],)"
                        }
                        "AboutToShow" -> "(false,)"
                        "AboutToShowGroup" -> "(@ai [], @ai [])"
                        else -> null
                    }
                    invocation?.let { reply(it, result) }
                }
            }
            get_property = object : GetPropertyCallback {
                override fun invoke(
                    connection: Pointer?,
                    senderName: String?,
                    objectPath: String?,
                    interfaceName: String?,
                    propertyName: String?,
                    error: Pointer?,
                    userData: Pointer?,
                ): Pointer? = when (propertyName) {
                    "Version" -> parse("uint32 3")
                    "TextDirection" -> parse("'ltr'")
                    "Status" -> parse("'normal'")
                    "IconThemePath" -> parse("@as []")
                    else -> null
                }
            }
        }
        itemVTable = item
        menuVTable = menu

        if (gio.g_dbus_connection_register_object(connection, ItemPath, itemInfo, item, null, null, null) == 0) return false
        return gio.g_dbus_connection_register_object(connection, MenuPath, menuInfo, menu, null, null, null) != 0
    }

    private fun parse(text: String): Pointer? = glib.g_variant_parse(null, text, null, null, null)

    private fun reply(invocation: Pointer, text: String?) {
        val value = text?.let(::parse)
        gio.g_dbus_method_invocation_return_value(invocation, value)
        value?.let(glib::g_variant_unref)
    }

    private fun pixmapText(source: Image): String {
        val image = BufferedImage(IconSize, IconSize, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().apply {
            drawImage(source, 0, 0, IconSize, IconSize, null)
            dispose()
        }
        val bytes = buildString {
            for (y in 0 until IconSize) {
                for (x in 0 until IconSize) {
                    val argb = image.getRGB(x, y)
                    for (shift in intArrayOf(24, 16, 8, 0)) {
                        if (isNotEmpty()) append(", ")
                        append("0x").append(((argb ushr shift) and 0xFF).toString(16))
                    }
                }
            }
        }
        return "[($IconSize, $IconSize, [byte $bytes])]"
    }
}
