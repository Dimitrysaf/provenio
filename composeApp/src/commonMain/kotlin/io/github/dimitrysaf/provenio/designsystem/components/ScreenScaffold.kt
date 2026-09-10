package io.github.dimitrysaf.provenio.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Standard page frame, used by tab pages and sub-pages alike: chrome on top, responsive
 * body underneath. The bar receives the [TopAppBarScrollBehavior] so it can apply the M3
 * on-scroll container colour change and collapse as the body scrolls.
 *
 * Nothing here hides or animates the chrome away. A page that needs to cover the app's
 * navigation is stacked over it as a separate layer instead, so the body below never
 * changes size and never re-lays-out its children.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Column(modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
        topBar(scrollBehavior)
        ResponsiveBody(modifier = Modifier.weight(1f), content = content)
    }
}
