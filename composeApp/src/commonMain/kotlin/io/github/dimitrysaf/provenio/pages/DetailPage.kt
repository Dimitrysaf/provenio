package io.github.dimitrysaf.provenio.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.Video

/**
 * Everything the addon gave us about one title.
 *
 * Addons vary wildly in how much they fill in, so every section is conditional and simply
 * absent when its field is. The two exceptions are the title and the description, which
 * always render, because a page with no heading and no body reads as broken rather than
 * as sparse.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPage(
    type: String,
    id: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var meta by remember { mutableStateOf<Meta?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(type, id) {
        loading = true
        meta = AddonRepository.meta(type, id)
        loading = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        val current = meta
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            current == null -> Text(
                text = "No add-on could describe this title.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
            )
            else -> MetaContent(current)
        }

        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun MetaContent(meta: Meta) {
    val seasons = meta.videos
        .groupBy { it.season ?: 0 }
        .toList()
        .sortedBy { (season, _) -> season }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Artwork(meta) }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = meta.name ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmall,
                )

                val facts = listOfNotNull(
                    meta.releaseInfo,
                    meta.runtime,
                    meta.imdbRating?.let { "IMDb $it" },
                    meta.country,
                    meta.language,
                )
                if (facts.isNotEmpty()) {
                    Text(
                        text = facts.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (meta.genres.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    meta.genres.forEach { genre ->
                        AssistChip(onClick = {}, label = { Text(genre) })
                    }
                }
            }
        }

        item {
            Text(
                text = meta.description ?: "No description provided",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        creditRow("Cast", meta.cast)
        creditRow("Director", meta.director)

        // Hoisted so the null check is not separated from the use by a lambda boundary.
        val awards = meta.awards
        if (awards != null) {
            item { LabelledFact("Awards", awards) }
        }

        seasons.forEach { (season, episodes) ->
            item {
                Text(
                    text = if (season == 0) "Specials" else "Season $season",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            items(episodes, key = { it.id }) { video -> EpisodeRow(video) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** A credit line, absent entirely when the addon listed nobody. */
private fun LazyListScope.creditRow(label: String, people: List<String>) {
    if (people.isEmpty()) return
    item { LabelledFact(label, people.joinToString(", ")) }
}

/** Background, logo and poster, each drawn only if the addon supplied it. */
@Composable
private fun Artwork(meta: Meta) {
    if (meta.background == null && meta.poster == null && meta.logo == null) return

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        if (meta.background != null) {
            AsyncImage(
                model = meta.background,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // Keeps the logo and the back button legible over a bright still.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                        ),
                    ),
            )
        } else if (meta.poster != null) {
            AsyncImage(
                model = meta.poster,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        if (meta.logo != null) {
            AsyncImage(
                model = meta.logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .height(56.dp),
            )
        }
    }
}

@Composable
private fun LabelledFact(label: String, value: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EpisodeRow(video: Video) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (video.thumbnail != null) {
            AsyncImage(
                model = video.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            val number = video.episode?.let { "$it. " } ?: ""
            Text(
                text = number + (video.title ?: "Episode"),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (video.released != null) {
                Text(
                    text = video.released.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (video.overview != null) {
                Text(
                    text = video.overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
