package com.nuvio.app.features.collection

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
import com.nuvio.app.shell.components.ListSubheader
import com.nuvio.app.shell.components.SingleChoiceBottomSheet
import com.nuvio.app.shell.components.SingleChoiceOption
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.settings.OuterCorner
import nuvio.composeapp.generated.resources.*
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
    imageUrl: String?,
    title: String,
    titlePlaceholder: String,
    onEditImage: () -> Unit,
    onEditTitle: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null,
    aspectRatio: Float = 16f / 9f,
) {
    val shape = RoundedCornerShape(OuterCorner)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(aspectRatio)
                .clip(shape)
                .clickable(onClick = onEditImage),
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

                    !emoji.isNullOrBlank() -> Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineMedium,
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

/**
 * Which of the two things a folder's cover can be. They are mutually exclusive in the folder
 * itself, which holds an emoji or an image and never both, so the dialog picks between them rather
 * than offering two fields that quietly clear each other.
 */
private enum class FolderCoverKind { TEXT, IMAGE }

/**
 * A folder's cover and the shape it is drawn in, as one dialog.
 *
 * Cover and tile shape were two rows in the form that each opened a sheet, which put three taps
 * between wanting a cover and having one. They belong together: the shape is what the cover is cut
 * to, so choosing it here shows immediately in the header the dialog was opened from.
 *
 * Both drafts are kept while the dialog is open, so switching between text and image does not
 * discard what was typed in the other. Confirming with an empty field is how a cover is removed.
 */
@Composable
internal fun FolderCoverDialog(
    initialEmoji: String?,
    initialImageUrl: String?,
    initialShape: PosterShape,
    onConfirm: (emoji: String?, imageUrl: String?, shape: PosterShape) -> Unit,
    onDismiss: () -> Unit,
) {
    var kind by remember {
        mutableStateOf(
            if (!initialImageUrl.isNullOrBlank()) FolderCoverKind.IMAGE else FolderCoverKind.TEXT,
        )
    }
    var textDraft by remember { mutableStateOf(initialEmoji.orEmpty()) }
    var urlDraft by remember { mutableStateOf(initialImageUrl.orEmpty()) }
    var tileShape by remember { mutableStateOf(initialShape) }

    val kinds = FolderCoverKind.entries
    val shapes = PosterShape.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.collections_editor_cover)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    kinds.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = entry == kind,
                            onClick = { kind = entry },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = kinds.size,
                            ),
                        ) {
                            Text(folderCoverKindLabel(entry))
                        }
                    }
                }

                when (kind) {
                    FolderCoverKind.TEXT -> OutlinedTextField(
                        value = textDraft,
                        onValueChange = { textDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(Res.string.collections_editor_cover_text)) },
                    )

                    FolderCoverKind.IMAGE -> OutlinedTextField(
                        value = urlDraft,
                        onValueChange = { urlDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(stringResource(Res.string.collections_editor_cover_image_url))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                }

                ListSubheader(text = stringResource(Res.string.collections_editor_tile_shape))

                // The three shapes share their width and their corner radius, so the choice is
                // only ever about how tall the tile stands.
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    shapes.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = entry == tileShape,
                            onClick = { tileShape = entry },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = shapes.size,
                            ),
                        ) {
                            Text(posterShapeLabel(entry))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val text = textDraft.trim()
                    val url = urlDraft.trim()
                    onConfirm(
                        text.takeIf { kind == FolderCoverKind.TEXT && it.isNotBlank() },
                        url.takeIf { kind == FolderCoverKind.IMAGE && it.isNotBlank() },
                        tileShape,
                    )
                },
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

@Composable
private fun folderCoverKindLabel(kind: FolderCoverKind): String =
    when (kind) {
        FolderCoverKind.TEXT -> stringResource(Res.string.collections_editor_cover_text)
        FolderCoverKind.IMAGE -> stringResource(Res.string.collections_editor_cover_image_url)
    }

/**
 * A folder's name, and whether that name is shown at all.
 *
 * Hiding the title is a fact about the title, so it is asked where the title is set rather than
 * sitting several rows away under a heading of its own.
 */
@Composable
internal fun FolderTitleDialog(
    initialTitle: String,
    initialHideTitle: Boolean,
    onConfirm: (title: String, hideTitle: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(initialTitle) }
    var hideTitle by remember { mutableStateOf(initialHideTitle) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.collections_rename_folder)) },
        text = {
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            if (draft.isNotBlank()) onConfirm(draft.trim(), hideTitle)
                        },
                    ),
                )

                // The whole row flips the switch, which keeps it to one target rather than two.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hideTitle = !hideTitle },
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.collections_editor_hide_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(Res.string.collections_editor_hide_title_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = hideTitle, onCheckedChange = { hideTitle = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(draft.trim(), hideTitle) },
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

/**
 * How tall a tile of each shape stands against a fixed width. The width and the corner radius do
 * not change with the shape, so this ratio is the whole of the difference between them.
 */
internal fun PosterShape.aspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 2f / 3f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 16f / 9f
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
                    IconButton(onClick = { showPresets = true }) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = presetSheetTitle,
                        )
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

