package com.engine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EngineConfigTest {
    private fun config(
        mode: UploadMode = UploadMode.Unlimited,
        limit: Long = 0,
    ) = EngineConfig(
        dataDirectory = File("data"),
        cacheDirectory = File("cache"),
        uploadMode = mode,
        uploadLimitBytesPerSecond = limit,
    )

    @Test
    fun defaultConfigurationIsValid() {
        val configuration = config()
        configuration.validate()
        assertEquals(TorrentProfile.Balanced, configuration.torrentProfile)
    }

    @Test
    fun limitedUploadRequiresPositiveRate() {
        assertFailsWith<IllegalArgumentException> {
            config(UploadMode.Limited, 0).validate()
        }
    }

    @Test
    fun unlimitedUploadRejectsStoredRate() {
        assertFailsWith<IllegalArgumentException> {
            config(UploadMode.Unlimited, 1024).validate()
        }
    }

    @Test
    fun platformRangesAreValidatedBeforeJni() {
        assertFailsWith<IllegalArgumentException> {
            config().copy(listenPort = 65_536).validate()
        }
        assertFailsWith<IllegalArgumentException> {
            config().copy(warmTorrentTimeoutMilliseconds = -1).validate()
        }
    }

    @Test
    fun explicitTlsBundleMustBeARegularNonEmptyFile() {
        assertFailsWith<IllegalArgumentException> {
            config().copy(tlsCaBundle = File("missing-ca.pem")).validate()
        }
    }

    @Test
    fun streamProgressRatiosAreBounded() {
        val stats = StreamStats(
            fileIndex = 0,
            fileSize = 100,
            contiguousReadyBytes = 25,
            verifiedFileBytes = 150,
            deliveredBytes = 10,
        )
        assertEquals(0.25f, stats.bufferProgress)
        assertEquals(1f, stats.fileProgress)
    }
}
