package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * What can be done with a title, from a long press on it.
 *
 * A sheet, because a list of actions is a list: each one is a row with its icon and its name,
 * which is how the rest of the app asks someone to pick something. What it is about is stated
 * once at the top, as a thumbnail and two lines — enough to be sure it is the right title,
 * without rebuilding the poster at half the screen's height.
 *
 * Every long press in the app arrives here — a poster, a continue-watching card — so they are one
 * sheet rather than several that drift apart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaActionsSheet(
    imageUrl: String?,
    title: String,
    subtitle: String?,
    actions: List<MediaSheetAction>,
    onDismiss: () -> Unit,
    landscapeThumbnail: Boolean = false,
    blurThumbnail: Boolean = false,
) {
    // On a large window the actions open as a menu where the press was, rather than as a sheet.
    if (!usesNativeBottomSheet && LocalWindowBreakpoint.current.isTwoPane) {
        MediaActionsMenu(title = title, actions = actions, onDismiss = onDismiss)
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // The action runs from `invokeOnCompletion`, not from inside the coroutine. Dismissing takes
    // the sheet out of the composition that owns this scope, so anything queued after the hide
    // was being cancelled with it: taps did nothing at all.
    fun close(then: (() -> Unit)? = null) {
        scope
            .launch { dismissBottomSheet(sheetState = sheetState, onDismiss = onDismiss) }
            .invokeOnCompletion { then?.invoke() }
    }

    val thumbnailWidth = if (landscapeThumbnail) LandscapeThumbnailWidth else ThumbnailWidth
    val thumbnailHeight = if (landscapeThumbnail) LandscapeThumbnailHeight else ThumbnailHeight

    ModalSheet(
        onDismissRequest = { close() },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = BottomSheetBodyMargin)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BottomSheetBodyMargin)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(thumbnailWidth)
                        .height(thumbnailHeight)
                        .clip(ShapeDefaults.Small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = thumbnailWidth, height = thumbnailHeight)
                                .then(if (blurThumbnail) Modifier.blur(ThumbnailBlur) else Modifier),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider()

            actions.forEach { action ->
                val contentColor = if (action.isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                ListItem(
                    headlineContent = { Text(action.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { close(action.onSelected) },
                    leadingContent = {
                        Icon(imageVector = action.icon, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                        headlineColor = contentColor,
                        leadingIconColor = contentColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MediaActionsMenu(
    title: String,
    actions: List<MediaSheetAction>,
    onDismiss: () -> Unit,
) {
    val press = remember { LastPressPosition.recent() }
    Popup(
        popupPositionProvider = remember(press) { PressPositionProvider(press) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MenuDefaults.shape,
            color = MenuDefaults.containerColor,
            tonalElevation = MenuDefaults.TonalElevation,
            shadowElevation = MenuDefaults.ShadowElevation,
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 200.dp, max = 320.dp)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                actions.forEach { action ->
                    val color = if (action.isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    DropdownMenuItem(
                        text = { Text(action.label) },
                        leadingIcon = { Icon(imageVector = action.icon, contentDescription = null) },
                        colors = MenuDefaults.itemColors(textColor = color, leadingIconColor = color),
                        onClick = {
                            onDismiss()
                            action.onSelected()
                        },
                    )
                }
            }
        }
    }
}

// Opens the menu at the press, flipped or nudged so it always fits the window; without a recent press it opens where the caller sits.
private class PressPositionProvider(private val press: Offset?) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = press?.x?.roundToInt() ?: anchorBounds.center.x
        val y = press?.y?.roundToInt() ?: anchorBounds.center.y
        val left = if (x + popupContentSize.width > windowSize.width) x - popupContentSize.width else x
        val top = if (y + popupContentSize.height > windowSize.height) y - popupContentSize.height else y
        return IntOffset(
            x = left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y = top.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

/** One thing that can be done with the title the sheet is about. */
class MediaSheetAction(
    val icon: ImageVector,
    val label: String,
    val isDestructive: Boolean = false,
    val onSelected: () -> Unit,
)

private val ThumbnailWidth = 48.dp

private val ThumbnailHeight = 72.dp

/** An episode still is 16:9, so it gets its own frame rather than a crop of its middle. */
private val LandscapeThumbnailWidth = 96.dp

private val LandscapeThumbnailHeight = 54.dp

/** Enough to keep an unwatched episode's still from spoiling it. */
private val ThumbnailBlur = 12.dp
