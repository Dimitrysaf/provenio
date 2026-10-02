package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult
import io.github.dimitrysaf.provenio.desktop.DesktopFileSaver
import io.github.dimitrysaf.provenio.desktop.SaveChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_save_logs_title

@Composable
internal actual fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnFinished = rememberUpdatedState(onFinished)
    return remember {
        { bundle ->
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    val title = runBlocking { getString(Res.string.settings_save_logs_title) }
                    when (val choice = DesktopFileSaver.chooseSaveFile(title, bundle.fileName)) {
                        SaveChoice.Cancelled -> AppLogSaveResult.CANCELLED
                        is SaveChoice.Chosen -> if (runCatching { choice.file.writeBytes(bundle.bytes) }.isSuccess) {
                            AppLogSaveResult.SAVED
                        } else {
                            AppLogSaveResult.FAILED
                        }
                    }
                }
                currentOnFinished.value(result)
            }
        }
    }
}
