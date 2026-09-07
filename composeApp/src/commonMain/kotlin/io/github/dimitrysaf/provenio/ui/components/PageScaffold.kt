package io.github.dimitrysaf.provenio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dimitrysaf.provenio.ui.chrome.LocalBarsVisible

@Composable
fun PageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(visible = LocalBarsVisible.current.value) {
            topBar()
        }

        ResponsiveBody(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.headlineLarge)
        }
    }
}
