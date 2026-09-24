package io.github.dimitrysaf.provenio.desktop.mpv

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import io.github.dimitrysaf.provenio.core.playback.AudioTrack
import io.github.dimitrysaf.provenio.core.playback.PlayerPlaybackSnapshot
import io.github.dimitrysaf.provenio.core.playback.PlayerResizeMode
import io.github.dimitrysaf.provenio.core.playback.SubtitleTrack
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

/**
 * One libmpv instance with software rendering: mpv draws each frame straight into a Skia bitmap's
 * memory and the newest frame is published for Compose to draw. Keeping the video inside Compose
 * lets the player's controls sit on top of it, as they do on Android.
 */
internal class MpvPlayer private constructor(
    private val mpv: LibMpv,
    private val handle: Pointer,
) {
    private val renderContext: Pointer
    private val released = AtomicBoolean(false)

    // Every call into mpv holds the read lock; destroying the handle takes the write lock, so no
    // thread can be inside mpv when it goes away.
    private val handleLock = ReentrantReadWriteLock()
    private val renderStopped = CountDownLatch(1)
    private val frameRequested = Object()
    private var frameDirty = false
    private var targetSize = IntSize(1280, 720)

    // JNA must keep the callback reachable for as long as mpv may call it.
    private val updateCallback = LibMpv.UpdateCallback { requestFrame() }

    private val mutableFrame = MutableStateFlow<ImageBitmap?>(null)
    val frame: StateFlow<ImageBitmap?> = mutableFrame.asStateFlow()

    /** Called on mpv's event thread. */
    var onFileLoaded: () -> Unit = {}
    var onEndFileError: (String) -> Unit = {}

    init {
        val apiType = Memory(3).apply { setString(0, "sw", "US-ASCII") }
        val params = renderParams(LibMpv.RENDER_PARAM_API_TYPE to apiType)
        val result = PointerByReference()
        val status = mpv.mpv_render_context_create(result, handle, params)
        check(status >= 0) { "mpv render context: ${mpv.mpv_error_string(status)}" }
        renderContext = result.value
        mpv.mpv_render_context_set_update_callback(renderContext, updateCallback, null)
        Thread(::eventLoop, "mpv-events").apply { isDaemon = true }.start()
        Thread(::renderLoop, "mpv-render").apply { isDaemon = true }.start()
    }

    fun load(
        url: String,
        audioUrl: String?,
        headers: Map<String, String>,
        subtitles: List<Pair<String, String?>>,
        startPositionMs: Long?,
    ) {
        command("change-list", "http-header-fields", "clr", "")
        headers.forEach { (key, value) ->
            if (key.equals("User-Agent", ignoreCase = true)) {
                setProperty("user-agent", value)
            } else {
                command("change-list", "http-header-fields", "append", "$key: $value")
            }
        }
        pendingSubtitles = subtitles
        val options = buildList {
            startPositionMs?.takeIf { it > 0 }?.let { add("start=${it / 1000.0}") }
            audioUrl?.takeIf { it.isNotBlank() }?.let { add("audio-files-append=${lengthQuoted(it)}") }
        }
        command("loadfile", url, "replace", "-1", options.joinToString(","))
    }

    @Volatile
    private var pendingSubtitles: List<Pair<String, String?>> = emptyList()

    fun setPaused(paused: Boolean) = setProperty("pause", if (paused) "yes" else "no")

    fun seekTo(positionMs: Long) = command("seek", (positionMs / 1000.0).toString(), "absolute+exact")

    fun seekBy(offsetMs: Long) = command("seek", (offsetMs / 1000.0).toString(), "relative+exact")

    fun setSpeed(speed: Float) = setProperty("speed", speed.toString())

    fun setMuted(muted: Boolean) = setProperty("mute", if (muted) "yes" else "no")

    fun setSubtitleDelayMs(delayMs: Int) = setProperty("sub-delay", (delayMs / 1000.0).toString())

    fun setResizeMode(mode: PlayerResizeMode) {
        when (mode) {
            PlayerResizeMode.Fit -> {
                setProperty("keepaspect", "yes")
                setProperty("panscan", "0")
            }
            // Fill stretches to the surface; Zoom crops, as ExoPlayer's modes of those names do.
            PlayerResizeMode.Fill -> {
                setProperty("keepaspect", "no")
                setProperty("panscan", "0")
            }
            PlayerResizeMode.Zoom -> {
                setProperty("keepaspect", "yes")
                setProperty("panscan", "1.0")
            }
        }
    }

    fun setPreferredAudioLanguages(languages: List<String>) {
        if (languages.isNotEmpty()) setProperty("alang", languages.joinToString(","))
    }

    fun setPreferredSubtitleLanguages(languages: List<String>) {
        if (languages.isNotEmpty()) setProperty("slang", languages.joinToString(","))
    }

    fun audioTracks(): List<AudioTrack> = tracks("audio").mapIndexed { index, track ->
        AudioTrack(
            index = index,
            id = track.id,
            label = track.label,
            language = track.language,
            isSelected = track.selected,
        )
    }

    fun subtitleTracks(): List<SubtitleTrack> = tracks("sub").mapIndexed { index, track ->
        SubtitleTrack(
            index = index,
            id = track.id,
            label = track.label,
            language = track.language,
            isSelected = track.selected,
            isForced = track.forced,
        )
    }

    fun selectAudioTrack(index: Int) {
        tracks("audio").getOrNull(index)?.let { setProperty("aid", it.id) }
    }

    fun selectSubtitleTrack(index: Int) {
        val track = tracks("sub").getOrNull(index)
        setProperty("sid", track?.id ?: "no")
    }

    fun addSubtitle(url: String, title: String? = null) {
        command("sub-add", url, "select", title ?: "", "")
    }

    fun removeExternalSubtitles() {
        tracks("sub").filter { it.external }.forEach { command("sub-remove", it.id) }
    }

    fun reload() {
        val position = doubleProperty("time-pos")
        val path = stringProperty("path") ?: return
        command("loadfile", path, "replace", "-1", position?.let { "start=$it" } ?: "")
    }

    fun snapshot(): PlayerPlaybackSnapshot {
        val position = doubleProperty("time-pos") ?: 0.0
        val cacheEnd = doubleProperty("demuxer-cache-time") ?: position
        return PlayerPlaybackSnapshot(
            isLoading = flagProperty("paused-for-cache") == true ||
                flagProperty("seeking") == true ||
                stringProperty("path") != null && doubleProperty("duration") == null,
            isPlaying = flagProperty("pause") == false && flagProperty("core-idle") == false,
            isEnded = flagProperty("eof-reached") == true,
            durationMs = ((doubleProperty("duration") ?: 0.0) * 1000).toLong(),
            positionMs = (position * 1000).toLong(),
            bufferedPositionMs = (maxOf(cacheEnd, position) * 1000).toLong(),
            playbackSpeed = doubleProperty("speed")?.toFloat() ?: 1f,
            videoWidth = stringProperty("video-params/w")?.toIntOrNull() ?: 0,
            videoHeight = stringProperty("video-params/h")?.toIntOrNull() ?: 0,
        )
    }

    /** The surface's size in pixels; frames are rendered no larger than it or the video. */
    fun setTargetSize(size: IntSize) {
        if (size.width <= 0 || size.height <= 0) return
        synchronized(frameRequested) {
            targetSize = size
            frameDirty = true
            frameRequested.notifyAll()
        }
    }

    /** Stops both threads; the event thread destroys mpv once the render thread has let go. */
    fun release() {
        if (!released.compareAndSet(false, true)) return
        synchronized(frameRequested) { frameRequested.notifyAll() }
        handleLock.read { mpv.mpv_wakeup(handle) }
    }

    private fun requestFrame() {
        synchronized(frameRequested) {
            frameDirty = true
            frameRequested.notifyAll()
        }
    }

    private fun renderLoop() {
        var bitmap: Bitmap? = null
        val sizeParam = Memory(8)
        val strideParam = Memory(Native.SIZE_T_SIZE.toLong())
        val formatParam = Memory(5).apply { setString(0, "bgr0", "US-ASCII") }
        while (!released.get()) {
            val size = synchronized(frameRequested) {
                while (!frameDirty && !released.get()) frameRequested.wait()
                frameDirty = false
                targetSize
            }
            if (released.get()) break
            val flags = mpv.mpv_render_context_update(renderContext)
            val videoWidth = stringProperty("video-params/w")?.toIntOrNull() ?: 0
            val videoHeight = stringProperty("video-params/h")?.toIntOrNull() ?: 0
            if (videoWidth <= 0 || videoHeight <= 0) continue
            val (width, height) = renderWidth(size, videoWidth, videoHeight)
            val resized = bitmap == null || bitmap.width != width || bitmap.height != height
            if (flags and LibMpv.RENDER_UPDATE_FRAME == 0L && !resized) continue
            if (resized) {
                bitmap?.close()
                bitmap = Bitmap().apply {
                    allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.OPAQUE))
                }
            }
            val pixels = bitmap.peekPixels() ?: continue
            sizeParam.setInt(0, width)
            sizeParam.setInt(4, height)
            if (Native.SIZE_T_SIZE == 8) {
                strideParam.setLong(0, pixels.rowBytes.toLong())
            } else {
                strideParam.setInt(0, pixels.rowBytes)
            }
            val params = renderParams(
                LibMpv.RENDER_PARAM_SW_SIZE to sizeParam,
                LibMpv.RENDER_PARAM_SW_FORMAT to formatParam,
                LibMpv.RENDER_PARAM_SW_STRIDE to strideParam,
                LibMpv.RENDER_PARAM_SW_POINTER to Pointer(pixels.addr),
            )
            if (mpv.mpv_render_context_render(renderContext, params) < 0) continue
            bitmap.notifyPixelsChanged()
            // makeFromBitmap copies a mutable bitmap, so the published frame never changes
            // under Compose while the next one renders.
            mutableFrame.value = Image.makeFromBitmap(bitmap).toComposeImageBitmap()
        }
        bitmap?.close()
        mpv.mpv_render_context_set_update_callback(renderContext, null, null)
        mpv.mpv_render_context_free(renderContext)
        renderStopped.countDown()
    }

    /**
     * Rendering at the window's full size costs CPU for nothing when the video is smaller;
     * Compose scales the frame up on the GPU instead.
     */
    private fun renderWidth(target: IntSize, videoWidth: Int, videoHeight: Int): Pair<Int, Int> {
        val scale = minOf(1.0, maxOf(videoWidth.toDouble() / target.width, videoHeight.toDouble() / target.height))
        return (target.width * scale).toInt().coerceAtLeast(1) to (target.height * scale).toInt().coerceAtLeast(1)
    }

    private fun eventLoop() {
        while (!released.get()) {
            val event = MpvEvent(mpv.mpv_wait_event(handle, 0.5))
            when (event.event_id) {
                LibMpv.EVENT_SHUTDOWN -> break
                LibMpv.EVENT_FILE_LOADED -> {
                    pendingSubtitles.forEach { (url, title) -> command("sub-add", url, "auto", title ?: "", "") }
                    requestFrame()
                    onFileLoaded()
                }
                LibMpv.EVENT_END_FILE -> {
                    val endFile = event.data?.let(::MpvEventEndFile)
                    if (endFile?.reason == LibMpv.END_FILE_REASON_ERROR) {
                        onEndFileError(mpv.mpv_error_string(endFile.error))
                    }
                }
            }
        }
        released.set(true)
        synchronized(frameRequested) { frameRequested.notifyAll() }
        renderStopped.await()
        handleLock.write { mpv.mpv_terminate_destroy(handle) }
    }

    private data class Track(
        val id: String,
        val label: String,
        val language: String?,
        val selected: Boolean,
        val forced: Boolean,
        val external: Boolean,
    )

    private fun tracks(type: String): List<Track> {
        val count = stringProperty("track-list/count")?.toIntOrNull() ?: return emptyList()
        return (0 until count).mapNotNull { index ->
            val prefix = "track-list/$index"
            if (stringProperty("$prefix/type") != type) return@mapNotNull null
            val id = stringProperty("$prefix/id") ?: return@mapNotNull null
            val language = stringProperty("$prefix/lang")
            val title = stringProperty("$prefix/title")
            Track(
                id = id,
                label = listOfNotNull(title, language?.uppercase()).joinToString(" · ").ifBlank { "#$id" },
                language = language,
                selected = stringProperty("$prefix/selected") == "yes",
                forced = stringProperty("$prefix/forced") == "yes",
                external = stringProperty("$prefix/external") == "yes",
            )
        }
    }

    private fun command(vararg args: String) = handleLock.read {
        if (!released.get()) mpv.mpv_command(handle, arrayOf(*args, null))
    }

    private fun setProperty(name: String, value: String) = handleLock.read {
        if (!released.get()) mpv.mpv_set_property_string(handle, name, value)
    }

    private fun stringProperty(name: String): String? = handleLock.read {
        if (released.get()) return@read null
        val pointer = mpv.mpv_get_property_string(handle, name) ?: return@read null
        try {
            pointer.getString(0, "UTF-8")
        } finally {
            mpv.mpv_free(pointer)
        }
    }

    /** mpv's %length% quoting, so a value may contain commas and equals signs. */
    private fun lengthQuoted(value: String): String = "%${value.encodeToByteArray().size}%$value"

    private fun doubleProperty(name: String): Double? = stringProperty(name)?.toDoubleOrNull()

    private fun flagProperty(name: String): Boolean? = when (stringProperty(name)) {
        "yes" -> true
        "no" -> false
        else -> null
    }

    private fun renderParams(vararg entries: Pair<Int, Pointer>): Pointer {
        val array = MpvRenderParam().toArray(entries.size + 1)
        entries.forEachIndexed { index, (type, data) ->
            (array[index] as MpvRenderParam).apply {
                this.type = type
                this.data = data
                write()
            }
        }
        (array.last() as MpvRenderParam).apply {
            type = LibMpv.RENDER_PARAM_INVALID
            data = null
            write()
        }
        return array[0].pointer
    }

    companion object {
        /** Null when libmpv cannot be loaded or started. */
        fun create(): MpvPlayer? {
            val mpv = LibMpv.instance ?: return null
            val handle = mpv.mpv_create() ?: return null
            val options = listOf(
                "vo" to "libmpv",
                // Software rendering needs frames in system memory, so decoded frames are copied back.
                "hwdec" to "auto-copy-safe",
                "keep-open" to "yes",
                "idle" to "yes",
                "cache" to "yes",
                "demuxer-max-bytes" to "150MiB",
                "demuxer-max-back-bytes" to "75MiB",
                "terminal" to "no",
                "input-default-bindings" to "no",
                "ytdl" to "no",
                "sub-auto" to "no",
                "audio-client-name" to "Provenio",
            )
            options.forEach { (name, value) -> mpv.mpv_set_option_string(handle, name, value) }
            if (mpv.mpv_initialize(handle) < 0) {
                mpv.mpv_terminate_destroy(handle)
                return null
            }
            return runCatching { MpvPlayer(mpv, handle) }
                .onFailure { mpv.mpv_terminate_destroy(handle) }
                .getOrNull()
        }
    }
}
