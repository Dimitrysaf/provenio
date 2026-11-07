package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import io.github.dimitrysaf.provenio.core.startup.AppStartupState
import io.github.dimitrysaf.provenio.shell.AppLoadingContent
import io.github.dimitrysaf.provenio.shell.AppTheme
import kotlinx.coroutines.delay

@Composable
fun AppLaunchScreen(
    content: @Composable () -> Unit,
) {
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
            // The launch screen sits outside the app, so it takes the app's theme itself: the
            // name, spinner and background in the palette chosen in Appearance.
            AppTheme {
                // The "p" drops in, the name follows it letter by letter, then the spinner.
                AppLoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            }
        }
    }
}

private const val MaxLaunchScreenMillis = 6_000L
