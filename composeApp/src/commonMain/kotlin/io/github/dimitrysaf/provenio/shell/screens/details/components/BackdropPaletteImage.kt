package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.ui.graphics.ImageBitmap
import coil3.request.SuccessResult

internal expect fun loadedBackdropImageBitmap(result: SuccessResult): ImageBitmap?
