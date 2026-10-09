package io.github.dimitrysaf.provenio.shell.screens.settings

import android.os.Handler
import android.os.Looper
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import io.github.dimitrysaf.provenio.shell.components.BottomSheetBodyMargin
import io.github.dimitrysaf.provenio.shell.components.LocalWindowBreakpoint
import io.github.dimitrysaf.provenio.shell.components.ModalSheet
import io.github.dimitrysaf.provenio.shell.components.SheetHeader
import io.github.dimitrysaf.provenio.shell.components.SheetNavigation
import io.github.dimitrysaf.provenio.shell.components.ToastController
import io.github.dimitrysaf.provenio.shell.components.dismissBottomSheet
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_close
import provenio.composeapp.generated.resources.local_sync_scan_code
import provenio.composeapp.generated.resources.local_sync_scanner_hint
import provenio.composeapp.generated.resources.local_sync_scanner_torch_off
import provenio.composeapp.generated.resources.local_sync_scanner_torch_on
import provenio.composeapp.generated.resources.local_sync_scanner_unavailable

/** The camera scanner for a pairing code: a bottom sheet on phones, a dialog on large screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QrScannerSheet(
    onCode: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(Res.string.local_sync_scan_code)
    if (LocalWindowBreakpoint.current.isTwoPane) {
        BasicAlertDialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.widthIn(max = 440.dp),
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.action_close))
                        }
                    }
                    QrScannerBody(onCode = onCode, horizontalPadding = 24.dp)
                }
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val close: () -> Unit = { scope.launch { dismissBottomSheet(sheetState, onDismiss) } }
        ModalSheet(
            onDismissRequest = close,
            sheetState = sheetState,
        ) {
            SheetHeader(
                title = title,
                navigation = SheetNavigation.Close,
                onNavigate = close,
            )
            QrScannerBody(
                onCode = onCode,
                horizontalPadding = BottomSheetBodyMargin,
                modifier = Modifier.padding(bottom = safeBottomPadding(24.dp)),
            )
        }
    }
}

@Composable
private fun QrScannerBody(
    onCode: (String) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    var torchOn by remember { mutableStateOf(false) }
    var hasTorch by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.large)
                .background(Color.Black),
        ) {
            QrCameraPreview(
                torchOn = torchOn,
                onTorchAvailable = { hasTorch = it },
                onCode = onCode,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
        }
        Text(
            text = stringResource(Res.string.local_sync_scanner_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (hasTorch) {
            FilledTonalIconToggleButton(
                checked = torchOn,
                onCheckedChange = { torchOn = it },
            ) {
                Icon(
                    imageVector = if (torchOn) FlashOnIcon else FlashOffIcon,
                    contentDescription = stringResource(
                        if (torchOn) Res.string.local_sync_scanner_torch_off else Res.string.local_sync_scanner_torch_on,
                    ),
                )
            }
        }
    }
}

/**
 * The back camera's picture, read for a QR code frame by frame. Calls [onCode] once with the first
 * code it reads, and reports through [onTorchAvailable] whether the camera has a light to turn on.
 */
@Composable
private fun QrCameraPreview(
    torchOn: Boolean,
    onTorchAvailable: (Boolean) -> Unit,
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCode by rememberUpdatedState(onCode)
    val currentOnTorchAvailable by rememberUpdatedState(onTorchAvailable)
    val unavailableMessage = stringResource(Res.string.local_sync_scanner_unavailable)
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(context, lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val delivered = AtomicBoolean(false)
        val mainHandler = Handler(Looper.getMainLooper())
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener(
            {
                runCatching {
                    val cameraProvider = providerFuture.get().also { provider = it }
                    camera = bindScanner(
                        provider = cameraProvider,
                        lifecycleOwner = lifecycleOwner,
                        previewView = previewView,
                        executor = analysisExecutor,
                    ) { text ->
                        if (delivered.compareAndSet(false, true)) mainHandler.post { currentOnCode(text) }
                    }.also { bound -> currentOnTorchAvailable(bound.cameraInfo.hasFlashUnit()) }
                }.onFailure { ToastController.show(unavailableMessage) }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
            camera = null
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun bindScanner(
    provider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    executor: ExecutorService,
    onCode: (String) -> Unit,
): Camera {
    val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
        CameraSelector.DEFAULT_BACK_CAMERA
    } else {
        CameraSelector.DEFAULT_FRONT_CAMERA
    }
    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { it.setAnalyzer(executor, QrCodeAnalyzer(onCode)) }
    provider.unbindAll()
    return provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
}

/** Reads QR codes from the brightness plane of each camera frame with ZXing. */
private class QrCodeAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            val plane = frame.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val source = PlanarYUVLuminanceSource(
                bytes,
                plane.rowStride,
                frame.height,
                0,
                0,
                frame.width,
                frame.height,
                false,
            )
            val text = runCatching { reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text }
                .getOrNull()
            reader.reset()
            if (!text.isNullOrBlank()) onCode(text)
        }
    }
}

private val FlashOnIcon: ImageVector = ImageVector.Builder(
    name = "FlashOn",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M7,3v9c0,0.55 0.45,1 1,1h2v7.15c0,0.51 0.67,0.69 0.93,0.25l5.19,-8.9c0.39,-0.67 -0.09,-1.5 " +
            "-0.86,-1.5H14l2.49,-6.65C16.74,2.7 16.26,2 15.56,2H8C7.45,2 7,2.45 7,3z",
    ),
    fill = SolidColor(Color.Black),
).build()

private val FlashOffIcon: ImageVector = ImageVector.Builder(
    name = "FlashOff",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).addPath(
    pathData = addPathNodes(
        "M16.12,11.5c0.39,-0.67 -0.09,-1.5 -0.86,-1.5h-1.87l2.28,2.28 0.45,-0.78zM16.28,3.35C16.53,2.7 " +
            "16.05,2 15.35,2H8C7.45,2 7,2.45 7,3v0.61l6.13,6.13 3.15,-6.39zM20.29,18.88L4.12,2.71c-0.39,-0.39 " +
            "-1.02,-0.39 -1.41,0 -0.39,0.39 -0.39,1.02 0,1.41L7,8.42V12c0,0.55 0.45,1 1,1h2v7.15c0,0.51 " +
            "0.67,0.69 0.93,0.25l2.65,-4.55 5.3,5.3c0.39,0.39 1.02,0.39 1.41,0 0.39,-0.38 0.39,-1.02 0,-1.41z",
    ),
    fill = SolidColor(Color.Black),
).build()
