package com.nuvio.app.features.settings

import androidx.compose.runtime.Composable
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption

internal data class TrackingPickerOption<T>(
    val value: T,
    val title: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val unavailableReason: String? = null,
)

/**
 * Pick one tracking source.
 *
 * This used to be a dialog on tablets and a sheet on phones. A list of choices is a sheet at every
 * size now, so the only thing left here is turning tracking's options into the shared picker's.
 */
@Composable
internal fun <T> TrackingAdaptivePicker(
    title: String,
    subtitle: String,
    selectedValue: T,
    options: List<TrackingPickerOption<T>>,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    SingleChoiceBottomSheet(
        title = title,
        description = subtitle,
        options = options.map { option ->
            SingleChoiceOption(
                value = option.value,
                label = option.title,
                // An unavailable option says why underneath, below its own description.
                supportingText = listOfNotNull(
                    option.description?.takeIf(String::isNotBlank),
                    option.unavailableReason?.takeIf(String::isNotBlank),
                ).joinToString("\n").takeIf(String::isNotBlank),
                enabled = option.enabled,
            )
        },
        isSelected = { it == selectedValue },
        onSelected = onSelected,
        onDismiss = onDismiss,
    )
}
