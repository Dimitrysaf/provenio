package io.github.dimitrysaf.provenio.desktop

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.shell.components.AppLaunchScreen
import io.github.dimitrysaf.provenio.shell.theme.ThemeColors

@Composable
internal fun DesktopLaunchScreen(content: @Composable () -> Unit) {
    AppLaunchScreen(
        background = ThemeColors.White.background,
        content = content,
    )
}
