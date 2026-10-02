package io.github.dimitrysaf.provenio.core.diagnostics

// A file ready to save: one .log, or a .zip when the logs span several files.
class AppLogBundle(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

enum class AppLogSaveResult {
    SAVED,
    CANCELLED,
    FAILED,
}

// Everything the app has logged, at every level, gathered for the user to save.
expect object AppLogs {
    val isSupported: Boolean

    suspend fun collect(): AppLogBundle?
}

internal const val AppLogBaseName = "provenio-logs"
