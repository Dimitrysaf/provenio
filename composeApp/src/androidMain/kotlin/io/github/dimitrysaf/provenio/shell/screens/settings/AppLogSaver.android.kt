package io.github.dimitrysaf.provenio.shell.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class SaveDocumentContract : ActivityResultContract<AppLogBundle, Uri?>() {
    override fun createIntent(context: Context, input: AppLogBundle): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

@Composable
internal actual fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnFinished = rememberUpdatedState(onFinished)
    val pending = remember { arrayOfNulls<AppLogBundle>(1) }
    val launcher = rememberLauncherForActivityResult(SaveDocumentContract()) { uri ->
        val bundle = pending[0]
        pending[0] = null
        if (uri == null || bundle == null) {
            currentOnFinished.value(if (uri == null) AppLogSaveResult.CANCELLED else AppLogSaveResult.FAILED)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bundle.bytes) }
                        ?: error("No output stream")
                }.isSuccess
            }
            currentOnFinished.value(if (saved) AppLogSaveResult.SAVED else AppLogSaveResult.FAILED)
        }
    }
    return remember(launcher) {
        { bundle ->
            pending[0] = bundle
            runCatching { launcher.launch(bundle) }
                .onFailure {
                    pending[0] = null
                    currentOnFinished.value(AppLogSaveResult.FAILED)
                }
        }
    }
}
