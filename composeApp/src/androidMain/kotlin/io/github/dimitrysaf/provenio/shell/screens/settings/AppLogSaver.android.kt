package io.github.dimitrysaf.provenio.shell.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveFailure
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult

private class SaveDocumentContract : ActivityResultContract<AppLogBundle, Uri?>() {
    override fun createIntent(context: Context, input: AppLogBundle): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

// The logs waiting for the place to save them, kept in a file while the system's save dialog is
// open. The app can be stopped meanwhile, more so on a device short of memory, and started again
// for the dialog's answer; logs kept only in memory were gone by then, and the file the dialog had
// made stayed empty.
private object PendingLogs {
    private fun file(context: Context) = File(context.cacheDir, "pending-logs")

    fun keep(context: Context, bundle: AppLogBundle) {
        file(context).writeBytes(bundle.bytes)
    }

    fun take(context: Context): ByteArray? {
        val file = file(context)
        if (!file.exists()) return null
        return runCatching { file.readBytes() }.getOrNull().also { file.delete() }
    }

    fun drop(context: Context) {
        file(context).delete()
    }
}

@Composable
internal actual fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit {
    val context = LocalContext.current
    val currentOnFinished = rememberUpdatedState(onFinished)
    val launcher = rememberLauncherForActivityResult(SaveDocumentContract()) { uri ->
        val bytes = PendingLogs.take(context)
        if (uri == null) {
            currentOnFinished.value(AppLogSaveResult.Cancelled)
            return@rememberLauncherForActivityResult
        }
        if (bytes == null || bytes.isEmpty()) {
            currentOnFinished.value(
                AppLogSaveResult.Failed(AppLogSaveFailure.Lost, emptyFileLeft = !deleteDocument(context, uri)),
            )
            return@rememberLauncherForActivityResult
        }
        // Some places to save to take only "w", others only "wt"; the file is new, so either starts
        // it empty. What went wrong with the last one tried is what the user is told.
        var failure: Throwable? = null
        val written = SaveModes.any { mode ->
            runCatching {
                context.contentResolver.openOutputStream(uri, mode)?.use { it.write(bytes) }
                    ?: error("The place chosen gave nowhere to write to")
            }.onFailure { failure = it }.isSuccess
        }
        // A place that says how big the file is can show that nothing reached it after all.
        val empty = written && documentSize(context, uri) == 0L
        val result = if (written && !empty) {
            AppLogSaveResult.Saved
        } else {
            AppLogSaveResult.Failed(
                reason = if (written) AppLogSaveFailure.Empty else AppLogSaveFailure.WriteFailed,
                detail = failure?.let { it.message ?: it::class.simpleName },
                emptyFileLeft = !deleteDocument(context, uri),
            )
        }
        currentOnFinished.value(result)
    }
    return remember(launcher) {
        { bundle ->
            runCatching {
                PendingLogs.keep(context, bundle)
                launcher.launch(bundle)
            }
                .onFailure { error ->
                    PendingLogs.drop(context)
                    currentOnFinished.value(
                        AppLogSaveResult.Failed(AppLogSaveFailure.WriteFailed, detail = error.message),
                    )
                }
        }
    }
}

/** The size the place saved to reports for the file, or null where it does not say. */
private fun documentSize(context: Context, uri: Uri): Long? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
    }
}.getOrNull()

/** Removes the file the save dialog made; false if it could not be. */
private fun deleteDocument(context: Context, uri: Uri): Boolean =
    runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }.getOrDefault(false)

private val SaveModes = listOf("w", "wt")
