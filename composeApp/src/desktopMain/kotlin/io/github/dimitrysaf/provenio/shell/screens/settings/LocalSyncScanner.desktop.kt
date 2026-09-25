package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.Composable

// Computers rarely have a camera pointed at a phone, so desktop pairs by entering the code.
@Composable
internal actual fun rememberLocalSyncScanner(onResult: (String?) -> Unit): (() -> Unit)? = null
