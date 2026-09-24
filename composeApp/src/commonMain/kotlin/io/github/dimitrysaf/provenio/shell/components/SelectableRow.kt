package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The corner a row shows when it is not the chosen one: the spec's 4dp inner corner. */
val ChoiceUnselectedCorner = 4.dp

/** The corner the chosen row morphs to: the spec's full container corner. */
val ChoiceSelectedCorner = 16.dp

/**
 * A row that can be chosen, and shows it the way the expressive list spec says to.
 *
 * Selection here is not a tick tucked at the end of an otherwise unchanged row: the chosen row
 * takes the primary container pair and its corners morph from 4dp to 16dp. That morph is the
 * whole of how the spec signals a selected list item, and it is why these rows need no dividers
 * or containers of their own.
 *
 * The morph belongs to selection alone. Pressing, focusing and hovering keep the unselected
 * radius, because otherwise a row would round itself just for being touched and read as chosen.
 *
 * `unselectedShape` is what the row shows when it is not chosen, so a row inside a segmented
 * group passes its place in that group and still morphs to the full corner when picked.
 *
 * m3.material.io/components/lists/specs
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectableListRow(
    selected: Boolean,
    onClick: () -> Unit,
    headline: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supporting: String? = null,
    unselectedShape: RoundedCornerShape = RoundedCornerShape(ChoiceUnselectedCorner),
    containerColor: Color = Color.Transparent,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ListItem(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leadingContent,
        supportingContent = supporting?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedSupportingContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shapes = ListItemDefaults.shapes(
            shape = unselectedShape,
            selectedShape = RoundedCornerShape(ChoiceSelectedCorner),
            pressedShape = unselectedShape,
            focusedShape = unselectedShape,
            hoveredShape = unselectedShape,
        ),
    ) {
        Text(text = headline)
    }
}
