package io.github.dimitrysaf.provenio.core.platform

import io.github.dimitrysaf.provenio.data.requireApplicationContext
import java.io.File

actual fun cacheDirectoryPath(): String {
    val context = requireApplicationContext()
    // externalCacheDir when there is one: torrent data is large, and internal storage on
    // a device with a card is the smaller of the two. Null when no external volume is
    // mounted, so the internal cache is the fallback rather than the assumption.
    val root = context.externalCacheDir ?: context.cacheDir
    return File(root, "torrents").apply { mkdirs() }.absolutePath
}
