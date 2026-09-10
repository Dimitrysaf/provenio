package io.github.dimitrysaf.provenio.feature.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.designsystem.components.AppTopBar
import io.github.dimitrysaf.provenio.designsystem.components.ScreenScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onFilterClick: () -> Unit,
) {
    ScreenScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppTopBar(
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onFilterClick) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                },
            )
        },
    ) {
        Text(text = "Library", style = MaterialTheme.typography.headlineLarge)
    }
}
