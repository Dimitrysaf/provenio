package io.github.dimitrysaf.provenio.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.ui.components.AppTopBar
import io.github.dimitrysaf.provenio.ui.components.PageScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    PageScaffold(
        modifier = modifier,
        topBar = { scrollBehavior ->
            AppTopBar(
                title = "Profile",
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) {
        Text(text = "Profile", style = MaterialTheme.typography.headlineLarge)
    }
}
