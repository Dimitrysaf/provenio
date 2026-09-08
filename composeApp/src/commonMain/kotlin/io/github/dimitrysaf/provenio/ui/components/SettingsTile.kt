package io.github.dimitrysaf.provenio.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink

/** Where a tile sits in its group, which is what decides its corners. */
enum class TilePosition { Single, First, Middle, Last }

fun tilePositionOf(index: Int, count: Int): TilePosition = when {
    count <= 1 -> TilePosition.Single
    index == 0 -> TilePosition.First
    index == count - 1 -> TilePosition.Last
    else -> TilePosition.Middle
}

/** Gap between tiles in a group — enough to read as separate rows, not as separate cards. */
val SettingsTileSpacing = 2.dp

private val OuterCorner = 28.dp
private val InnerCorner = 4.dp

/**
 * A group of tiles is shaped as one block: only the corners on the outside of the group
 * are round, the ones facing a neighbour stay square. A tile on its own is round all over.
 *
 * Interacting with a tile morphs it to fully rounded, which is what separates it visually
 * from the block while it is being touched.
 */
@Composable
fun settingsTileShapes(position: TilePosition): ListItemShapes {
    val shape = when (position) {
        TilePosition.Single -> RoundedCornerShape(OuterCorner)
        TilePosition.First -> RoundedCornerShape(
            topStart = OuterCorner,
            topEnd = OuterCorner,
            bottomStart = InnerCorner,
            bottomEnd = InnerCorner,
        )
        TilePosition.Middle -> RoundedCornerShape(InnerCorner)
        TilePosition.Last -> RoundedCornerShape(
            topStart = InnerCorner,
            topEnd = InnerCorner,
            bottomStart = OuterCorner,
            bottomEnd = OuterCorner,
        )
    }
    val morphed = RoundedCornerShape(OuterCorner)
    return ListItemDefaults.shapes(
        shape = shape,
        pressedShape = morphed,
        focusedShape = morphed,
        hoveredShape = morphed,
    )
}

/**
 * The one settings row in the app. Every settings surface goes through this so they cannot
 * drift apart.
 */
@Composable
fun SettingsTile(
    title: @Composable () -> Unit,
    position: TilePosition,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    SettingsMenuLink(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        action = action,
        shapes = settingsTileShapes(position),
        onClick = onClick,
    )
}
