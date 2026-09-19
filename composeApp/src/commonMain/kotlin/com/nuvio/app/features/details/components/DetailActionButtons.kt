package com.nuvio.app.features.details.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.AppIconResource
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
    val onLongClick: (() -> Unit)? = null,
)

/**
 * What you can do with this title, as one row: play it, and the toggles that were folded behind
 * the overflow control.
 *
 * Every control here is a Material component rather than a tinted surface with a click on it, so
 * the press morph, the state layers and the disabled roles are the ones the spec publishes.
 * The toggles are toggles: an action that is on says so by being checked, not by being repainted.
 *
 * m3.material.io/components/buttons/specs
 * m3.material.io/components/icon-buttons/specs
 */
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
    val playPainter = appIconPainter(AppIconResource.PlayerPlay)
    val buttonHeight = if (isTablet) 56.dp else 52.dp
    val iconButtonSize = buttonHeight
    var actionsExpanded by remember { mutableStateOf(false) }
    val menuProgress by animateFloatAsState(
        targetValue = if (actionsExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "detail_action_menu_progress",
    )
    val hasSecondaryActions = secondaryActions.isNotEmpty()
    val playInteractionSource = remember { MutableInteractionSource() }
    val playLongPress = rememberLongPressGate(
        interactionSource = playInteractionSource,
        onLongClick = onPlayLongClick,
    )

    Box(
        modifier = modifier
            .widthIn(max = if (isTablet) 520.dp else 420.dp)
            .fillMaxWidth()
            .height(buttonHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { if (!playLongPress.consume()) onPlayClick() },
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                enabled = playEnabled,
                shape = RoundedCornerShape(PlayCornerRadius),
                contentPadding = ButtonDefaults.contentPaddingFor(
                    buttonHeight = buttonHeight,
                    hasStartIcon = true,
                ),
                interactionSource = playInteractionSource,
            ) {
                Icon(
                    painter = playPainter,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = playLabel,
                    style = if (isTablet) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (hasSecondaryActions) {
                Spacer(modifier = Modifier.width(12.dp))
                secondaryActions.forEachIndexed { index, action ->
                    // The slot opens by width, so the row's own layout carries the reveal and
                    // the button inside it is never asked to animate its own size.
                    Box(
                        modifier = Modifier
                            .width(iconButtonSize * menuProgress)
                            .height(iconButtonSize)
                            .graphicsLayer { clip = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (actionsExpanded || menuProgress > 0.01f) {
                            DetailIconAction(
                                action = action,
                                progress = menuProgress,
                                size = iconButtonSize,
                            )
                        }
                    }

                    if (index != secondaryActions.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp * menuProgress))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp * menuProgress))

                // The overflow control is itself a toggle: it is either showing the actions or
                // it is not, which is exactly what a checked icon toggle says.
                FilledTonalIconToggleButton(
                    checked = actionsExpanded,
                    onCheckedChange = { actionsExpanded = it },
                    modifier = Modifier.size(iconButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = actionsMenuLabel,
                        modifier = Modifier
                            .size(SecondaryIconSize)
                            .graphicsLayer { rotationZ = 90f * menuProgress },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailIconAction(
    action: DetailSecondaryAction,
    progress: Float,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val longPress = rememberLongPressGate(
        interactionSource = interactionSource,
        onLongClick = action.onLongClick,
    )
    val haptics = LocalHapticFeedback.current

    FilledTonalIconToggleButton(
        checked = action.isActive,
        onCheckedChange = {
            if (longPress.consume()) return@FilledTonalIconToggleButton
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            action.onClick()
        },
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = progress
                scaleX = 0.86f + (0.14f * progress)
                scaleY = 0.86f + (0.14f * progress)
            },
        colors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
        interactionSource = interactionSource,
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.label,
            modifier = Modifier.size(SecondaryIconSize),
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

/** Full-height rounding, the shape the spec gives a button at this size. */
private val PlayCornerRadius = 40.dp

/** `IconButtonDefaults` publishes no icon size for the extra-large configuration. */
private val SecondaryIconSize = 24.dp
