package com.engine

import java.io.File

public enum class UploadMode(internal val nativeValue: Int) {
    Disabled(0),
    Unlimited(1),
    Limited(2),
}

public enum class TorrentProfile(internal val nativeValue: Int) {
    Soft(0),
    Balanced(1),
    Fast(2),
}

public data class EngineConfig(
    val dataDirectory: File,
    val cacheDirectory: File,
    val memoryCacheCapacityBytes: Long = 64L * 1024L * 1024L,
    val diskCacheCapacityBytes: Long = 2L * 1024L * 1024L * 1024L,
    val torrentProfile: TorrentProfile = TorrentProfile.Balanced,
    val listenPort: Int = 0,
    val uploadMode: UploadMode = UploadMode.Unlimited,
    val uploadLimitBytesPerSecond: Long = 0L,
    val streamInactivityTimeoutMilliseconds: Int = 30_000,
    val warmTorrentTimeoutMilliseconds: Int = 60_000,
    val tlsCaBundle: File? = null,
) {
    internal fun validate() {
        require(memoryCacheCapacityBytes >= 0) { "memory cache capacity must be non-negative" }
        require(diskCacheCapacityBytes >= 0) { "disk cache capacity must be non-negative" }
        require(listenPort in 0..65_535) { "listen port must be between 0 and 65535" }
        require(streamInactivityTimeoutMilliseconds >= 0) {
            "stream inactivity timeout must be non-negative"
        }
        require(warmTorrentTimeoutMilliseconds >= 0) {
            "warm torrent timeout must be non-negative"
        }
        when (uploadMode) {
            UploadMode.Limited -> require(uploadLimitBytesPerSecond > 0) {
                "limited upload mode requires a positive byte rate"
            }
            UploadMode.Disabled,
            UploadMode.Unlimited,
            -> require(uploadLimitBytesPerSecond == 0L) {
                "disabled and unlimited upload modes require a zero byte rate"
            }
        }
        tlsCaBundle?.let(::validateTlsCaBundle)
    }

    internal fun validateTlsCaBundle(file: File) {
        require(file.isFile) { "TLS CA bundle must be a regular file" }
        require(file.length() in 1..MAXIMUM_TLS_CA_BUNDLE_BYTES) {
            "TLS CA bundle must contain between 1 byte and 16 MiB"
        }
    }

    private companion object {
        const val MAXIMUM_TLS_CA_BUNDLE_BYTES = 16L * 1024L * 1024L
    }
}
