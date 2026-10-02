package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.dimitrysaf.provenio.desktop.DesktopFileSaver
import io.github.dimitrysaf.provenio.desktop.SaveChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.image_viewer_save_title
import java.net.URL

@Composable
internal actual fun rememberImageSaver(): (ImageSaveRequest) -> Unit {
    val scope = rememberCoroutineScope()
    return remember {
        { request ->
            scope.launch {
                val title = getString(Res.string.image_viewer_save_title)
                withContext(Dispatchers.IO) {
                    val choice = DesktopFileSaver.chooseSaveFile(title, request.fileName)
                    if (choice is SaveChoice.Chosen) {
                        runCatching {
                            val bytes = URL(request.url).openStream().use { it.readBytes() }
                            choice.file.writeBytes(bytes)
                        }
                    }
                }
            }
        }
    }
}
