package com.nuvio.app.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * A settings list, built from expressive segmented list items.
 *
 * Baseline lists are no longer recommended, so each row is its own container: the first and last
 * take the group's outer corner and the ones between take the small inner corner, which
 * [ListItemDefaults.segmentedShapes] derives from an item's index. Segmentation is what separates
 * rows, so there are no dividers between them.
 *
 * Rows are declared rather than composed directly, so the list knows how many there are before it
 * draws any of them — which is what lets conditional rows still get the right corners.
 *
 * m3.material.io/components/lists/specs
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@NonRestartableComposable
internal fun SettingsList(
    modifier: Modifier = Modifier,
    content: @Composable SettingsListScope.() -> Unit,
) {
    // Rows are collected during composition, then rendered below, so the collect and the render
    // have to happen in the same pass.
    //
    // Compose keeps a composable lambda's identity stable across recompositions, so `content`
    // looks unchanged to this function and it would be skipped — while the lambda's own scope
    // re-runs and appends to `rows`. The render loop would then never see the new rows.
    // @NonRestartableComposable removes this function's restart scope so its body is inlined
    // into the caller's: when the caller recomposes, the clear, the collect and the render all
    // run together.
    val scope = remember { SettingsListScope() }
    scope.rows.clear()
    scope.content()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        scope.rows.forEachIndexed { index, row ->
            // Every corner that faces the outside of the group is an outer corner; only the ones
            // facing a neighbour are inner. A lone row therefore has four outer corners.
            val shape = segmentShape(index = index, count = scope.rows.size)
            // The same shape in every state. The spec's shape morph is defined for the *selected*
            // state, and these rows are single-action so they never have one — left to Compose's
            // defaults, pressing or focusing a row would re-round it.
            row.Render(
                shape = shape,
                shapes = ListItemDefaults.shapes(
                    shape = shape,
                    selectedShape = shape,
                    pressedShape = shape,
                    focusedShape = shape,
                    hoveredShape = shape,
                ),
            )
        }
    }
}

/**
 * The spec publishes no gap between segmented list items — only that unselected items have a 4dp
 * inner corner, which implies some separation. This borrows the 2dp that connected button groups
 * do publish, as the smallest value that lets those inner corners read.
 */
internal val ListItemBetweenSpace = 2.dp

/** `ListTokens.ItemContainerExpressiveShape` — the corner facing a neighbouring row. */
internal val InnerCorner = 4.dp

/** `ListTokens.ContainerShape` — the corner facing the outside of the group. */
internal val OuterCorner = 16.dp

internal fun segmentShape(index: Int, count: Int): RoundedCornerShape = when {
    count == 1 -> RoundedCornerShape(OuterCorner)
    index == 0 -> RoundedCornerShape(
        topStart = OuterCorner,
        topEnd = OuterCorner,
        bottomStart = InnerCorner,
        bottomEnd = InnerCorner,
    )
    index == count - 1 -> RoundedCornerShape(
        topStart = InnerCorner,
        topEnd = InnerCorner,
        bottomStart = OuterCorner,
        bottomEnd = OuterCorner,
    )
    else -> RoundedCornerShape(InnerCorner)
}

/**
 * `ListTokens.ItemSegmentedContainerColor` is `surface`, which assumes the page sits on a lower
 * layer. This app puts the page itself on `surface`, so the rows take the next container tone up
 * to read as the raised layer they are.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val settingsListItemColors: ListItemColors
    @Composable
    get() = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

/** Size of the optional leading icon's container. */
private val LeadingIconContainerSize = 40.dp

/**
 * Where a list's rows are declared.
 *
 * A `@Composable` helper that declares rows for a caller has to be [NonRestartableComposable]
 * too. Strong skipping compares this scope by identity, and it is remembered, so a helper that
 * takes it would be skipped on the caller's next recomposition — after the clear, before the
 * render. Its rows would simply not be there, and the group would vanish on the first state
 * change. Without a restart scope there is nothing to skip, and the rows are declared every pass.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal class SettingsListScope internal constructor() {

    internal val rows = mutableListOf<SettingsListRow>()

    /** A row that opens something. */
    fun navigationRow(
        title: String,
        description: String? = null,
        icon: ImageVector? = null,
        iconPainter: Painter? = null,
        enabled: Boolean = true,
        leadingContent: (@Composable () -> Unit)? = null,
        trailingContent: (@Composable () -> Unit)? = null,
        onClick: () -> Unit,
    ) {
        rows += SettingsListRow.Navigation(
            title = title,
            description = description,
            icon = icon,
            iconPainter = iconPainter,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent,
            onClick = onClick,
        )
    }

    /**
     * A row that toggles something; the switch is the row's trailing element.
     *
     * [checked] is a lambda, not a value, so the state read happens inside the row's own
     * composition. Read here in the builder instead, it would register against the builder
     * lambda's scope, and the rendered Switch would never see the change.
     */
    fun switchRow(
        title: String,
        description: String? = null,
        checked: () -> Boolean,
        enabled: Boolean = true,
        icon: ImageVector? = null,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        rows += SettingsListRow.Toggle(
            title = title,
            description = description,
            icon = icon,
            enabled = enabled,
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }

    /** Anything that isn't a standard row, still segmented with its neighbours. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun customRow(content: @Composable (ListItemShapes) -> Unit) {
        rows += SettingsListRow.Custom(content)
    }

    /**
     * A row that paints its own container, for a control that needs the row's full width rather
     * than a list item's trailing slot — a slider, for one.
     */
    fun shapedRow(content: @Composable (RoundedCornerShape) -> Unit) {
        rows += SettingsListRow.Shaped(content)
    }

    /**
     * A row that only states something. It paints its own container because a list item's slots
     * are arranged around an action, and this row has none: nothing here is pressable.
     */
    fun infoRow(
        title: String,
        description: String? = null,
        value: String? = null,
    ) = shapedRow { shape ->
        SettingsInfoRowContent(
            shape = shape,
            title = title,
            description = description,
            value = value,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal sealed interface SettingsListRow {

    /**
     * [shape] is the same corners [shapes] is built from, for a row that paints its own container
     * because its control does not fit a list item's slots.
     */
    @Composable
    fun Render(shape: RoundedCornerShape, shapes: ListItemShapes)

    class Navigation(
        private val title: String,
        private val description: String?,
        private val icon: ImageVector?,
        private val iconPainter: Painter?,
        private val enabled: Boolean,
        /** Anything the icon slots cannot express, a remote image for one. Wins over both. */
        private val leadingContent: (@Composable () -> Unit)?,
        private val trailingContent: (@Composable () -> Unit)?,
        private val onClick: () -> Unit,
    ) : SettingsListRow {
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) {
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                enabled = enabled,
                colors = settingsListItemColors,
                leadingContent = leadingContent ?: leadingIcon(icon, iconPainter),
                supportingContent = description?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                trailingContent = trailingContent,
            ) {
                Text(title)
            }
        }
    }

    class Toggle(
        private val title: String,
        private val description: String?,
        private val icon: ImageVector?,
        private val enabled: Boolean,
        private val checked: () -> Boolean,
        private val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsListRow {
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) {
            val isChecked = checked()
            SegmentedListItem(
                // Tapping anywhere on the row flips the switch, which keeps the row to the
                // spec's one selection interaction per item.
                onClick = { if (enabled) onCheckedChange(!isChecked) },
                shapes = shapes,
                enabled = enabled,
                colors = settingsListItemColors,
                leadingContent = leadingIcon(icon, null),
                supportingContent = description?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
                trailingContent = {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { onCheckedChange(it) },
                        enabled = enabled,
                    )
                },
            ) {
                Text(title)
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    class Custom(
        private val content: @Composable (ListItemShapes) -> Unit,
    ) : SettingsListRow {
        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) = content(shapes)
    }

    /** A row that draws its own container, given the corners its place in the group calls for. */
    class Shaped(
        private val content: @Composable (RoundedCornerShape) -> Unit,
    ) : SettingsListRow {
        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) = content(shape)
    }
}

/** A list item's one-line height, so a stated row lines up with the rows around it. */
private val InfoRowMinHeight = 56.dp

/** `ListItemDefaults` keeps its content insets private, so a self-drawn row repeats them. */
private val InfoRowHorizontalPadding = 16.dp
private val InfoRowVerticalPadding = 12.dp

@Composable
private fun SettingsInfoRowContent(
    shape: RoundedCornerShape,
    title: String,
    description: String?,
    value: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = InfoRowMinHeight)
                .padding(
                    horizontal = InfoRowHorizontalPadding,
                    vertical = InfoRowVerticalPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            value?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The leading slot. Rows that have no icon get no container — an empty tile would just be an
 * indent pretending to be an element.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun leadingIcon(
    icon: ImageVector?,
    iconPainter: Painter?,
): (@Composable () -> Unit)? {
    if (icon == null && iconPainter == null) return null
    return {
        Box(
            modifier = Modifier
                .size(LeadingIconContainerSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
