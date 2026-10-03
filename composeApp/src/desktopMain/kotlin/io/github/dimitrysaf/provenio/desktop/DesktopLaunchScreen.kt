package io.github.dimitrysaf.provenio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.dimitrysaf.provenio.shell.components.AppLaunchScreen
import io.github.dimitrysaf.provenio.shell.theme.ThemeColors

@Composable
internal fun DesktopLaunchScreen(content: @Composable () -> Unit) {
    AppLaunchScreen(
        logoColor = DesktopLaunchLogoColor,
        background = ThemeColors.White.background,
        content = content,
    )
}

private val DesktopLaunchLogoColor = Color(0xFFA9C7FF)
