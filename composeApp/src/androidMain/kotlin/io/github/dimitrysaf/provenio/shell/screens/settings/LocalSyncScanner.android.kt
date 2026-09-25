package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.local_sync_scan_prompt

@Composable
internal actual fun rememberLocalSyncScanner(onResult: (String?) -> Unit): (() -> Unit)? {
    val prompt = stringResource(Res.string.local_sync_scan_prompt)
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        currentOnResult(result.contents)
    }
    return remember(launcher, prompt) {
        {
            launcher.launch(
                ScanOptions()
                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    .setPrompt(prompt)
                    .setBeepEnabled(false),
            )
        }
    }
}
