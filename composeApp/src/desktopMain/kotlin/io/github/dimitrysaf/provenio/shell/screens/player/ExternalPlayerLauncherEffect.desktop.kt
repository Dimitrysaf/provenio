package io.github.dimitrysaf.provenio.shell.screens.player

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.core.playback.ExternalPlayerIntentResult

@Composable
actual fun rememberExternalPlayerLauncher(
    onResult: (ExternalPlaybackResult?) -> Unit,
): (ExternalPlayerIntentResult.Success) -> Boolean = { false }
