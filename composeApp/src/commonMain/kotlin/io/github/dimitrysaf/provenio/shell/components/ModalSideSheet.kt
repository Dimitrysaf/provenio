package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
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
                enter = slideInHorizontally { it } + fadeIn(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                Surface(
                    modifier = modifier
                        .padding(16.dp)
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
