package io.github.dimitrysaf.provenio.core.diagnostics

// iOS keeps the app's log in the system log, which only the device's own tools can export.
actual object AppLogs {
    actual val isSupported: Boolean = false

    actual suspend fun collect(): AppLogBundle? = null
}
