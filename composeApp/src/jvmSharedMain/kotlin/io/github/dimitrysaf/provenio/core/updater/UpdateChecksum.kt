package io.github.dimitrysaf.provenio.core.updater

import java.io.File
import java.security.MessageDigest

/**
 * True when [sha256] is unknown or matches the file's SHA-256, the digest GitHub publishes for
 * each release asset. An update is only installed once its download matches it.
 */
internal fun File.matchesSha256(sha256: String?): Boolean {
    val expected = sha256?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return true
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) } == expected
}
