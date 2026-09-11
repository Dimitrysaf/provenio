package io.github.dimitrysaf.provenio.core.platform

actual fun logDebug(tag: String, message: String) {
    println("$tag: $message")
}
