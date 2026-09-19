package com.nuvio.app.core.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

actual fun isDynamicColorAvailable(): Boolean = false

@Composable
actual fun rememberDynamicColorScheme(): ColorScheme? = null
