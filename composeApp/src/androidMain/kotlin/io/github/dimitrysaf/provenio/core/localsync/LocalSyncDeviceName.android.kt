package io.github.dimitrysaf.provenio.core.localsync

import android.os.Build

internal actual fun localSyncDeviceName(): String =
    listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { !it.isNullOrBlank() }
        .distinct()
        .joinToString(" ")
        .ifBlank { "Android" }
