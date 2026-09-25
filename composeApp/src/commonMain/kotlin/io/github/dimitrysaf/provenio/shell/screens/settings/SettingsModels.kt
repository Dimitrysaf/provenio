package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.compose_settings_category_about
import provenio.composeapp.generated.resources.compose_settings_category_general
import provenio.composeapp.generated.resources.compose_settings_page_addons
import provenio.composeapp.generated.resources.compose_settings_page_advanced
import provenio.composeapp.generated.resources.compose_settings_page_appearance
import provenio.composeapp.generated.resources.compose_settings_page_content_discovery
import provenio.composeapp.generated.resources.compose_settings_page_debrid
import provenio.composeapp.generated.resources.compose_settings_page_continue_watching
import provenio.composeapp.generated.resources.compose_settings_page_homescreen
import provenio.composeapp.generated.resources.compose_settings_page_integrations
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import provenio.composeapp.generated.resources.compose_settings_page_meta_screen
import provenio.composeapp.generated.resources.compose_settings_page_notifications
import provenio.composeapp.generated.resources.compose_settings_page_playback
import provenio.composeapp.generated.resources.compose_settings_page_plugins
import provenio.composeapp.generated.resources.compose_settings_page_poster_customization
import provenio.composeapp.generated.resources.compose_settings_page_root
import provenio.composeapp.generated.resources.compose_settings_page_streams
import provenio.composeapp.generated.resources.compose_settings_page_supporters_contributors
import provenio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import provenio.composeapp.generated.resources.compose_settings_page_trakt
import provenio.composeapp.generated.resources.compose_settings_page_tracking
import provenio.composeapp.generated.resources.settings_category_profile_tracking
import provenio.composeapp.generated.resources.local_sync_title
import org.jetbrains.compose.resources.StringResource

internal enum class SettingsCategory(
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    Profile(Res.string.settings_category_profile_tracking, Icons.Rounded.AccountCircle),
    General(Res.string.compose_settings_category_general, Icons.Rounded.Settings),
    About(Res.string.compose_settings_category_about, Icons.Rounded.Info),
    Advanced(Res.string.compose_settings_page_advanced, Icons.Rounded.Tune),
}

internal enum class SettingsPage(
    val titleRes: StringResource,
    val category: SettingsCategory,
    val parentPage: SettingsPage?,
) {
    Root(
        titleRes = Res.string.compose_settings_page_root,
        category = SettingsCategory.General,
        parentPage = null,
    ),
    SupportersContributors(
        titleRes = Res.string.compose_settings_page_supporters_contributors,
        category = SettingsCategory.About,
        parentPage = Root,
    ),
    LicensesAttributions(
        titleRes = Res.string.compose_settings_page_licenses_attributions,
        category = SettingsCategory.About,
        parentPage = Root,
    ),
    Playback(
        titleRes = Res.string.compose_settings_page_playback,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Appearance(
        titleRes = Res.string.compose_settings_page_appearance,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Streams(
        titleRes = Res.string.compose_settings_page_streams,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    Advanced(
        titleRes = Res.string.compose_settings_page_advanced,
        category = SettingsCategory.Advanced,
        parentPage = Root,
    ),
    Notifications(
        titleRes = Res.string.compose_settings_page_notifications,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    ContinueWatching(
        titleRes = Res.string.compose_settings_page_continue_watching,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    PosterCustomization(
        titleRes = Res.string.compose_settings_page_poster_customization,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    ContentDiscovery(
        titleRes = Res.string.compose_settings_page_content_discovery,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    Addons(
        titleRes = Res.string.compose_settings_page_addons,
        category = SettingsCategory.General,
        parentPage = ContentDiscovery,
    ),
    Plugins(
        titleRes = Res.string.compose_settings_page_plugins,
        category = SettingsCategory.General,
        parentPage = ContentDiscovery,
    ),
    Homescreen(
        titleRes = Res.string.compose_settings_page_homescreen,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    MetaScreen(
        titleRes = Res.string.compose_settings_page_meta_screen,
        category = SettingsCategory.General,
        parentPage = Appearance,
    ),
    Integrations(
        titleRes = Res.string.compose_settings_page_integrations,
        category = SettingsCategory.General,
        parentPage = Root,
    ),
    TmdbEnrichment(
        titleRes = Res.string.compose_settings_page_tmdb_enrichment,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    MdbListRatings(
        titleRes = Res.string.compose_settings_page_mdblist_ratings,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    Debrid(
        titleRes = Res.string.compose_settings_page_debrid,
        category = SettingsCategory.General,
        parentPage = Integrations,
    ),
    LocalSync(
        titleRes = Res.string.local_sync_title,
        category = SettingsCategory.Profile,
        parentPage = Root,
    ),
    TraktAuthentication(
        // Keep the enum name for saved navigation-state compatibility.
        titleRes = Res.string.compose_settings_page_tracking,
        category = SettingsCategory.Profile,
        parentPage = Root,
    ),
}

internal val SettingsPage.opensInlineOnTablet: Boolean
    get() = parentPage != null

internal fun SettingsPage.previousPage(): SettingsPage? = parentPage
