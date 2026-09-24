package io.github.dimitrysaf.provenio.core.tracking.trakt

import io.github.dimitrysaf.provenio.core.time.parseZonedIsoDateTimeToEpochMs

internal fun parseTraktIsoDateTimeToEpochMs(value: String): Long? =
    parseZonedIsoDateTimeToEpochMs(value)
