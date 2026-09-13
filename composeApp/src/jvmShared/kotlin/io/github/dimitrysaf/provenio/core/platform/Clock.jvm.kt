package io.github.dimitrysaf.provenio.core.platform

import java.time.LocalDate

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun todayIso(): String = LocalDate.now().toString()
