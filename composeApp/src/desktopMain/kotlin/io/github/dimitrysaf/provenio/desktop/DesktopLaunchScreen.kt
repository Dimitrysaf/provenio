package io.github.dimitrysaf.provenio.desktop

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.shell.components.AppLaunchScreen

@Composable
internal fun DesktopLaunchScreen(content: @Composable () -> Unit) {
    AppLaunchScreen(content = content)
}
