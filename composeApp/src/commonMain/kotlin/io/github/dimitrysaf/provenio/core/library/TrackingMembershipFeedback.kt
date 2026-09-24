package io.github.dimitrysaf.provenio.core.library

import io.github.dimitrysaf.provenio.core.tracking.TrackingLibraryTab

internal fun List<TrackingLibraryTab>.statusTitle(
    key: String,
    providerName: String,
): String = firstOrNull { tab -> tab.key == key }
    ?.title
    ?.removePrefix("$providerName ")
    ?: key.substringAfterLast(':')
