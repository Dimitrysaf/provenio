package io.github.dimitrysaf.provenio.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TopAppBarDefaults
import io.github.dimitrysaf.provenio.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvPage(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onOpenDetail: (type: String, id: String) -> Unit,
    onAddAddons: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
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
        CatalogPage(
            type = "series",
            onOpenDetail = onOpenDetail,
            onAddAddons = onAddAddons,
        )
    }
}
