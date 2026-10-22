package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Round choices in an even grid: as many columns as cells of [cell] fit, each column the same
 * width, so the rows line up and the spare width is shared out rather than left at the end.
 */
@Composable
internal fun <T> SwatchGrid(
    items: List<T>,
    cell: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    item: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (cell + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                ) {
                    row.forEach { entry ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { item(entry) }
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}
