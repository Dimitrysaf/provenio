package io.github.dimitrysaf.provenio.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.designsystem.components.PosterCard
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/**
 * What a query answers with. Cells size themselves to the window rather than to the fixed
 * shelf width, so a wide window shows more per row instead of more empty gutter.
 */
@Composable
fun ResultGrid(
    results: List<MetaPreview>,
    onOpenDetail: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(results, key = { it.id }) { meta ->
            PosterCard(
                title = meta.name ?: meta.id,
                posterUrl = meta.poster,
                onClick = { onOpenDetail(meta.type, meta.id) },
                subtitle = meta.releaseInfo,
                placeholder = Icons.Outlined.Search,
            )
        }
    }
}
