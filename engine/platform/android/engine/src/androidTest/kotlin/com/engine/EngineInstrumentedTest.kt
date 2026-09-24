package com.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EngineInstrumentedTest {
    @Test
    fun createsEngineWithExportedAndroidTrustStore() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "engine-instrumented").apply {
            deleteRecursively()
            mkdirs()
        }
        val dataDirectory = File(root, "data")
        val cacheDirectory = File(root, "cache")

        val engine = Engine.create(
            EngineConfig(
                dataDirectory = dataDirectory,
                cacheDirectory = cacheDirectory,
                memoryCacheCapacityBytes = 1024 * 1024,
                diskCacheCapacityBytes = 0,
                uploadMode = UploadMode.Disabled,
                streamInactivityTimeoutMilliseconds = 0,
                warmTorrentTimeoutMilliseconds = 0,
            ),
        )
        try {
            assertEquals("0.1.1", Engine.version)
            assertTrue(Engine.protocolBackendVersion.startsWith("2.0.12"))
            val trustBundle = File(dataDirectory, "tls/android-ca.pem")
            assertTrue(trustBundle.isFile)
            assertTrue(trustBundle.length() > 0)
            assertTrue(trustBundle.readText().contains("-----BEGIN CERTIFICATE-----"))
        } finally {
            engine.close()
            root.deleteRecursively()
        }
    }
}
