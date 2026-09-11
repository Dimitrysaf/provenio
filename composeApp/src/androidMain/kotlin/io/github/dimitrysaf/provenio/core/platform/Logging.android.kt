package io.github.dimitrysaf.provenio.core.platform

import android.util.Log

actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}
