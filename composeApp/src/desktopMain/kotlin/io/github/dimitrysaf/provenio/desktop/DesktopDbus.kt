package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

internal const val GBusTypeSession = 2

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
    ): String? = runCatching {
        val connection = gio.g_bus_get_sync(GBusTypeSession, null, null) ?: return null
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
                0,
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
            gobject.g_object_unref(connection)
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
