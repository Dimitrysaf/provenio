package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * The row that makes a new thing, sitting at the top of the list it adds to.
 *
 * It is a row of the same segmented group rather than a button floating above it, so the list
 * reads as one object: the first row makes one, the rest are the ones that already exist. It never
 * moves, so it is not a reorderable item and the rows below it start at index one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewEntryRow(
    text: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.shapes(
            shape = shape,
            selectedShape = shape,
            pressedShape = shape,
            focusedShape = shape,
            hoveredShape = shape,
        ),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        leadingContent = {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
        },
    ) {
        Text(text)
    }
}
