package io.github.dimitrysaf.provenio.shell.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val displayItems = sourceItems.take(MaxProductionItems)
    if (displayItems.isEmpty()) return

    val entityKind = if (isNetworkSource) "network" else "company"

    DetailSection(
        title = if (isSeriesLike) {
            stringResource(Res.string.details_networks)
        } else {
            stringResource(Res.string.meta_section_production_title)
        },
        modifier = modifier,
        showHeader = showHeader,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            displayItems.forEach { item ->
                ProductionChip(
                    item = item,
                    onClick = if (onCompanyClick != null && item.tmdbId != null) {
                        { onCompanyClick(item, entityKind) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductionChip(
    item: MetaCompany,
    onClick: (() -> Unit)?,
) {
    val logo = item.logo?.takeIf { it.isNotBlank() }
    AssistChip(
        onClick = onClick ?: {},
        label = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = logo?.let {
            {
                CompanyLogo(
                    url = it,
                    contentDescription = null,
                    modifier = Modifier
                        .height(18.dp)
                        .widthIn(max = 48.dp),
                )
            }
        },
    )
}

private const val MaxProductionItems = 8
