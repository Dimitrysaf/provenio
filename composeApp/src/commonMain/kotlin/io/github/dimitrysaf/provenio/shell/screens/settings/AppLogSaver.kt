package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogBundle
import io.github.dimitrysaf.provenio.core.diagnostics.AppLogSaveResult

// Opens the system's Save as for the bundle and writes it where the user chooses.
@Composable
internal expect fun rememberAppLogSaver(onFinished: (AppLogSaveResult) -> Unit): (AppLogBundle) -> Unit
