package io.github.dimitrysaf.provenio.core.localsync

import java.net.InetAddress

internal actual fun localSyncDeviceName(): String =
    runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: System.getProperty("os.name").orEmpty().ifBlank { "Computer" }
