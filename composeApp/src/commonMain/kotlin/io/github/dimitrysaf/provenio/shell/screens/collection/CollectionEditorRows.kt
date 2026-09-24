package io.github.dimitrysaf.provenio.shell.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.shell.components.ListSubheader
import io.github.dimitrysaf.provenio.shell.components.safeBottomPadding
import io.github.dimitrysaf.provenio.shell.screens.settings.ListItemBetweenSpace
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import io.github.dimitrysaf.provenio.shell.screens.settings.segmentShape
import provenio.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PickerOptionRow(
    title: String,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    // A real list item rather than a tinted box: these rows are a list of things to pick, so they
    // take the group's container, its corners and its own press handling.
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
        supportingContent = subtitle?.takeIf { it.isNotBlank() }?.let {
            { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        },
    ) {
        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PickerSectionLabel(text: String) {
    // No uppercasing here: how a heading is capitalised is its translation's business.
    ListSubheader(
        text = text,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

/**
 * A segmented group in the editor, built the way the settings lists are.
 *
 * Rows are declared rather than composed directly, so the group knows how many there are before
 * it draws any of them, which is what lets a row that only appears sometimes still take the right
 * corner. Everything the group holds is a row: fields and empty states included, since they sit
 * inside the group's container rather than beside it.
 *
 * `@NonRestartableComposable` for the reason [io.github.dimitrysaf.provenio.shell.screens.settings.SettingsList] gives:
 * the collect and the render have to happen in the same pass.
 */
@Composable
@NonRestartableComposable
internal fun CollectionEditorGroup(
    modifier: Modifier = Modifier,
    content: @Composable CollectionEditorGroupScope.() -> Unit,
) {
    val scope = remember { CollectionEditorGroupScope() }
    scope.rows.clear()
    scope.content()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemBetweenSpace),
    ) {
        scope.rows.forEachIndexed { index, row ->
            row(segmentShape(index = index, count = scope.rows.size))
        }
    }
}

internal class CollectionEditorGroupScope {
    val rows = mutableListOf<@Composable (RoundedCornerShape) -> Unit>()

    /** Declares one row; it is handed the corner its position in the group gives it. */
    fun row(content: @Composable (RoundedCornerShape) -> Unit) {
        rows += content
    }
}

/**
 * A setting the editor picks one value for. The row states the current value and opens the app's
 * single-choice sheet, which is how every other picker in the app works.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CollectionEditorChoiceRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(OuterCorner),
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
        supportingContent = { Text(value) },
    ) {
        Text(title)
    }
}

/**
 * A setting the editor toggles. The expressive list item itself, so it carries the segmented
 * container, and tapping anywhere on the row flips the switch, which keeps the row to the spec's
 * one interaction per item. Each is its own group here, since the settings it toggles are
 * separated by the choice between them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CollectionEditorSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape = RoundedCornerShape(OuterCorner),
) {
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
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
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    ) {
        Text(title)
    }
}

/**
 * The editor's docked actions. These pages are forms that run past the fold, so the action that
 * commits them sits in the screen's bottom bar rather than in the scrolling content, where it
 * would be reachable only from the end of the list.
 *
 * It is the screen's bottom bar rather than a surface overlaid on top of it: the scaffold then
 * measures the content's bottom padding from the bar, instead of the fixed spacer these pages used
 * to reserve, which was right only while the bar happened to be that tall. The bar is opaque for
 * the same reason the profile editor's is, so content scrolls out of sight beneath it, and it owns
 * the navigation bar inset as the bottom-most element.
 */
@Composable
internal fun CollectionEditorActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = safeBottomPadding(12.dp),
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
