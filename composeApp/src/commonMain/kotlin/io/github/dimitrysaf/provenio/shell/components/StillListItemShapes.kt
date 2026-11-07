package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape

/** [shape] as a plain [Shape], so Material does not take it for one whose corners it can morph. */
@Immutable
private data class StillShape(val shape: Shape) : Shape by shape

/**
 * [ListItemDefaults.shapes] with the corners held still. Material morphs a list item's corners
 * whenever every one of its shapes is corner-based, and the morph's outline reads its progress
 * where the drawn row never hears of it, which can leave segmented rows undrawn until a scroll
 * redraws them. One shape that is not corner-based (the dragged one, as no row here is dragged)
 * turns the morph off, so each row draws its own shape at once.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun stillListItemShapes(
    shape: Shape? = null,
    selectedShape: Shape? = null,
    pressedShape: Shape? = null,
    focusedShape: Shape? = null,
    hoveredShape: Shape? = null,
    draggedShape: Shape? = null,
): ListItemShapes {
    val shapes = ListItemDefaults.shapes(
        shape = shape,
        selectedShape = selectedShape,
        pressedShape = pressedShape,
        focusedShape = focusedShape,
        hoveredShape = hoveredShape,
        draggedShape = draggedShape,
    )
    return shapes.copy(draggedShape = StillShape(shapes.draggedShape))
}
