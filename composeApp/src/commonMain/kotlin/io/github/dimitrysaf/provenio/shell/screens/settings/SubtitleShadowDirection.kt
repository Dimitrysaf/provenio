package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.playback.SUBTITLE_SHADOW_ANGLE_STEP
import io.github.dimitrysaf.provenio.core.playback.normalizeSubtitleShadowAngle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/**
 * Which way the subtitle shadow falls, set on a dial: the hand points where the shadow goes, in
 * degrees clockwise from straight right. Dragging or tapping the dial turns it, the arrow keys
 * turn it a step at a time, and each step it lands on applies at once with a haptic tick, as the
 * stepped sliders do.
 */
// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
internal fun SettingsListScope.ShadowDirectionRow(
    title: String,
    degrees: Int,
    isTablet: Boolean,
    enabled: Boolean,
    onDisabledClick: (() -> Unit)?,
    onDegreesChange: (Int) -> Unit,
) = shapedRow { shape ->
    val haptics = LocalHapticFeedback.current
    var shown by remember(degrees) { mutableIntStateOf(degrees) }
    val latestChange by rememberUpdatedState(onDegreesChange)
    val turnTo: (Int) -> Unit = { target ->
        val stepped = normalizeSubtitleShadowAngle(target)
        if (stepped != shown) {
            shown = stepped
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            latestChange(stepped)
        }
    }
    val value = stringResource(Res.string.settings_subtitles_shadow_direction_value, directionLabel(shown), shown)
    SettingsSliderContainer(shape = shape, enabled = enabled, onDisabledClick = onDisabledClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 20.dp else 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            ShadowDirectionDial(
                degrees = shown,
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = title
                    stateDescription = value
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = shown.toFloat(),
                        range = 0f..(360f - SUBTITLE_SHADOW_ANGLE_STEP),
                        steps = 360 / SUBTITLE_SHADOW_ANGLE_STEP - 2,
                    )
                    if (enabled) {
                        setProgress { target ->
                            turnTo(target.roundToInt())
                            true
                        }
                    }
                },
                onTurn = turnTo,
            )
        }
    }
}

/**
 * The dial itself: a ring with a mark every 45 degrees and a hand from its centre to a knob on the
 * ring, where the shadow falls.
 */
@Composable
private fun ShadowDirectionDial(
    degrees: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onTurn: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ring = colors.outlineVariant
    val marks = colors.onSurfaceVariant
    val hand = colors.primary
    val focusRing = colors.secondary
    val latestTurn by rememberUpdatedState(onTurn)
    var focused by remember { mutableStateOf(false) }
    val input = if (enabled) {
        Modifier
            .pointerInput(Unit) {
                fun turnToward(position: Offset) {
                    val angle = atan2(position.y - size.height / 2f, position.x - size.width / 2f)
                    latestTurn((angle * 180f / PI.toFloat()).roundToInt())
                }
                detectTapGestures(onTap = ::turnToward)
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val position = change.position
                    val angle = atan2(position.y - size.height / 2f, position.x - size.width / 2f)
                    latestTurn((angle * 180f / PI.toFloat()).roundToInt())
                }
            }
            // Left and right turn it, like a slider; up and down move on to the rows around it,
            // so the dial never holds on to a remote's focus.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        latestTurn(degrees + SUBTITLE_SHADOW_ANGLE_STEP)
                        true
                    }
                    Key.DirectionLeft -> {
                        latestTurn(degrees - SUBTITLE_SHADOW_ANGLE_STEP)
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .focusable()
    } else {
        Modifier
    }
    Canvas(modifier = modifier.size(DialSize).then(input)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val knobRadius = 7.dp.toPx()
        val radius = size.minDimension / 2f - knobRadius
        if (focused) {
            // Material's focus ring, as the other controls draw it, around the dial.
            drawCircle(
                color = focusRing,
                radius = size.minDimension / 2f - 1.5.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        drawCircle(color = ring, radius = radius, center = center, style = Stroke(width = 2.dp.toPx()))
        for (mark in 0 until 360 step 45) {
            val radians = mark * PI.toFloat() / 180f
            val direction = Offset(cos(radians), sin(radians))
            drawLine(
                color = marks,
                start = center + direction * (radius - 5.dp.toPx()),
                end = center + direction * (radius - 1.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val radians = degrees * PI.toFloat() / 180f
        val tip = center + Offset(cos(radians), sin(radians)) * radius
        drawLine(color = hand, start = center, end = tip, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = hand, radius = 3.dp.toPx(), center = center)
        drawCircle(color = hand, radius = knobRadius, center = tip)
    }
}

/** The direction nearest to [degrees], in words, by eighths of a turn. */
@Composable
private fun directionLabel(degrees: Int): String = stringResource(
    when (((degrees + 22) / 45).mod(8)) {
        0 -> Res.string.settings_subtitles_direction_right
        1 -> Res.string.settings_subtitles_direction_down_right
        2 -> Res.string.settings_subtitles_direction_down
        3 -> Res.string.settings_subtitles_direction_down_left
        4 -> Res.string.settings_subtitles_direction_left
        5 -> Res.string.settings_subtitles_direction_up_left
        6 -> Res.string.settings_subtitles_direction_up
        else -> Res.string.settings_subtitles_direction_up_right
    },
)

private val DialSize = 72.dp
