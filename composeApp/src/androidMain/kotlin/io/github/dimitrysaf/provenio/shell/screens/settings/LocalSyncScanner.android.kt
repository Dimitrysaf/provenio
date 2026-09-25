package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import io.github.dimitrysaf.provenio.shell.components.ToastController
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.local_sync_scanner_unavailable

// Google's code scanner runs the camera itself, in its own portrait screen, so the app needs no camera permission.
@Composable
internal actual fun rememberLocalSyncScanner(onResult: (String?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val unavailableMessage = stringResource(Res.string.local_sync_scanner_unavailable)
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(context, unavailableMessage) {
        val scan: () -> Unit = {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build()
            GmsBarcodeScanning.getClient(context, options)
                .startScan()
                .addOnSuccessListener { barcode -> currentOnResult(barcode.rawValue) }
                .addOnFailureListener { ToastController.show(unavailableMessage) }
        }
        scan
    }
}
