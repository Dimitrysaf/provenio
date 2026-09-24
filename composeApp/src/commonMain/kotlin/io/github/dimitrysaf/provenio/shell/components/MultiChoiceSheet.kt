package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.cd_selected
import org.jetbrains.compose.resources.stringResource

/**
 * Pick any number of a set of options, in a modal bottom sheet.
 *
 * The multi-select counterpart of [SingleChoiceBottomSheet], and the same sheet in every other
 * respect. Picking cannot close it, since there is more to pick, so the choice is reported when
 * the sheet is dismissed.
 *
 * Rows are [SelectableListRow] with a checkbox trailing them, as the catalog picker does: the box
 * takes no click handler of its own because the row already owns the tap.
 *
 * [allLabel], when given, heads the list with a row meaning no restriction at all, which is what
 * an empty selection stores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> MultiChoiceBottomSheet(
    title: String,
    options: List<SingleChoiceOption<T>>,
    selected: Set<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
    allLabel: String? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val values = remember(options) { options.map { it.value }.toSet() }
    var draft by remember(selected, values) { mutableStateOf(selected.intersect(values)) }

    ModalSheet(
        onDismissRequest = {
            onSelectionChanged(draft)
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

            if (allLabel != null) {
                item {
                    val allSelected = draft.isEmpty()
                    SelectableListRow(
                        selected = allSelected,
                        onClick = { draft = emptySet() },
                        headline = allLabel,
                        trailingContent = {
                            if (allSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(Res.string.cd_selected),
                                )
                            }
                        },
                    )
                }
            }

            itemsIndexed(options) { _, option ->
                val isChecked = option.value in draft
                SelectableListRow(
                    selected = isChecked,
                    onClick = {
                        draft = if (isChecked) draft - option.value else draft + option.value
                    },
                    headline = option.label,
                    enabled = option.enabled,
                    supporting = option.supportingText,
                    leadingContent = option.leadingContent,
                    trailingContent = {
                        // Null handler: the row owns the click, so the box is an indicator rather
                        // than a second target inside it.
                        Checkbox(checked = isChecked, onCheckedChange = null)
                    },
                )
            }
        }
    }
}
