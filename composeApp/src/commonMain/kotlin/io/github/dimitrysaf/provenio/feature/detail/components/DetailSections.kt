package io.github.dimitrysaf.provenio.feature.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.resources.Res
import io.github.dimitrysaf.provenio.resources.detail_backdrops
import io.github.dimitrysaf.provenio.resources.detail_cast
import io.github.dimitrysaf.provenio.resources.detail_cast_and_crew
import io.github.dimitrysaf.provenio.resources.detail_director
import io.github.dimitrysaf.provenio.resources.detail_fact_air_date
import io.github.dimitrysaf.provenio.resources.detail_fact_awards
import io.github.dimitrysaf.provenio.resources.detail_fact_country
import io.github.dimitrysaf.provenio.resources.detail_fact_language
import io.github.dimitrysaf.provenio.resources.detail_fact_runtime
import io.github.dimitrysaf.provenio.resources.detail_fact_website
import io.github.dimitrysaf.provenio.resources.detail_facts
import io.github.dimitrysaf.provenio.resources.detail_tags
import io.github.dimitrysaf.provenio.resources.detail_trailer
import io.github.dimitrysaf.provenio.resources.detail_trailers
import org.jetbrains.compose.resources.stringResource

fun LazyListScope.castAndCrew(meta: Meta) {
    // A LazyListScope body is not composed, so the role cannot be looked up here. It
    // travels as a resource and is resolved inside the item that draws it.
    val people = meta.cast.map { it to Res.string.detail_cast } +
        meta.director.map { it to Res.string.detail_director }
    // No header either, not just no content — an addon that has nothing here is not
    // rare enough to be worth a permanent "nothing to see" block on every title, and a
    // different addon that does list a cast still gets the section as normal.
    if (people.isEmpty()) return
    item {
        SectionHeader(stringResource(Res.string.detail_cast_and_crew), Icons.Outlined.Group)
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            people.forEach { (name, role) -> PersonCard(name, stringResource(role)) }
        }
    }
}

@Composable
private fun PersonCard(name: String, role: String) {
    Column(
        modifier = Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The protocol carries names only, so there is no portrait to show.
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            text = role,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun LazyListScope.tagsSection(meta: Meta) {
    // Only appears once there is actually something — an addon that skips genres is not
    // rare enough to be worth a permanent dead block on every title.
    if (meta.genres.isEmpty()) return
    item { SectionHeader(stringResource(Res.string.detail_tags), Icons.Outlined.Sell) }
    item { ChipFlow(meta.genres) }
}

fun LazyListScope.factsSection(meta: Meta) {
    val facts = listOfNotNull(
        meta.released?.let { Res.string.detail_fact_air_date to it.take(10) },
        meta.country?.let { Res.string.detail_fact_country to it },
        meta.language?.let { Res.string.detail_fact_language to it },
        meta.runtime?.let { Res.string.detail_fact_runtime to it },
        meta.awards?.let { Res.string.detail_fact_awards to it },
        meta.website?.let { Res.string.detail_fact_website to it },
    )
    if (facts.isEmpty()) return
    item { SectionHeader(stringResource(Res.string.detail_facts), Icons.Outlined.Info) }
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            facts.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

fun LazyListScope.trailersSection(meta: Meta, openUrl: (String) -> Unit) {
    if (meta.trailers.isEmpty()) return
    item { SectionHeader(stringResource(Res.string.detail_trailers), Icons.Outlined.Movie) }
    items(meta.trailers, key = { it.source }) { trailer ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { openUrl("https://www.youtube.com/watch?v=${trailer.source}") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PlayCircle, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailer.type ?: stringResource(Res.string.detail_trailer),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

fun LazyListScope.backdropsSection(meta: Meta) {
    val art = listOfNotNull(meta.background, meta.poster, meta.logo)
    if (art.isEmpty()) return
    item { SectionHeader(stringResource(Res.string.detail_backdrops), Icons.Outlined.Image) }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            art.forEach { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}
