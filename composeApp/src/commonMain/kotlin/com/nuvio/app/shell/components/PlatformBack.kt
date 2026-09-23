package com.nuvio.app.shell.components

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)

expect fun platformExitApp()
