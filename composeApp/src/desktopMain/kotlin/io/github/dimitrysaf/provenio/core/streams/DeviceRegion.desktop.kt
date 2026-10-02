package io.github.dimitrysaf.provenio.core.streams

import java.util.Locale

internal actual fun deviceRegion(): String? = Locale.getDefault().country
