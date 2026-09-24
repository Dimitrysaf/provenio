package io.github.dimitrysaf.provenio.core.streams

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun epochMs(): Long =
    (NSDate().timeIntervalSince1970 * 1000.0).toLong()
