package io.github.dimitrysaf.provenio.shell

import androidx.compose.runtime.Composable

internal actual val playerWindowSupported: Boolean = false

@Composable
internal actual fun DetachedPlayerWindow(
    title: String,
    onCloseRequest: () -> Unit,
    content: @Composable () -> Unit,
) = Unit
