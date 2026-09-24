package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.cd_selected
import org.jetbrains.compose.resources.stringResource

/** One choice in a [SingleChoiceBottomSheet] or a [MultiChoiceBottomSheet]. */
class SingleChoiceOption<T>(
    val value: T,
    val label: String,
    val supportingText: String? = null,
    val enabled: Boolean = true,
    val leadingContent: @Composable (() -> Unit)? = null,
)

/**
 * Pick one of a set of options, in a modal bottom sheet.
 *
 * This is the app's single-select picker. A list of choices is not a prompt that interrupts a
 * flow, so it is a sheet rather than a dialog; picking one applies it and closes the sheet, and
 * there are no confirm or cancel buttons to press.
 *
 * Selection is shown by [SelectableListRow], which carries the expressive list spec's treatment
 * for a chosen row, so the rows need no dividers or containers of their own.
 *
 * m3.material.io/components/bottom-sheets/specs, m3.material.io/components/lists/specs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SingleChoiceBottomSheet(
    title: String,
    options: List<SingleChoiceOption<T>>,
    isSelected: (T) -> Boolean,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
    footnote: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalSheet(
        onDismissRequest = {
            scope.launch {
                dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                // The sheet's body margin, so a selected row's container is inset from the sheet's
                // edges instead of running into them.
                .padding(horizontal = BottomSheetBodyMargin)
                .padding(bottom = BottomSheetBodyMargin),
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = BottomSheetBodyMargin)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = BottomSheetBodyMargin),
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            itemsIndexed(options) { _, option ->
                val selected = isSelected(option.value)
                SelectableListRow(
                    selected = selected,
                    onClick = {
                        onSelected(option.value)
                        scope.launch {
                            dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                    headline = option.label,
                    enabled = option.enabled,
                    supporting = option.supportingText,
                    leadingContent = option.leadingContent,
                    trailingContent = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.cd_selected),
                            )
                        }
                    },
                )
            }

            if (footnote != null) {
                item {
                    Text(
                        text = footnote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = BottomSheetBodyMargin, start = 16.dp, end = 16.dp),
                    )
                }
            }
        }
    }
}
