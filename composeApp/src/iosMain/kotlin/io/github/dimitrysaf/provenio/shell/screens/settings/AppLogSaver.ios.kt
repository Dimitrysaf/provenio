package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult

@Composable
internal actual fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit =
    { onFinished(AppLogSaveResult.FAILED) }
