package io.github.dimitrysaf.provenio.shell.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal actual fun PageScrollbar(state: LazyListState, modifier: Modifier) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 2.dp),
        style = LocalScrollbarStyle.current.copy(
            thickness = 8.dp,
            shape = RoundedCornerShape(4.dp),
            unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f),
            hoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
        ),
    )
}
