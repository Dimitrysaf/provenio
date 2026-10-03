package io.github.dimitrysaf.provenio.desktop

import androidx.compose.runtime.Composable
import io.github.dimitrysaf.provenio.shell.components.AppLaunchScreen
import io.github.dimitrysaf.provenio.shell.theme.ThemeColors
import org.jetbrains.compose.resources.painterResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_splash_logo

@Composable
internal fun DesktopLaunchScreen(content: @Composable () -> Unit) {
    AppLaunchScreen(
        logo = painterResource(Res.drawable.app_splash_logo),
        background = ThemeColors.White.background,
        content = content,
    )
}
