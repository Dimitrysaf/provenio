package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.ui.responsive.contentHorizontalPadding
import io.github.dimitrysaf.provenio.ui.responsive.maxContentWidth

/**
 * Search uses the M3 search component rather than a text field styled to look like one,
 * so the shape, height, colours and expand behaviour all come from the spec. The back
 * action lives in the search bar's own leading slot — the M3 search pattern replaces the
 * top app bar rather than sitting underneath one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = contentHorizontalPadding(maxWidth)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { expanded = false },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder = { Text("Search movies and shows") },
                        leadingIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxContentWidth),
            ) {
                // Results land here once the addon catalog is wired up.
            }
        }
    }
}
