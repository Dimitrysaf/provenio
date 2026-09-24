package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

data class DropdownOption(
    val key: String,
    val label: String,
)

// A filter chip with a dropdown arrow; its options open in the app's single-choice sheet.
@Composable
fun DropdownChip(
    title: String,
    label: String,
    selectedKey: String?,
    options: List<DropdownOption>,
    enabled: Boolean = true,
    onSelected: (DropdownOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isSheetVisible by remember { mutableStateOf(false) }

    FilterChip(
        selected = false,
        onClick = { isSheetVisible = true },
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        enabled = enabled,
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )

    if (isSheetVisible) {
        SingleChoiceBottomSheet(
            title = title,
            options = options.map { SingleChoiceOption(value = it, label = it.label) },
            isSelected = { it.key == selectedKey },
            onSelected = onSelected,
            onDismiss = { isSheetVisible = false },
        )
    }
}
