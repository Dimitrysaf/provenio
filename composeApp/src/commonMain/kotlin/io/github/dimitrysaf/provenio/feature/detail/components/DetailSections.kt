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
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
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

fun LazyListScope.castAndCrew(meta: Meta) {
    item { SectionHeader("Cast and crew", Icons.Outlined.Group) }

    val people = meta.cast.map { it to "Cast" } + meta.director.map { it to "Director" }
    if (people.isEmpty()) {
        item { EmptyNote("This add-on did not list any cast or crew.") }
        return
    }
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            people.forEach { (name, role) -> PersonCard(name, role) }
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

fun LazyListScope.tagsAndThemes(meta: Meta) {
    item { SectionHeader("Tags", Icons.Outlined.Sell) }
    if (meta.genres.isEmpty()) {
        item { EmptyNote("No tags provided.") }
    } else {
        item { ChipFlow(meta.genres) }
    }

    item { SectionHeader("Themes", Icons.Outlined.Palette) }
    // Themes are a Simkl concept. The addon protocol has no equivalent field.
    item { EmptyNote("Themes are not available from add-ons.") }
}

fun LazyListScope.commentsSection() {
    item { SectionHeader("Comments", Icons.Outlined.ChatBubbleOutline) }
    item { EmptyNote("Comments arrive with account sign-in.") }
}

fun LazyListScope.factsSection(meta: Meta) {
    item { SectionHeader("Facts", Icons.Outlined.Info) }

    val facts = listOfNotNull(
        meta.released?.let { "Air date" to it.take(10) },
        meta.country?.let { "Country" to it },
        meta.language?.let { "Language" to it },
        meta.runtime?.let { "Runtime" to it },
        meta.awards?.let { "Awards" to it },
        meta.website?.let { "Website" to it },
    )
    if (facts.isEmpty()) {
        item { EmptyNote("No facts provided.") }
        return
    }
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            facts.forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(
                        text = label,
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
    item { SectionHeader("Trailers", Icons.Outlined.Movie) }
    if (meta.trailers.isEmpty()) {
        item { EmptyNote("No trailers provided.") }
        return
    }
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
                text = trailer.type ?: "Trailer",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

fun LazyListScope.backdropsSection(meta: Meta) {
    item { SectionHeader("Backdrops", Icons.Outlined.Image) }
    val art = listOfNotNull(meta.background, meta.poster, meta.logo)
    if (art.isEmpty()) {
        item { EmptyNote("No artwork provided.") }
        return
    }
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
