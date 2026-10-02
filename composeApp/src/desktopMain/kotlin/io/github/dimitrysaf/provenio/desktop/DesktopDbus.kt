package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

internal const val GBusTypeSystem = 1
internal const val GBusTypeSession = 2
internal const val GDBusCallAllowInteractiveAuthorization = 2

internal object DesktopDbus {
    val glib: GLib by lazy { Native.load("libglib-2.0.so.0", GLib::class.java) }
    val gio: Gio by lazy { Native.load("libgio-2.0.so.0", Gio::class.java) }
    val gobject: GObject by lazy { Native.load("libgobject-2.0.so.0", GObject::class.java) }

    fun call(
        busName: String,
        objectPath: String,
        interfaceName: String,
        method: String,
        arguments: String,
        timeoutMilliseconds: Int = 2_000,
        busType: Int = GBusTypeSession,
        flags: Int = 0,
        sharedConnection: Pointer? = null,
    ): String? = runCatching {
        val connection = sharedConnection ?: gio.g_bus_get_sync(busType, null, null) ?: return null
        try {
            val parameters = glib.g_variant_parse(null, arguments, null, null, null) ?: return null
            val reply = gio.g_dbus_connection_call_sync(
                connection,
                busName,
                objectPath,
                interfaceName,
                method,
                parameters,
                null,
                flags,
                timeoutMilliseconds,
                null,
                null,
            ) ?: return null
            try {
                printVariant(reply)
            } finally {
                glib.g_variant_unref(reply)
            }
        } finally {
            if (sharedConnection == null) gobject.g_object_unref(connection)
        }
    }.getOrNull()

    fun printVariant(variant: Pointer): String {
        val text = glib.g_variant_print(variant, false)
        return try {
            text.getString(0)
        } finally {
            glib.g_free(text)
        }
    }
}

internal fun String.toGVariantString(): String = "'" + replace("\\", "\\\\").replace("'", "\\'") + "'"

@Suppress("FunctionName")
internal interface GLib : Library {
    fun g_main_context_new(): Pointer
    fun g_main_context_push_thread_default(context: Pointer)
    fun g_main_context_pop_thread_default(context: Pointer)
    fun g_main_context_unref(context: Pointer)
    fun g_main_context_iteration(context: Pointer, mayBlock: Boolean): Boolean
    fun g_variant_parse(type: Pointer?, text: String, limit: Pointer?, endptr: Pointer?, error: PointerByReference?): Pointer?
    fun g_variant_print(value: Pointer, typeAnnotate: Boolean): Pointer
    fun g_variant_unref(value: Pointer)
    fun g_variant_ref(value: Pointer): Pointer
    fun g_free(memory: Pointer)
}

@Suppress("FunctionName")
internal interface Gio : Library {
    fun g_bus_get_sync(busType: Int, cancellable: Pointer?, error: PointerByReference?): Pointer?
    fun g_dbus_connection_get_unique_name(connection: Pointer): String?
    fun g_dbus_connection_signal_subscribe(
        connection: Pointer,
        sender: String?,
        interfaceName: String?,
        member: String?,
        objectPath: String?,
        arg0: String?,
        flags: Int,
        callback: SignalCallback,
        userData: Pointer?,
        userDataFreeFunction: Pointer?,
    ): Int
    fun g_dbus_connection_signal_unsubscribe(connection: Pointer, subscriptionId: Int)
    fun g_dbus_node_info_new_for_xml(xml: String, error: PointerByReference?): Pointer?
    fun g_dbus_node_info_lookup_interface(info: Pointer, name: String): Pointer?
    fun g_dbus_connection_register_object(
        connection: Pointer,
        objectPath: String,
        interfaceInfo: Pointer,
        vtable: GDBusInterfaceVTable,
        userData: Pointer?,
        userDataFreeFunction: Pointer?,
        error: PointerByReference?,
    ): Int
    fun g_dbus_method_invocation_return_value(invocation: Pointer, parameters: Pointer?)
    fun g_dbus_connection_call_sync(
        connection: Pointer,
        busName: String,
        objectPath: String,
        interfaceName: String,
        methodName: String,
        parameters: Pointer,
        replyType: Pointer?,
        flags: Int,
        timeoutMilliseconds: Int,
        cancellable: Pointer?,
        error: PointerByReference?,
    ): Pointer?
}

@Suppress("FunctionName")
internal interface GObject : Library {
    fun g_object_unref(instance: Pointer)
}

internal interface SignalCallback : Callback {
    fun invoke(
        connection: Pointer?,
        senderName: String?,
        objectPath: String?,
        interfaceName: String?,
        signalName: String?,
        parameters: Pointer?,
        userData: Pointer?,
    )
}

internal interface MethodCallCallback : Callback {
    fun invoke(
        connection: Pointer?,
        senderName: String?,
        objectPath: String?,
        interfaceName: String?,
        methodName: String?,
        parameters: Pointer?,
        invocation: Pointer?,
        userData: Pointer?,
    )
}

internal interface GetPropertyCallback : Callback {
    fun invoke(
        connection: Pointer?,
        senderName: String?,
        objectPath: String?,
        interfaceName: String?,
        propertyName: String?,
        error: Pointer?,
        userData: Pointer?,
    ): Pointer?
}

@Suppress("PropertyName")
@Structure.FieldOrder("method_call", "get_property", "set_property", "padding")
internal class GDBusInterfaceVTable : Structure() {
    @JvmField var method_call: MethodCallCallback? = null
    @JvmField var get_property: GetPropertyCallback? = null
    @JvmField var set_property: Pointer? = null
    @JvmField var padding: Array<Pointer?> = arrayOfNulls(8)
}
