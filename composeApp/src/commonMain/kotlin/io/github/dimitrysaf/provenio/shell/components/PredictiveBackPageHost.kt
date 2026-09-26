package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// Switches in-screen pages with the shared axis pattern, and lets the back gesture preview the page it returns to.
@OptIn(ExperimentalTransitionApi::class)
@Composable
internal fun <T : Any> PredictiveBackPageHost(
    page: T,
    backPage: T?,
    backEnabled: Boolean,
    isForward: (from: T, to: T) -> Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val slidePx = with(density) { M3Motion.SharedAxisSlide.roundToPx() } * if (rtl) -1 else 1
    val transitionState = remember { SeekableTransitionState(page) }
    var predictive by remember { mutableStateOf(false) }
    val currentBackPage by rememberUpdatedState(backPage)
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(page) {
        if (transitionState.currentState != page || transitionState.targetState != page) {
            transitionState.animateTo(page)
        }
        predictive = false
    }

    PlatformPredictiveBackHandler(enabled = backEnabled && backPage != null) { events ->
        val target = currentBackPage
        if (target == null) {
            events.collect {}
            return@PlatformPredictiveBackHandler
        }
        predictive = true
        try {
            events.collect { event ->
                transitionState.seekTo(event.progress, target)
            }
            currentOnBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                transitionState.animateTo(transitionState.currentState)
                predictive = false
            }
            throw cancelled
        }
    }

    rememberTransition(transitionState, label = "pageHost").AnimatedContent(
        modifier = modifier,
        transitionSpec = {
            if (predictive) M3Motion.predictiveBack() else M3Motion.sharedAxisX(isForward(initialState, targetState), slidePx)
        },
    ) { shownPage ->
        Box(modifier = Modifier.fillMaxSize()) {
            content(shownPage)
        }
    }
}
