package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A row of a segmented list that opens the rows under it, such as a language over its subtitles.
 * It sits a tone above those rows, and its chevron turns as it opens. [count] stands beside the
 * chevron when there is one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableListHeader(
    title: String,
    expanded: Boolean,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "expandable_list_chevron")
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (count != null) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(chevronRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
