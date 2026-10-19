package io.github.dimitrysaf.provenio.shell.components

import coil3.ImageLoader
import coil3.disk.DiskCache
import io.github.dimitrysaf.provenio.desktop.Context
import okio.Path.Companion.toOkioPath

/** Keeps the image cache with the app's own cache, so Storage can measure, clear and move it. */
internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder =
    diskCache {
        DiskCache.Builder()
            .directory(Context.app.cacheDir.resolve("image_cache").toOkioPath())
            .maxSizePercent(ImageCacheDiskShare)
            .build()
    }

private const val ImageCacheDiskShare = 0.02
