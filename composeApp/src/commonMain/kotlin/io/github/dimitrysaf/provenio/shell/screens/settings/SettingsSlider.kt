package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import provenio.composeapp.generated.resources.*
import kotlin.math.roundToInt

internal fun formatStep(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}

fun snapToStep(value: Float, step: Float): Float {
    return (value / step).roundToInt() * step
}

fun calculateSteps(
    min: Float,
    max: Float,
    stepSize: Float
): Int {
    val totalSteps = ((max - min) / stepSize).roundToInt()
    return (totalSteps - 1).coerceAtLeast(0)
}

@Composable
fun ValueBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// Declares rows, so it must not be skippable: see SettingsListScope.
@Composable
@NonRestartableComposable
internal fun SettingsListScope.SettingsSliderRow(
    title: String,
    value: Int,
    /** The label for a value, shown for the one under the thumb while it is dragged. */
    valueText: @Composable (Int) -> String,
    valueRange: IntRange,
    step: Int,
    isTablet: Boolean,
    enabled: Boolean = true,
    /** Answers a tap on the row while it is off, so it can say why. */
    onDisabledClick: (() -> Unit)? = null,
    onValueChange: (Int) -> Unit,
) = shapedRow { shape ->
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    var sliderValue by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val current = sliderValue.roundToInt().coerceIn(valueRange.first, valueRange.last)

    SettingsSliderContainer(shape = shape, enabled = enabled, onDisabledClick = onDisabledClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            ValueBox(text = valueText(current), modifier = Modifier.wrapContentWidth())
        }
        Slider(
            value = sliderValue.coerceIn(valueRange.first.toFloat(), valueRange.last.toFloat()),
            // Applied on each new step while dragging, so what it changes follows the thumb; the
            // steps keep that to a few changes a drag.
            onValueChange = {
                if (enabled) {
                    sliderValue = snapToStep(it, step.toFloat())
                    val stepped = sliderValue.roundToInt().coerceIn(valueRange.first, valueRange.last)
                    if (stepped != value) onValueChange(stepped)
                }
            },
            onValueChangeFinished = {
                if (enabled && current != value) onValueChange(current)
            },
            enabled = enabled,
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = calculateSteps(valueRange.first.toFloat(), valueRange.last.toFloat(), step.toFloat()),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        }
    }
}

/**
 * The container a slider row paints for itself.
 *
 * A slider needs the row's whole width beneath its label, which is not a shape a list item's slots
 * make, so the row draws the group's container itself and keeps the corners it was given.
 */
@Composable
internal fun SettingsSliderContainer(
    shape: RoundedCornerShape,
    enabled: Boolean,
    onDisabledClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.55f)
            .then(
                if (!enabled && onDisabledClick != null) {
                    Modifier.clip(shape).clickable(onClick = onDisabledClick)
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        content()
    }
}
