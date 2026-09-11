package io.github.dimitrysaf.provenio.core.platform

import io.github.dimitrysaf.provenio.data.requireApplicationContext
import java.io.File

actual fun cacheDirectoryPath(): String {
    val context = requireApplicationContext()
    // Internal, and not externalCacheDir however tempting the extra room is. Everything
    // under /storage/emulated is served by MediaProvider's FUSE daemon rather than by a
    // real filesystem, and libtorrent 2 reaches its files through mmap. Memory-mapping a
    // file that is growing under FUSE walks straight into MediaProvider's node tracking:
    // it aborts on an inode it thinks it already freed, the mount goes with it, and every
    // page this process had mapped there turns into a SIGBUS the moment playback touches
    // it. The film died on its first frame and the tombstone had someone else's name on
    // it. cacheDir is a real filesystem and none of that applies.
    return File(context.cacheDir, "torrents").apply { mkdirs() }.absolutePath
}
