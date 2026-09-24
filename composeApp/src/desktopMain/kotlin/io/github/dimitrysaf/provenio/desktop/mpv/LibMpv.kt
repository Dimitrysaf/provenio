package io.github.dimitrysaf.provenio.desktop.mpv

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

/** The parts of libmpv's client and render APIs the desktop player uses (mpv/client.h, mpv/render.h). */
@Suppress("FunctionName")
internal interface LibMpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(handle: Pointer): Int
    fun mpv_terminate_destroy(handle: Pointer)
    fun mpv_error_string(error: Int): String
    fun mpv_free(data: Pointer)
    fun mpv_set_option_string(handle: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(handle: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(handle: Pointer, name: String): Pointer?
    fun mpv_command(handle: Pointer, args: Array<String?>): Int
    fun mpv_wait_event(handle: Pointer, timeout: Double): Pointer
    fun mpv_wakeup(handle: Pointer)

    fun mpv_render_context_create(result: PointerByReference, handle: Pointer, params: Pointer): Int
    fun mpv_render_context_set_update_callback(context: Pointer, callback: UpdateCallback?, callbackContext: Pointer?)
    fun mpv_render_context_update(context: Pointer): Long
    fun mpv_render_context_render(context: Pointer, params: Pointer): Int
    fun mpv_render_context_free(context: Pointer)

    fun interface UpdateCallback : Callback {
        fun invoke(context: Pointer?)
    }

    companion object {
        const val EVENT_NONE = 0
        const val EVENT_SHUTDOWN = 1
        const val EVENT_END_FILE = 7
        const val EVENT_FILE_LOADED = 8

        const val END_FILE_REASON_ERROR = 4

        const val RENDER_PARAM_INVALID = 0
        const val RENDER_PARAM_API_TYPE = 1
        const val RENDER_PARAM_SW_SIZE = 17
        const val RENDER_PARAM_SW_FORMAT = 18
        const val RENDER_PARAM_SW_STRIDE = 19
        const val RENDER_PARAM_SW_POINTER = 20

        const val RENDER_UPDATE_FRAME = 1L

        /** Null when libmpv is not installed; the player then reports an error instead. */
        val instance: LibMpv? by lazy {
            // mpv refuses to start unless numbers are formatted the C way.
            runCatching { CLibrary.INSTANCE.setlocale(LC_NUMERIC, "C") }
            listOf("mpv", "libmpv.so.2", "libmpv.so.1").firstNotNullOfOrNull { name ->
                runCatching {
                    Native.load(name, LibMpv::class.java, mapOf(Library.OPTION_STRING_ENCODING to "UTF-8"))
                }.getOrNull()
            }
        }

        private const val LC_NUMERIC = 1
    }
}

internal interface CLibrary : Library {
    fun setlocale(category: Int, locale: String): Pointer?

    companion object {
        val INSTANCE: CLibrary = Native.load("c", CLibrary::class.java)
    }
}

/** mpv_event */
@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
internal class MpvEvent(pointer: Pointer) : Structure(pointer) {
    @JvmField var event_id: Int = 0
    @JvmField var error: Int = 0
    @JvmField var reply_userdata: Long = 0
    @JvmField var data: Pointer? = null

    init {
        read()
    }
}

/** mpv_event_end_file */
@Structure.FieldOrder("reason", "error")
internal class MpvEventEndFile(pointer: Pointer) : Structure(pointer) {
    @JvmField var reason: Int = 0
    @JvmField var error: Int = 0

    init {
        read()
    }
}

/** mpv_render_param */
@Structure.FieldOrder("type", "data")
internal open class MpvRenderParam : Structure() {
    @JvmField var type: Int = 0
    @JvmField var data: Pointer? = null
}
