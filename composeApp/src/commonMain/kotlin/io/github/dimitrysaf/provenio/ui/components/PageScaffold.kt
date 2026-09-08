package io.github.dimitrysaf.provenio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import io.github.dimitrysaf.provenio.theme.MotionTokens
import io.github.dimitrysaf.provenio.ui.chrome.LocalBarsVisible

/**
 * Standard page frame: a top app bar that collapses as the body scrolls under it, plus a
 * responsive body. The bar is handed the [TopAppBarScrollBehavior] so it can apply the M3
 * on-scroll container colour change; the separate [LocalBarsVisible] flag stays for the
 * full-screen player case, which hides the chrome outright rather than on scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
        AnimatedVisibility(
            visible = LocalBarsVisible.current.value,
            enter = fadeIn(
                tween(MotionTokens.DurationShort4, easing = MotionTokens.StandardDecelerate),
            ) + expandVertically(
                tween(MotionTokens.DurationShort4, easing = MotionTokens.EmphasizedDecelerate),
            ),
            exit = shrinkVertically(
                tween(MotionTokens.DurationShort3, easing = MotionTokens.EmphasizedAccelerate),
            ) + fadeOut(
                tween(MotionTokens.DurationShort3, easing = MotionTokens.StandardAccelerate),
            ),
        ) {
            topBar(scrollBehavior)
        }

        ResponsiveBody(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
        }
    }
}
