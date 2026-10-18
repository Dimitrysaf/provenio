package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.animation.AnimatedVisibility
import io.github.dimitrysaf.provenio.shell.components.FindableText
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
/**
 * Whether rows show their plain leading icons. Off, as Settings pages are; the category list's
 * coloured icons show regardless. Screens outside Settings that keep their icons turn it on.
 */
internal val LocalSettingsRowIcons = staticCompositionLocalOf { false }

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
    // Corners follow the rows on screen, so a collapsed group's rows are not counted. Its rows
    // take the places they have when it is open, since that is the only time they are seen.
    val visibleCount = scope.rows.sumOf { it.visibleRowCount }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        var index = 0
        scope.rows.forEach { row ->
            if (row is SettingsListRow.Expandable) {
                val openCount = if (row.expanded) visibleCount else visibleCount + row.rows.size
                val firstIndex = index
                // The spec's own expansion: the rows grow in under the one that reveals them and
                // shrink back into it, on the expressive spatial motion.
                AnimatedVisibility(
                    visible = row.expanded,
                    enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                    exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                        fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace)) {
                        row.rows.forEachIndexed { childIndex, child ->
                            child.RenderSegment(index = firstIndex + childIndex, count = openCount)
                        }
                    }
                }
                if (row.expanded) index += row.rows.size
            } else {
                row.RenderSegment(index = index, count = visibleCount)
                index += 1
            }
        }
    }
}

/**
 * Every corner that faces the outside of the group is an outer corner; only the ones facing a
 * neighbour are inner. A lone row therefore has four outer corners.
 *
 * The same shape in every state. The spec's shape morph is defined for the *selected* state, and
 * these rows are single-action so they never have one — left to Compose's defaults, pressing or
 * focusing a row would re-round it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsListRow.RenderSegment(index: Int, count: Int) {
    val shape = segmentShape(index = index, count = count)
    Render(
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
        progress: Float? = null,
        /** The row opens a page of its own, which a chevron at its end says. */
        opensPage: Boolean = false,
        /** Colours the icon's circle in its own hue instead of the theme's. */
        iconHue: SettingsIconHue? = null,
        onClick: () -> Unit,
    ) {
        rows += SettingsListRow.Navigation(
            title = title,
            description = description,
            icon = icon,
            iconPainter = iconPainter,
            iconHue = iconHue,
            enabled = enabled,
            leadingContent = leadingContent,
            trailingContent = trailingContent ?: if (opensPage) PageChevron else null,
            progress = progress,
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
     * Rows that only apply while [expanded], such as the options of a switch above them. They
     * stay declared either way and animate in and out of the group, rather than vanishing.
     *
     * [expanded] has to be read outside the list's builder: a state read in the builder re-runs
     * only the builder, and the list would never re-render.
     */
    inline fun expandableRows(expanded: Boolean, declare: SettingsListScope.() -> Unit) {
        val start = rows.size
        declare()
        val children = rows.subList(start, rows.size).toList()
        repeat(children.size) { rows.removeAt(rows.lastIndex) }
        if (children.isNotEmpty()) rows += SettingsListRow.Expandable(expanded, children)
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
        private val iconHue: SettingsIconHue? = null,
        private val enabled: Boolean,
        /** Anything the icon slots cannot express, a remote image for one. Wins over both. */
        private val leadingContent: (@Composable () -> Unit)?,
        private val trailingContent: (@Composable () -> Unit)?,
        private val progress: Float? = null,
        private val onClick: () -> Unit,
    ) : SettingsListRow {
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) {
            val fill by animateFloatAsState(
                targetValue = (progress ?: 0f).coerceIn(0f, 1f),
                label = "settings_row_progress",
            )
            val containerColor = MaterialTheme.colorScheme.surfaceContainer
            val fillColor = MaterialTheme.colorScheme.primaryContainer
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                enabled = enabled,
                modifier = if (progress != null) {
                    Modifier
                        .clip(shape)
                        .drawBehind {
                            drawRect(containerColor)
                            drawRect(fillColor, size = Size(size.width * fill, size.height))
                        }
                } else {
                    Modifier
                },
                colors = if (progress != null) {
                    ListItemDefaults.segmentedColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                    )
                } else {
                    settingsListItemColors
                },
                // Centred at any height: by default a tall row pins its controls to the top.
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = leadingContent ?: leadingIcon(icon, iconPainter, iconHue, LocalSettingsRowIcons.current),
                supportingContent = description?.takeIf { it.isNotBlank() }?.let { { FindableText(it) } },
                trailingContent = trailingContent,
            ) {
                FindableText(title)
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
                // Centred at any height: by default a tall row pins its controls to the top.
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = leadingIcon(icon, null, showPlainIcons = LocalSettingsRowIcons.current),
                supportingContent = description?.takeIf { it.isNotBlank() }?.let { { FindableText(it) } },
                trailingContent = {
                    Switch(
                        checked = isChecked,
                        onCheckedChange = { onCheckedChange(it) },
                        enabled = enabled,
                    )
                },
            ) {
                FindableText(title)
            }
        }
    }

    /** How many rows this entry puts on screen. */
    val visibleRowCount: Int get() = 1

    /** Rows declared by [SettingsListScope.expandableRows]; [SettingsList] lays them out itself. */
    class Expandable(
        val expanded: Boolean,
        val rows: List<SettingsListRow>,
    ) : SettingsListRow {
        override val visibleRowCount: Int get() = if (expanded) rows.size else 0

        @Composable
        override fun Render(shape: RoundedCornerShape, shapes: ListItemShapes) =
            error("An expandable group is laid out by SettingsList, not rendered as one row")
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
                FindableText(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                description?.takeIf { it.isNotBlank() }?.let {
                    FindableText(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            value?.takeIf { it.isNotBlank() }?.let {
                FindableText(
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
    hue: SettingsIconHue? = null,
    showPlainIcons: Boolean = false,
): (@Composable () -> Unit)? {
    if (icon == null && iconPainter == null) return null
    if (hue != null && icon != null && iconPainter == null) {
        return { SettingsIconCircle(icon = icon, hue = hue) }
    }
    // Inside Settings only the category list carries icons; a page's rows are text alone.
    if (!showPlainIcons) return null
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

/** Marks a row that opens a page of its own. */
private val PageChevron: @Composable () -> Unit = {
    Icon(
        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A hue for an icon's circle, Pixel Settings style: a pastel circle with a deep icon of the same
 * hue, lighter in the light theme and brighter in the dark one.
 */
internal enum class SettingsIconHue(
    private val darkContainer: Long,
    private val darkContent: Long,
    private val lightContainer: Long,
    private val lightContent: Long,
) {
    Blue(0xFFA8C7FA, 0xFF062E6F, 0xFFD3E3FD, 0xFF041E49),
    Purple(0xFFD7BAFF, 0xFF3F1F70, 0xFFEADDFF, 0xFF21005D),
    Teal(0xFF80D5D0, 0xFF003735, 0xFFB2EFEA, 0xFF00201E),
    Pink(0xFFFFB0CB, 0xFF5E1133, 0xFFFFD9E3, 0xFF3E001D),
    Red(0xFFFFB4AB, 0xFF690005, 0xFFFFDAD6, 0xFF410002),
    Orange(0xFFFFB68B, 0xFF542200, 0xFFFFDBC8, 0xFF331200),
    Indigo(0xFFBAC3FF, 0xFF1A2678, 0xFFDEE0FF, 0xFF00105C),
    Green(0xFF8CD69A, 0xFF00391A, 0xFFC4EFC9, 0xFF00210B),
    Yellow(0xFFE9C349, 0xFF3C2F00, 0xFFFFE08B, 0xFF241A00),
    Brown(0xFFE7BDB0, 0xFF442A21, 0xFFFFDBD0, 0xFF2C160D),
    Grey(0xFFC4C7C5, 0xFF2D3130, 0xFFE1E3E1, 0xFF191C1B),
    Cyan(0xFF7DD3F0, 0xFF003545, 0xFFBEE9FF, 0xFF001F2A),
    Lime(0xFFB5D07A, 0xFF253600, 0xFFD7EFA0, 0xFF141F00),
    ;

    /** The circle's colour and the icon's, for the theme on screen. */
    @Composable
    fun colors(): Pair<Color, Color> {
        val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        return if (dark) {
            Color(darkContainer) to Color(darkContent)
        } else {
            Color(lightContainer) to Color(lightContent)
        }
    }
}

/** An icon in a circle of its own hue, the leading element of a category row. */
@Composable
internal fun SettingsIconCircle(
    icon: ImageVector,
    hue: SettingsIconHue,
    modifier: Modifier = Modifier,
) {
    val (container, content) = hue.colors()
    Box(
        modifier = modifier
            .size(LeadingIconContainerSize)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(24.dp),
        )
    }
}
