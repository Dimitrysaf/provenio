package io.github.dimitrysaf.provenio.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.ui.components.AppTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold

@Composable
fun TvPage(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
) {
    PageScaffold(
        title = "TV",
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppTopBar(
                title = "TV",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onFilterClick) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                },
            )
        },
    )
}
