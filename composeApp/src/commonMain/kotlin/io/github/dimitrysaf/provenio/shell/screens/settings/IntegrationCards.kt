package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.provenio.core.debrid.DebridSettings
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettings
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettings
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettings
import org.jetbrains.compose.resources.stringResource
import provenio.composeapp.generated.resources.*

/** One thing an integration brings: a short tag with its icon. */
internal class IntegrationTag(val icon: ImageVector, val label: String)

/**
 * The Accounts page's Integrations section: a card per service in the tracking cards' style, the
 * service's artwork, what it brings, and a row into its options.
 */
internal fun LazyListScope.integrationCardsContent(
    isTablet: Boolean,
    tmdbSettings: TmdbSettings,
    mdbListSettings: MdbListSettings,
    streamingAvailabilitySettings: StreamingAvailabilitySettings,
    debridSettings: DebridSettings,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onStreamingAvailabilityClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item {
        SettingsSection(title = stringResource(Res.string.compose_settings_page_tmdb_enrichment), isTablet = isTablet) {
            IntegrationCard(
                name = "TMDB",
                logo = integrationLogoPainter(IntegrationLogo.Tmdb),
                fallbackIcon = Icons.Rounded.Movie,
                brush = Brush.linearGradient(listOf(Color(0xFF0D253F), Color(0xFF01B4E4))),
                tags = listOf(
                    IntegrationTag(Icons.Rounded.Info, stringResource(Res.string.settings_integration_tag_details)),
                    IntegrationTag(Icons.Rounded.Image, stringResource(Res.string.settings_integration_tag_artwork)),
                    IntegrationTag(Icons.Rounded.Movie, stringResource(Res.string.settings_integration_tag_trailers)),
                    IntegrationTag(Icons.Rounded.People, stringResource(Res.string.settings_integration_tag_cast)),
                    IntegrationTag(Icons.Rounded.CollectionsBookmark, stringResource(Res.string.settings_integration_tag_collections)),
                    IntegrationTag(Icons.Rounded.Tv, stringResource(Res.string.settings_integration_tag_episodes)),
                ),
                active = tmdbSettings.enabled && tmdbSettings.apiKey.isNotBlank(),
                status = integrationStatus(tmdbSettings.enabled, tmdbSettings.apiKey.isNotBlank()),
                onOptionsClick = onTmdbClick,
            )
        }
    }
    item {
        SettingsSection(title = stringResource(Res.string.compose_settings_page_mdblist_ratings), isTablet = isTablet) {
            IntegrationCard(
                name = "MDBList",
                logo = integrationLogoPainter(IntegrationLogo.MdbList),
                fallbackIcon = Icons.Rounded.Star,
                brush = Brush.linearGradient(listOf(Color(0xFF101820), Color(0xFF3B4A60))),
                tags = listOf("IMDb", "Rotten Tomatoes", "Metacritic", "Letterboxd", "Trakt", "MyAnimeList")
                    .map { IntegrationTag(Icons.Rounded.Star, it) },
                active = mdbListSettings.enabled && mdbListSettings.hasApiKey,
                status = integrationStatus(mdbListSettings.enabled, mdbListSettings.hasApiKey),
                onOptionsClick = onMdbListClick,
            )
        }
    }
    item {
        SettingsSection(title = stringResource(Res.string.compose_settings_page_streaming_availability), isTablet = isTablet) {
            IntegrationCard(
                name = stringResource(Res.string.compose_settings_page_streaming_availability),
                logo = null,
                fallbackIcon = Icons.Rounded.LiveTv,
                brush = Brush.linearGradient(listOf(Color(0xFF3A1C71), Color(0xFF2E86DE))),
                tags = listOf(
                    IntegrationTag(Icons.Rounded.Public, stringResource(Res.string.settings_integration_tag_where_to_watch)),
                    IntegrationTag(Icons.Rounded.LiveTv, stringResource(Res.string.settings_integration_tag_subscriptions)),
                    IntegrationTag(Icons.Rounded.ShoppingCart, stringResource(Res.string.settings_integration_tag_rent_buy)),
                ),
                active = streamingAvailabilitySettings.isActive,
                status = integrationStatus(streamingAvailabilitySettings.enabled, streamingAvailabilitySettings.hasApiKey),
                onOptionsClick = onStreamingAvailabilityClick,
            )
        }
    }
    item {
        SettingsSection(title = stringResource(Res.string.compose_settings_page_debrid), isTablet = isTablet) {
            IntegrationCard(
                name = stringResource(Res.string.compose_settings_page_debrid),
                logo = null,
                fallbackIcon = Icons.Rounded.Bolt,
                brush = Brush.linearGradient(listOf(Color(0xFF0F3D2E), Color(0xFF2E8B57))),
                tags = listOf(
                    IntegrationTag(Icons.Rounded.Bolt, stringResource(Res.string.settings_integration_tag_instant_playback)),
                    IntegrationTag(Icons.Rounded.CloudDownload, stringResource(Res.string.settings_integration_tag_cloud_library)),
                ) + listOf("Real-Debrid", "TorBox", "Premiumize").map { IntegrationTag(Icons.Rounded.Bolt, it) },
                active = debridSettings.enabled && debridSettings.hasAnyApiKey,
                status = integrationStatus(debridSettings.enabled, debridSettings.hasAnyApiKey),
                onOptionsClick = onDebridClick,
            )
        }
    }
}

/** On, off, or on but waiting for its key. */
@Composable
private fun integrationStatus(enabled: Boolean, hasKey: Boolean): String = stringResource(
    when {
        enabled && hasKey -> Res.string.settings_integration_status_on
        enabled -> Res.string.settings_integration_status_needs_key
        else -> Res.string.settings_integration_status_off
    },
)

/**
 * One service, built like a tracking card: the artwork heads the group, the tags say what it
 * brings, filled in while it is on, and a row leads into its options.
 */
@Composable
private fun IntegrationCard(
    name: String,
    logo: Painter?,
    fallbackIcon: ImageVector,
    brush: Brush,
    tags: List<IntegrationTag>,
    active: Boolean,
    status: String,
    onOptionsClick: () -> Unit,
) {
    val optionsLabel = stringResource(Res.string.settings_integration_options)
    SettingsList {
        shapedRow { shape ->
            IntegrationBanner(name = name, logo = logo, fallbackIcon = fallbackIcon, brush = brush, shape = shape)
        }
        shapedRow { shape ->
            IntegrationTagsRow(tags = tags, active = active, shape = shape)
        }
        navigationRow(
            title = optionsLabel,
            description = status,
            opensPage = true,
            onClick = onOptionsClick,
        )
    }
}

@Composable
private fun IntegrationBanner(
    name: String,
    logo: Painter?,
    fallbackIcon: ImageVector,
    brush: Brush,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntegrationBannerHeight)
            .clip(shape)
            .background(brush),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(
            imageVector = fallbackIcon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(IntegrationBannerGlyphSize),
        )
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (logo != null) {
                Image(
                    painter = logo,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

/** What the service brings, as tonal chips: filled in while it is on, muted until then. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntegrationTagsRow(
    tags: List<IntegrationTag>,
    active: Boolean,
    shape: RoundedCornerShape,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    Surface(
                        shape = CircleShape,
                        color = if (active) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        contentColor = if (active) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = tag.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(text = tag.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

private val IntegrationBannerHeight = 104.dp
private val IntegrationBannerGlyphSize = 120.dp
