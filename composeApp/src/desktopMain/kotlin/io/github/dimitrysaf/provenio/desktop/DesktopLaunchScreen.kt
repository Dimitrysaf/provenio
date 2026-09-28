package io.github.dimitrysaf.provenio.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.startup.AppStartupState
import io.github.dimitrysaf.provenio.shell.theme.ThemeColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.app_splash_logo

@Composable
internal fun DesktopLaunchScreen(content: @Composable () -> Unit) {
    val firstScreenReady by AppStartupState.firstScreenReady.collectAsState()
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(MaxLaunchScreenMillis)
        timedOut = true
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        AnimatedVisibility(
            visible = !firstScreenReady && !timedOut,
            enter = EnterTransition.None,
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ThemeColors.White.background),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.app_splash_logo),
                    contentDescription = null,
                    modifier = Modifier.size(288.dp),
                )
            }
        }
    }
}

private const val MaxLaunchScreenMillis = 6_000L
