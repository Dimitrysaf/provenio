package io.github.dimitrysaf.provenio.core.diagnostics

import androidx.compose.runtime.Composable

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

// Shows the system's save dialog for the bundle and writes it where the user chooses.
@Composable
expect fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit

internal const val AppLogBaseName = "provenio-logs"
