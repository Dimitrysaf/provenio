package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.core.metadata.MetaCompany
import io.github.dimitrysaf.provenio.core.metadata.MetaDetails
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailProductionSection(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onCompanyClick: ((MetaCompany, String) -> Unit)? = null,
) {
    val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
    val isNetworkSource = isSeriesLike && meta.networks.isNotEmpty()
    val sourceItems = if (isSeriesLike) {
        meta.networks.ifEmpty { meta.productionCompanies }
    } else {
        meta.productionCompanies.ifEmpty { meta.networks }
    }
    if (sourceItems.isEmpty()) return

    val entityKind = if (isNetworkSource) "network" else "company"

    val displayItems = if (isSeriesLike) {
        sourceItems.take(6)
    } else {
        val logosOnly = sourceItems.filter { !it.logo.isNullOrBlank() }
        (if (logosOnly.isNotEmpty()) logosOnly else sourceItems).take(6)
    }
    if (displayItems.isEmpty()) return

    DetailSection(
        title = if (isSeriesLike) {
            stringResource(Res.string.details_networks)
        } else {
            stringResource(Res.string.meta_section_production_title)
        },
        modifier = modifier,
        showHeader = showHeader,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chipHeight = when {
                maxWidth >= 1024.dp -> 44.dp
                maxWidth >= 720.dp -> 40.dp
                else -> 36.dp
            }
            val logoWidth = when {
                maxWidth >= 1024.dp -> 72.dp
                maxWidth >= 720.dp -> 68.dp
                else -> 64.dp
            }
            val logoHeight = when {
                maxWidth >= 1024.dp -> 26.dp
                maxWidth >= 720.dp -> 24.dp
                else -> 22.dp
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                displayItems.forEach { item ->
                    ProductionChip(
                        item = item,
                        chipHeight = chipHeight,
                        logoWidth = logoWidth,
                        logoHeight = logoHeight,
                        onClick = if (onCompanyClick != null && item.tmdbId != null) {
                            { onCompanyClick(item, entityKind) }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductionChip(
    item: MetaCompany,
    chipHeight: Dp,
    logoWidth: Dp,
    logoHeight: Dp,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(chipHeight),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.logo.isNullOrBlank()) {
                AsyncImage(
                    model = item.logo,
                    contentDescription = item.name,
                    modifier = Modifier
                        .width(logoWidth)
                        .height(logoHeight),
                    contentScale = ContentScale.Fit,
                    // Logos are marks on transparency, so they take the card's content colour and read in either theme.
                    colorFilter = ColorFilter.tint(LocalContentColor.current),
                )
            } else {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }

    if (onClick != null) {
        Card(onClick = onClick, content = content)
    } else {
        Card(content = content)
    }
}
