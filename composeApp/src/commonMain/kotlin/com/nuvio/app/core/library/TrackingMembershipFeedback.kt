package com.nuvio.app.core.library

import com.nuvio.app.core.tracking.TrackingLibraryTab

internal fun List<TrackingLibraryTab>.statusTitle(
    key: String,
    providerName: String,
): String = firstOrNull { tab -> tab.key == key }
    ?.title
    ?.removePrefix("$providerName ")
    ?: key.substringAfterLast(':')
