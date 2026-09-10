package io.github.dimitrysaf.provenio.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * A chip that opens a sheet of options and reports the one picked.
 *
 * Each chip owns exactly one piece of state — whether its own sheet is open — so a row of
 * them needs no coordination between them.
 */
@Composable
fun <T> PickerChip(
    sheetTitle: String,
    label: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onPick: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    SuggestionChip(
        onClick = { open = true },
        enabled = options.isNotEmpty(),
        label = { Text(label) },
        icon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
    )

    if (open) {
        OptionSheet(
            title = sheetTitle,
            options = options,
            selected = selected,
            onPick = onPick,
            onDismiss = { open = false },
        )
    }
}
