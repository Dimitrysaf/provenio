package io.github.dimitrysaf.provenio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.navigation.Destination
import io.github.dimitrysaf.provenio.pages.ListsPage
import io.github.dimitrysaf.provenio.pages.MoviesPage
import io.github.dimitrysaf.provenio.pages.ProfilePage
import io.github.dimitrysaf.provenio.pages.SettingsPage
import io.github.dimitrysaf.provenio.pages.TvPage
import io.github.dimitrysaf.provenio.ui.responsive.contentHorizontalPadding
import io.github.dimitrysaf.provenio.ui.responsive.maxContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var destination by remember { mutableStateOf(Destination.Profile) }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { entry ->
                        val selected = entry == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { destination = entry },
                            icon = {
                                Icon(
                                    imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
                                    contentDescription = entry.label,
                                )
                            },
                            label = { Text(entry.label) },
                        )
                    }
                }
            },
        ) { innerPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                val horizontalPadding = contentHorizontalPadding(maxWidth)

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    AnimatedContent(
                        targetState = destination,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = maxContentWidth)
                            .padding(horizontal = horizontalPadding),
                        transitionSpec = {
                            (fadeIn() + slideInVertically { height -> height / 8 })
                                .togetherWith(fadeOut() + slideOutVertically { height -> -height / 8 })
                        },
                    ) { current ->
                        val content: @Composable (Modifier) -> Unit = when (current) {
                            Destination.Profile -> { modifier -> ProfilePage(modifier) }
                            Destination.Tv -> { modifier -> TvPage(modifier) }
                            Destination.Movies -> { modifier -> MoviesPage(modifier) }
                            Destination.Lists -> { modifier -> ListsPage(modifier) }
                            Destination.Settings -> { modifier -> SettingsPage(modifier) }
                        }
                        content(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
