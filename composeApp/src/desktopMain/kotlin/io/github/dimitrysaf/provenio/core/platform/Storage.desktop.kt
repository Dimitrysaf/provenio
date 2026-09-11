package io.github.dimitrysaf.provenio.core.platform

import java.io.File

actual fun cacheDirectoryPath(): String {
    val root = File(System.getProperty("user.home"), ".cache/provenio/torrents")
    root.mkdirs()
    return root.absolutePath
}
