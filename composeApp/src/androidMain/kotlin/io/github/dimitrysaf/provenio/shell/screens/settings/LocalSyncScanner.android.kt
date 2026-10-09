package io.github.dimitrysaf.provenio.shell.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.dimitrysaf.provenio.shell.components.ToastController
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.local_sync_camera_denied

/**
 * Scans a pairing code with the app's own camera scanner: a bottom sheet on phones and a dialog on
 * large screens. Asks for the camera first, and offers nothing on a device without one, where the
 * code is entered instead.
 */
@Composable
internal actual fun rememberLocalSyncScanner(onResult: (String?) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val hasCamera = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    if (!hasCamera) return null

    val deniedMessage = stringResource(Res.string.local_sync_camera_denied)
    val currentOnResult by rememberUpdatedState(onResult)
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scannerOpen = true else ToastController.show(deniedMessage)
    }

    if (scannerOpen) {
        QrScannerSheet(
            onCode = { code ->
                scannerOpen = false
                currentOnResult(code)
            },
            onDismiss = { scannerOpen = false },
        )
    }

    return remember(context, permissionLauncher) {
        {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) scannerOpen = true else permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}
