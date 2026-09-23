package com.nuvio.app.shell.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.os.Build

actual fun isDynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun rememberDynamicColorScheme(): ColorScheme? {
    if (!isDynamicColorAvailable()) return null
    val context = LocalContext.current
    return dynamicDarkColorScheme(context)
}
