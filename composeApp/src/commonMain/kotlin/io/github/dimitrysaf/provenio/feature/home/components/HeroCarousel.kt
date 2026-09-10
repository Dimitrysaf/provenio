package io.github.dimitrysaf.provenio.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.dimitrysaf.provenio.stremio.AddonRepository
import io.github.dimitrysaf.provenio.stremio.model.Meta
import io.github.dimitrysaf.provenio.stremio.model.MetaPreview

/** Taller than it is wide, the way a phone hero fills the top of a screen. */
private const val HeroAspectRatio = 0.9f

/** Past this the hero stops growing, so a tablet does not get a full page of one title. */
private val HeroMaxHeight = 560.dp

/**
 * The spotlight at the top of Home: one title at a time, swiped through.
 *
 * Backdrop and logo artwork live on the full [Meta], not on the catalog preview, so each
 * page fetches its own the first time it is shown and keeps it for the session. The pager
 * only composes the page either side of the current one, so this is two or three requests
 * rather than one per title, and swiping back never refetches.
 *
 * Everything the preview already knows — name, genre, year — draws immediately, and the
 * artwork upgrades in place when it arrives. Nothing waits on the network to appear.
 */
@Composable
fun HeroCarousel(
    items: List<MetaPreview>,
    onOpenDetail: (type: String, id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val full = remember { mutableStateMapOf<String, Meta>() }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HeroAspectRatio)
                .heightIn(max = HeroMaxHeight),
        ) { page ->
            val item = items[page]

            LaunchedEffect(item.id) {
                if (item.id !in full) {
                    AddonRepository.meta(item.type, item.id)?.let { full[item.id] = it }
                }
            }

            HeroPage(
                item = item,
                meta = full[item.id],
                onOpenDetail = onOpenDetail,
            )
        }

        Spacer(Modifier.height(16.dp))
        PageDots(count = items.size, current = pagerState.currentPage)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroPage(
    item: MetaPreview,
    meta: Meta?,
    onOpenDetail: (type: String, id: String) -> Unit,
) {
    // The catalog preview carries a banner but never a backdrop, so the wide art is only
    // right once the full meta lands; the poster keeps something on screen until then.
    val backdrop = meta?.background ?: item.banner ?: item.poster
    val logo = meta?.logo
    val facts = listOfNotNull(
        heroTypeLabel(item.type),
        (meta?.genres ?: item.genres).firstOrNull(),
        item.releaseInfo?.take(4)?.takeIf { it.length == 4 },
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Resolves to the page's own background before the text starts, so everything
        // below sits on a real surface and can use scheme colours rather than a fixed
        // white that would ignore the wallpaper palette. `scrim` is the role for
        // darkening media, so the fade honours a scheme override too.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.30f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                    0.52f to MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                    0.62f to MaterialTheme.colorScheme.background,
                    1f to MaterialTheme.colorScheme.background,
                ),
            ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (logo != null) {
                AsyncImage(
                    model = logo,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.75f).heightIn(max = 110.dp),
                )
            } else {
                Text(
                    text = item.name ?: item.id,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (facts.isNotEmpty()) {
                Text(
                    text = facts.joinToString("  •  "),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // No colour override: the filled button's own primary/onPrimary is what
            // carries the wallpaper palette through, which a fixed white never would.
            Button(
                onClick = { onOpenDetail(item.type, item.id) },
                shape = CircleShape,
                modifier = Modifier.padding(top = 20.dp),
            ) {
                Text(
                    text = "View Details",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** The current page reads as a bar, the rest as dots. */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        },
                    ),
            )
        }
    }
}

/** Singular here: the hero names one title, not a category of them. */
private fun heroTypeLabel(type: String): String = when (type) {
    "movie" -> "Movie"
    "series" -> "Series"
    "channel" -> "Channel"
    "tv" -> "TV"
    else -> type.replaceFirstChar { it.uppercase() }
}
