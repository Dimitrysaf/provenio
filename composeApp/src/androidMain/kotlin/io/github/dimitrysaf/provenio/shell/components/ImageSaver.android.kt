package io.github.dimitrysaf.provenio.shell.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

private class SaveImageContract : ActivityResultContract<ImageSaveRequest, Uri?>() {
    override fun createIntent(context: Context, input: ImageSaveRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mimeType)
            .putExtra(Intent.EXTRA_TITLE, input.fileName)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

@Composable
internal actual fun rememberImageSaver(): (ImageSaveRequest) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<ImageSaveRequest>(1) }
    val launcher = rememberLauncherForActivityResult(SaveImageContract()) { uri ->
        val request = pending[0]
        pending[0] = null
        if (uri == null || request == null) return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = URL(request.url).openStream().use { it.readBytes() }
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
                }
            }
        }
    }
    return remember(launcher) {
        { request ->
            pending[0] = request
            runCatching { launcher.launch(request) }.onFailure { pending[0] = null }
        }
    }
}
