package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_search_clear
import org.jetbrains.compose.resources.stringResource

/**
 * The app's search bar: a fill-less field, the same one the Streams sheet filters with.
 *
 * [divided] draws the spec's baseline style: a divider between the bar and the results below it,
 * which is what separates the two when the results are part of the same scrolling page rather
 * than a surface of their own. [actions] adds buttons after the clear button, such as filters.
 *
 * m3.material.io/components/search/specs
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    divided: Boolean = false,
    focusRequester: FocusRequester? = null,
    onSearch: (String) -> Unit = {},
    actions: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    // Kept here rather than only the text, so the arrow keys know where the cursor is.
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val value = if (fieldValue.text == query) fieldValue else TextFieldValue(query, TextRange(query.length))
    val ownRequester = remember { FocusRequester() }
    val textRequester = focusRequester ?: ownRequester
    val buttonsRequester = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }
    var textFocused by remember { mutableStateOf(false) }
    var leftButtons by remember { mutableStateOf(false) }
    val hasButtons = query.isNotBlank() || actions != null
    val textMenu = rememberTextActionsMenu()
    // A sheet opened with keys starts on what comes after the field, not on its buttons.
    val sheetFocus = LocalSheetFocus.current
    var passOverButtons by remember { mutableStateOf(false) }
    LaunchedEffect(passOverButtons) {
        if (passOverButtons) {
            passOverButtons = false
            focusManager.moveFocus(FocusDirection.Down)
        }
    }
    // With a remote or a keyboard, Back leaves the field for what is below it rather than
    // leaving the screen.
    val leaveField = {
        if (!focusManager.moveFocus(FocusDirection.Down)) focusManager.clearFocus()
    }
    val usingKeys by rememberUpdatedState(openedWithKeys())
    PlatformBackHandler(enabled = fieldFocused && usingKeys, onBack = leaveField)

    Column(modifier = modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = { changed ->
                fieldValue = changed
                if (changed.text != query) onQueryChange(changed.text)
            },
            singleLine = true,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = if (hasButtons) {
                {
                    // The buttons sit inside the field, so the arrow keys would never find them
                    // beside it: right from the end of the text enters them, and left from the
                    // first one goes back to the text.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .onFocusChanged {
                                if (it.hasFocus && sheetFocus?.headerFocusable == false) passOverButtons = true
                            }
                            .focusRequester(buttonsRequester)
                            .focusProperties {
                                onExit = {
                                    if (requestedFocusDirection == FocusDirection.Left) {
                                        cancelFocusChange()
                                        leftButtons = true
                                    }
                                }
                            }
                            .focusGroup(),
                    ) {
                        if (query.isNotBlank()) {
                            WithTooltip(stringResource(Res.string.compose_search_clear)) {
                                IconButton(
                                    onClick = {
                                        onQueryChange("")
                                        // The button goes with the text; focus would go with it.
                                        if (fieldFocused) TextFieldEntry.asking { textRequester.requestFocus() }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(Res.string.compose_search_clear),
                                    )
                                }
                            }
                        }
                        actions?.invoke()
                    }
                }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    fieldFocused = it.hasFocus
                    textFocused = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Escape || (event.key == Key.Back && usingKeys)) {
                        if (event.type == KeyEventType.KeyUp) leaveField()
                        return@onPreviewKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyDown || !hasButtons) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionRight -> {
                            val atEnd = value.selection.collapsed && value.selection.end == value.text.length
                            textFocused && atEnd && buttonsRequester.requestFocus(FocusDirection.Enter)
                        }
                        Key.DirectionLeft -> {
                            if (textFocused || !fieldFocused) return@onPreviewKeyEvent false
                            leftButtons = false
                            if (!focusManager.moveFocus(FocusDirection.Left) || leftButtons) {
                                TextFieldEntry.asking { textRequester.requestFocus() }
                            }
                            true
                        }
                        else -> false
                    }
                }
                .arrowKeysLeaveField { query.isEmpty() }
                .textActionsMenu(textMenu)
                .focusRequester(textRequester),
        )
        TextActionsMenu(textMenu, text = query) { pasted ->
            val selection = value.selection
            val changed = value.text.replaceRange(selection.min, selection.max, pasted)
            fieldValue = TextFieldValue(changed, TextRange(selection.min + pasted.length))
            onQueryChange(changed)
        }

        if (divided) {
            HorizontalDivider()
        }
    }
}
