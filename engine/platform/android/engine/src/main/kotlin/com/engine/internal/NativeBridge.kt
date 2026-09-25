package com.engine.internal

internal object NativeBridge {
    init {
        // A Compose Desktop app bundles its native libraries in its resources directory; Android
        // and plain JVM hosts find the library on the usual search path.
        val bundled = System.getProperty("compose.application.resources.dir")
            ?.let { java.io.File(it, System.mapLibraryName("engine")) }
        if (bundled != null && bundled.isFile) {
            System.load(bundled.absolutePath)
        } else {
            System.loadLibrary("engine")
        }
    }

    external fun nativeCreate(
        dataDirectory: String,
        cacheDirectory: String,
        memoryCacheCapacityBytes: Long,
        diskCacheCapacityBytes: Long,
        torrentProfile: Int,
        listenPort: Int,
        uploadMode: Int,
        uploadLimitBytesPerSecond: Long,
        streamInactivityTimeoutMilliseconds: Int,
        warmTorrentTimeoutMilliseconds: Int,
        tlsCaBundlePath: String,
    ): LongArray

    external fun nativeDestroy(handle: Long)
    external fun nativeAddMagnet(handle: Long, magnetUri: String): LongArray
    external fun nativeAddTorrentData(handle: Long, torrentData: ByteArray): LongArray
    external fun nativePollEvent(handle: Long): NativeEventPayload?
    external fun nativeGetFiles(handle: Long, torrentId: String): NativeFilesPayload
    external fun nativePrepareStream(
        handle: Long,
        torrentId: String,
        fileIndex: Int,
        filenameHint: String?,
    ): LongArray
    external fun nativeStopStream(handle: Long, streamId: String): LongArray
    external fun nativeRemoveTorrent(handle: Long, torrentId: String): LongArray
    external fun nativeGetStats(handle: Long): LongArray
    external fun nativeGetStreamStats(handle: Long, streamId: String): LongArray
    external fun nativeReclaimDiskCache(handle: Long, targetBytes: Long): LongArray
    external fun nativeSetUploadMode(handle: Long, uploadMode: Int, uploadLimitBytesPerSecond: Long): Int
    external fun nativeSetStreamDuration(handle: Long, streamId: String, durationMilliseconds: Long): Int
    external fun nativeGetTorrentDetails(handle: Long, torrentId: String): NativeTorrentDetailsPayload?
    external fun nativeStatusMessage(status: Int): String
    external fun nativeEngineVersion(): String
    external fun nativeBackendVersion(): String
}

internal class NativeEventPayload(
    @JvmField val type: Int,
    @JvmField val sequence: Long,
    @JvmField val requestId: Long,
    @JvmField val droppedEvents: Long,
    @JvmField val torrentId: String,
    @JvmField val message: String,
    @JvmField val fileIndex: Int,
    @JvmField val fileSize: Long,
    @JvmField val streamId: String,
    @JvmField val streamUrl: String,
)

internal class NativeFilePayload(
    @JvmField val index: Int,
    @JvmField val offset: Long,
    @JvmField val size: Long,
    @JvmField val pathTruncated: Boolean,
    @JvmField val path: String,
)

internal class NativeFilesPayload(
    @JvmField val status: Int,
    @JvmField val files: Array<NativeFilePayload>,
)

internal class NativePeerPayload(
    @JvmField val values: LongArray,
    @JvmField val address: String,
    @JvmField val client: String,
)

internal class NativeTrackerPayload(
    @JvmField val values: LongArray,
    @JvmField val url: String,
    @JvmField val message: String,
)

internal class NativeTorrentDetailsPayload(
    @JvmField val status: Int,
    @JvmField val values: LongArray,
    @JvmField val name: String,
    @JvmField val currentTracker: String,
    @JvmField val peers: Array<NativePeerPayload>,
    @JvmField val trackers: Array<NativeTrackerPayload>,
    @JvmField val pieceStates: ByteArray,
    @JvmField val pieceAvailability: ByteArray,
)
