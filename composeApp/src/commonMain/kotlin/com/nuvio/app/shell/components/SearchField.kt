package com.nuvio.app.shell.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.compose_search_clear
import org.jetbrains.compose.resources.stringResource

/**
 * The app's search bar, built from `SearchBarDefaults.InputField` so it carries the spec's own
 * 56dp height, icon placement and text style.
 *
 * Deliberately not the `SearchBar` composable. That one owns a results surface and expands over
 * the page, whereas every search in this app leaves its results in the list underneath. Passing
 * `expanded = false` keeps just the bar.
 *
 * [divided] draws the spec's baseline style: a divider between the bar and the results below it,
 * which is what separates the two when the results are part of the same scrolling page rather
 * than a surface of their own.
 *
 * m3.material.io/components/search/specs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuvioSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    divided: Boolean = false,
    focusRequester: FocusRequester? = null,
    onSearch: (String) -> Unit = {},
) {
    // InputField clears its own focus whenever `expanded` is false, so this has to be real state
    // even though there is no results surface to expand: hardcoding false makes the field
    // impossible to type into.
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // No container behind the field. In the baseline style the divider is what separates the
        // bar from what follows, so a filled container would be a second separation doing the
        // same job. The field is only ever wrapped in a surface where there is no divider.
        SearchBarDefaults.InputField(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            // The requester belongs to the field itself, not to the column around it: one on the
            // column has nothing to give focus to.
            modifier = Modifier
                .fillMaxWidth()
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(Res.string.compose_search_clear),
                        )
                    }
                }
            } else {
                null
            },
        )

        if (divided) {
            HorizontalDivider()
        }
    }
}
