package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// A detached modal side sheet, the large-window stand-in for a bottom sheet, like the profile editor's.
@Composable
internal fun ModalSideSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val visibleState = remember { MutableTransitionState(false) }.apply { targetState = true }
        val backPreview = rememberSideSheetBackPreview(enabled = true, onBack = onDismissRequest)
        Box(modifier = Modifier.fillMaxSize()) {
            // The scrim sits behind the sheet, so only a tap outside the sheet dismisses it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
            AnimatedVisibility(
                visibleState = visibleState,
                enter = slideInHorizontally(tween(SideSheetEnterMillis, easing = M3Motion.EmphasizedDecelerate)) { it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                Surface(
                    modifier = modifier
                        .padding(16.dp)
                        .then(backPreview.modifier())
                        .width(SideSheetWidth)
                        .fillMaxHeight(),
                    shape = ShapeDefaults.ExtraLarge,
                    color = containerColor,
                    contentColor = contentColor,
                ) {
                    Column(
                        modifier = Modifier.padding(top = 24.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

private val SideSheetWidth = 400.dp
private const val SideSheetEnterMillis = 400
private val SideSheetBackShrink = 24.dp
private val SideSheetBackGrow = 12.dp
private val SideSheetBackHeightShrink = 48.dp

// Back gesture progress for a side sheet, which previews its dismissal before the gesture commits.
internal class SideSheetBackPreview {
    var progress by mutableFloatStateOf(0f)
    var fromRightEdge by mutableStateOf(true)

    @Composable
    fun modifier(): Modifier {
        val ltr = LocalLayoutDirection.current == LayoutDirection.Ltr
        return Modifier.graphicsLayer {
            // It shrinks toward its own edge, or grows when swiped from the far one.
            val towardSheetEdge = fromRightEdge == ltr
            val widthChange = if (towardSheetEdge) -SideSheetBackShrink.toPx() else SideSheetBackGrow.toPx()
            scaleX = 1f + widthChange * progress / size.width.coerceAtLeast(1f)
            scaleY = 1f - SideSheetBackHeightShrink.toPx() * progress / size.height.coerceAtLeast(1f)
            transformOrigin = TransformOrigin(if (ltr) 1f else 0f, 0.5f)
        }
    }
}

@Composable
internal fun rememberSideSheetBackPreview(enabled: Boolean, onBack: () -> Unit): SideSheetBackPreview {
    val preview = remember { SideSheetBackPreview() }
    val currentOnBack by rememberUpdatedState(onBack)
    PlatformPredictiveBackHandler(enabled = enabled) { events ->
        try {
            events.collect { event ->
                preview.fromRightEdge = event.fromRightEdge
                preview.progress = event.progress
            }
            currentOnBack()
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                animate(preview.progress, 0f) { value, _ -> preview.progress = value }
            }
            throw cancelled
        }
    }
    return preview
}
