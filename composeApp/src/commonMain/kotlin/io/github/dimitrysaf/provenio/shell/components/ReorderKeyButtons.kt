package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.action_move_down
import provenio.composeapp.generated.resources.action_move_up

/**
 * Move up and move down, for a list whose rows are otherwise reordered by dragging: a remote or a
 * keyboard cannot drag. They show only while keys are in use, and touch or a mouse drags as before.
 */
@Composable
internal fun ReorderKeyButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!openedWithKeys()) return
    Row(modifier = modifier) {
        val moveUp = stringResource(Res.string.action_move_up)
        WithTooltip(moveUp) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = moveUp)
            }
        }
        val moveDown = stringResource(Res.string.action_move_down)
        WithTooltip(moveDown) {
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = moveDown)
            }
        }
    }
}
