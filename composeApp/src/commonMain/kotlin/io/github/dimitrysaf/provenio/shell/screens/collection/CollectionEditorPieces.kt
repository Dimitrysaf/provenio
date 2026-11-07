package io.github.dimitrysaf.provenio.shell.screens.collection

import io.github.dimitrysaf.provenio.shell.components.WithTooltip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.shell.screens.profiles.pictureDataUri
import io.github.dimitrysaf.provenio.shell.screens.profiles.rememberPicturePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceBottomSheet
import io.github.dimitrysaf.provenio.shell.components.SingleChoiceOption
import io.github.dimitrysaf.provenio.shell.components.shapedClickable
import io.github.dimitrysaf.provenio.shell.screens.settings.OuterCorner
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * What a collection or a folder is called and what it looks like, as the head of its editor.
 *
 * These two things are what identify it, so they lead the page instead of sitting further down as
 * two more fields in the form. Both are edited through a dialog: the image by the pencil over it,
 * the name by the pencil beside it or by the name itself, which is the larger target. Collections
 * and folders share the composable so the two pages are the same page with different words.
 */
@Composable
internal fun EditableIdentityHeader(
    title: String,
    titlePlaceholder: String,
    onEditTitle: () -> Unit,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    // Null for a collection, which has no picture.
    onEditImage: (() -> Unit)? = null,
    imageAspectRatio: Float = 2f / 3f,
) {
    val shape = RoundedCornerShape(OuterCorner)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onEditImage != null) Surface(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(imageAspectRatio)
                .clip(shape)
                .shapedClickable(shape, onClick = onEditImage),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    !imageUrl.isNullOrBlank() -> AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    else -> Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // The pencil keeps its own container so it stays legible over any image.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(Res.string.action_edit),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // The name takes whatever width is left and wraps to two lines before it is cut, so a long
        // one can never push the pencil off the row.
        Text(
            text = title.ifBlank { titlePlaceholder },
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEditTitle),
            style = MaterialTheme.typography.headlineSmall,
            color = if (title.isBlank()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        WithTooltip(stringResource(Res.string.action_edit)) {
            IconButton(
                onClick = onEditTitle,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(Res.string.action_edit),
                )
            }
        }
    }
}

/**
 * Opens the device's picker and gives the picked picture as the collection keeps it: scaled down
 * so its longer side is at most [maxSide] pixels, inside the collection itself. Nothing is given
 * when the picker is closed or the picture cannot be read. Null where there is no picker.
 */
@Composable
internal fun rememberStoredPicturePicker(maxSide: Int, onPicked: (String) -> Unit): (() -> Unit)? {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)
    return rememberPicturePicker { bytes ->
        if (bytes == null) return@rememberPicturePicker
        scope.launch {
            withContext(Dispatchers.Default) { pictureDataUri(bytes, maxSide) }?.let(currentOnPicked)
        }
    }
}

// Long enough for a tile on a large screen, small enough to keep inside the collection.
internal const val FolderPictureMaxSide = 960

/** A folder's name, which is all a folder shows. */
@Composable
internal fun FolderTitleDialog(
    initialTitle: String,
    onConfirm: (title: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initialTitle) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.collections_rename_folder)) },
        text = {
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                label = {
                    Text(stringResource(Res.string.collections_editor_placeholder_folder))
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (draft.isNotBlank()) onConfirm(draft.trim())
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft.trim()) },
                enabled = draft.isNotBlank(),
            ) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
    )
}

/** One way to add something: what it is called and what it does. */
internal data class AddSourceChoice(
    val label: String,
    val onClick: () -> Unit,
)

/**
 * One button for adding, and a sheet to say which kind.
 *
 * Three buttons in a row never sat right: sized to their labels they left the group short of the
 * right margin, and forced to equal thirds the labels had to be cut down to fit a phone. Either
 * way the section spent a whole row of width on a choice that is made once and then not thought
 * about again.
 *
 * So the row is one button that spans the section, and the choice moves into the app's
 * single-choice sheet where each kind gets a full row and its own name. Nothing is selected in
 * that sheet and nothing is illustrated: the rows are actions, so picking one runs it and closes,
 * and three names need no icons to tell them apart.
 */
@Composable
internal fun AddSourceButton(
    label: String,
    sheetTitle: String,
    choices: List<AddSourceChoice>,
    modifier: Modifier = Modifier,
) {
    var showChoices by remember { mutableStateOf(false) }

    // Outlined, not filled: the page's one filled button is Save, in the docked bar, and two
    // filled buttons on a screen compete over which is the thing to press.
    OutlinedButton(
        onClick = { showChoices = true },
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(label)
    }

    if (showChoices) {
        SingleChoiceBottomSheet(
            title = sheetTitle,
            options = choices.map { SingleChoiceOption(value = it.label, label = it.label) },
            // Actions, not a setting: there is no current value for a row to be checked against.
            isSelected = { false },
            onSelected = { picked -> choices.firstOrNull { it.label == picked }?.onClick?.invoke() },
            onDismiss = { showChoices = false },
        )
    }
}

/**
 * The modes of a builder, as tabs.
 *
 * There are eight of them and they are exclusive, which is a tab bar's job rather than a row of
 * filter chips: chips are for narrowing a set, and eight of them wrapped into three ragged lines.
 * Scrollable rather than fixed, because eight labels do not fit a phone's width and a tab must
 * show its whole label.
 *
 * m3.material.io/components/tabs/specs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> PickerTabRow(
    tabs: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        // The default start padding indents the first tab away from everything else on the page.
        edgePadding = 0.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(tab) },
                text = { Text(label(tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

/**
 * A row of a segmented group that holds a field rather than a list item.
 *
 * A text field is still one thing the form asks for, so it belongs in the group's container with
 * the rows around it instead of floating on the page between them.
 */
@Composable
internal fun FormFieldRow(
    shape: RoundedCornerShape,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * One value the form asks for, typed or picked from a shortlist.
 *
 * The shortlist used to be a row of chips sitting above its own text field, so every one of these
 * filters said the same thing twice and took two rows to do it. Here the shortlist lives behind
 * the field's own trailing button: the field is the single place the value is shown and typed, and
 * the sheet is a shortcut into it rather than a second control beside it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PresetFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    shape: RoundedCornerShape,
    supportingText: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    /** Label to the value it writes. */
    presets: List<Pair<String, String>> = emptyList(),
    presetSheetTitle: String = label,
) {
    var showPresets by remember { mutableStateOf(false) }

    FormFieldRow(shape) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            supportingText = supportingText?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = if (presets.isEmpty()) {
                null
            } else {
                {
                    WithTooltip(presetSheetTitle) {
                        IconButton(onClick = { showPresets = true }) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = presetSheetTitle,
                            )
                        }
                    }
                }
            },
        )
    }

    if (showPresets) {
        SingleChoiceBottomSheet(
            title = presetSheetTitle,
            options = presets.map { (presetLabel, presetValue) ->
                SingleChoiceOption(value = presetValue, label = presetLabel)
            },
            isSelected = { it == value },
            onSelected = onValueChange,
            onDismiss = { showPresets = false },
        )
    }
}

