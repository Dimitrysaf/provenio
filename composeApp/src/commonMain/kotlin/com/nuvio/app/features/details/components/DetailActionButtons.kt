package com.nuvio.app.features.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.SingleChoiceBottomSheet
import com.nuvio.app.core.ui.SingleChoiceOption
import com.nuvio.app.core.ui.appIconPainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_play
import nuvio.composeapp.generated.resources.details_actions_menu_label
import org.jetbrains.compose.resources.stringResource

data class DetailSecondaryAction(
    val label: String,
    val icon: ImageVector,
    val isActive: Boolean = false,
    val onClick: () -> Unit = {},
)

/** Play, split from the rest of what can be done with this title, which opens as a sheet of options. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailActionButtons(
    modifier: Modifier = Modifier,
    playLabel: String = stringResource(Res.string.action_play),
    playEnabled: Boolean = true,
    secondaryActions: List<DetailSecondaryAction> = emptyList(),
    actionsMenuLabel: String = stringResource(Res.string.details_actions_menu_label),
    isTablet: Boolean = false,
    onPlayClick: () -> Unit = {},
    onPlayLongClick: (() -> Unit)? = null,
) {
    val buttonHeight = SplitButtonDefaults.MediumContainerHeight
    var actionsSheetVisible by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (actionsSheetVisible) 180f else 0f,
        label = "detail_actions_chevron",
    )
    val playInteractionSource = remember { MutableInteractionSource() }
    val playLongPress = rememberLongPressGate(
        interactionSource = playInteractionSource,
        onLongClick = onPlayLongClick,
    )
    val playContent: @Composable RowScope.() -> Unit = {
        Icon(
            painter = appIconPainter(AppIconResource.PlayerPlay),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight)),
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(buttonHeight)))
        Text(
            text = playLabel,
            style = ButtonDefaults.textStyleFor(buttonHeight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth(),
    ) {
        if (secondaryActions.isEmpty()) {
            Button(
                onClick = { if (!playLongPress.consume()) onPlayClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                enabled = playEnabled,
                contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight, hasStartIcon = true),
                interactionSource = playInteractionSource,
                content = playContent,
            )
        } else {
            // The layout measures the leading button first, so it is given the width the trailing one leaves.
            SplitButtonLayout(
                leadingButton = {
                    SplitButtonDefaults.LeadingButton(
                        onClick = { if (!playLongPress.consume()) onPlayClick() },
                        modifier = Modifier
                            .width(maxWidth - buttonHeight - SplitButtonDefaults.Spacing)
                            .height(buttonHeight),
                        enabled = playEnabled,
                        shapes = SplitButtonDefaults.leadingButtonShapesFor(buttonHeight),
                        contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(buttonHeight),
                        interactionSource = playInteractionSource,
                        content = playContent,
                    )
                },
                trailingButton = {
                    SplitButtonDefaults.TrailingButton(
                        checked = actionsSheetVisible,
                        onCheckedChange = { actionsSheetVisible = it },
                        modifier = Modifier.size(buttonHeight),
                        shapes = SplitButtonDefaults.trailingButtonShapesFor(buttonHeight),
                        contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(buttonHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = actionsMenuLabel,
                            modifier = Modifier
                                .size(SplitButtonDefaults.trailingButtonIconSizeFor(buttonHeight))
                                .graphicsLayer { rotationZ = chevronRotation },
                        )
                    }
                },
            )
        }
    }

    if (actionsSheetVisible) {
        SingleChoiceBottomSheet(
            title = actionsMenuLabel,
            options = secondaryActions.map { action ->
                SingleChoiceOption(
                    value = action,
                    label = action.label,
                    leadingContent = { Icon(imageVector = action.icon, contentDescription = null) },
                )
            },
            isSelected = { it.isActive },
            onSelected = { it.onClick() },
            onDismiss = { actionsSheetVisible = false },
        )
    }
}

/**
 * Long press on a Material button.
 *
 * The button components take a click and nothing else, so the press is read off the interaction
 * source instead: a press that outlives the platform's long-press timeout fires [onLongClick] and
 * marks the release that follows as already spent. The click handler asks [LongPressGate.consume]
 * whether it is that spent release, which is what keeps one gesture from counting twice.
 *
 * `collectLatest` is what times it: the release or cancel arrives as the next interaction and
 * cancels the pending wait, so a short press never reaches the timeout at all.
 */
@Stable
private class LongPressGate {
    var fired by mutableStateOf(false)

    fun consume(): Boolean {
        if (!fired) return false
        fired = false
        return true
    }
}

@Composable
private fun rememberLongPressGate(
    interactionSource: InteractionSource,
    onLongClick: (() -> Unit)?,
): LongPressGate {
    val gate = remember { LongPressGate() }
    val timeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
    val haptics = LocalHapticFeedback.current
    // The handler is read through the latest state rather than keyed on: a lambda is a new
    // object on every recomposition, and restarting the collection mid-press would lose it.
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val armed = onLongClick != null
    LaunchedEffect(interactionSource, timeoutMillis, armed) {
        if (!armed) {
            gate.fired = false
            return@LaunchedEffect
        }
        interactionSource.interactions.collectLatest { interaction ->
            if (interaction !is PressInteraction.Press) return@collectLatest
            // A press that was never released cannot leave the gate armed for the next tap.
            gate.fired = false
            delay(timeoutMillis)
            gate.fired = true
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            currentOnLongClick?.invoke()
        }
    }
    return gate
}
