package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIViewController

@Composable
internal actual fun rememberImageSaver(): (ImageSaveRequest) -> Unit {
    val scope = rememberCoroutineScope()
    val viewController = LocalUIViewController.current
    return remember(viewController) {
        { request ->
            scope.launch {
                val fileUrl = withContext(Dispatchers.Default) {
                    val source = NSURL.URLWithString(request.url) ?: return@withContext null
                    val data = NSData.dataWithContentsOfURL(source) ?: return@withContext null
                    val target = NSURL.fileURLWithPath(NSTemporaryDirectory() + request.fileName)
                    target.takeIf { data.writeToURL(it, atomically = true) }
                } ?: return@launch
                val presenter = viewController.topmostPresented()
                val controller = UIActivityViewController(
                    activityItems = listOf(fileUrl),
                    applicationActivities = null,
                )
                controller.popoverPresentationController?.sourceView = presenter.view
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

private fun UIViewController.topmostPresented(): UIViewController =
    presentedViewController?.topmostPresented() ?: this
