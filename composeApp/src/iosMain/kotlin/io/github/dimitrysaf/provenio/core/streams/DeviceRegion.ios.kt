package io.github.dimitrysaf.provenio.core.streams

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale

internal actual fun deviceRegion(): String? =
    NSLocale.currentLocale.objectForKey(NSLocaleCountryCode) as? String
