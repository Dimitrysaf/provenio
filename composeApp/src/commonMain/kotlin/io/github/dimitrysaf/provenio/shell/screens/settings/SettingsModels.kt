package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.addons_guide_title
import provenio.composeapp.generated.resources.collections_header
import provenio.composeapp.generated.resources.compose_settings_category_about
import provenio.composeapp.generated.resources.compose_settings_page_addons
import provenio.composeapp.generated.resources.compose_settings_page_advanced
import provenio.composeapp.generated.resources.compose_settings_page_continue_watching
import provenio.composeapp.generated.resources.compose_settings_page_debrid
import provenio.composeapp.generated.resources.compose_settings_page_hero_carousel
import provenio.composeapp.generated.resources.compose_settings_page_homescreen
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import provenio.composeapp.generated.resources.compose_settings_page_meta_screen
import provenio.composeapp.generated.resources.compose_settings_page_notifications
import provenio.composeapp.generated.resources.compose_settings_page_plugins
import provenio.composeapp.generated.resources.compose_settings_page_poster_customization
import provenio.composeapp.generated.resources.compose_settings_page_root
import provenio.composeapp.generated.resources.compose_settings_page_streaming_availability
import provenio.composeapp.generated.resources.compose_settings_page_streams
import provenio.composeapp.generated.resources.compose_settings_page_subtitles
import provenio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import provenio.composeapp.generated.resources.compose_settings_page_tracking
import provenio.composeapp.generated.resources.compose_settings_root_downloads_title
import provenio.composeapp.generated.resources.keyboard_shortcuts_title
import provenio.composeapp.generated.resources.settings_category_about_description
import provenio.composeapp.generated.resources.settings_category_accounts
import provenio.composeapp.generated.resources.settings_category_accounts_description
import provenio.composeapp.generated.resources.settings_category_advanced_description
import provenio.composeapp.generated.resources.settings_category_audio
import provenio.composeapp.generated.resources.settings_category_audio_description
import provenio.composeapp.generated.resources.settings_category_interface
import provenio.composeapp.generated.resources.settings_category_interface_description
import provenio.composeapp.generated.resources.settings_category_playback
import provenio.composeapp.generated.resources.settings_category_playback_description
import provenio.composeapp.generated.resources.settings_category_server
import provenio.composeapp.generated.resources.settings_category_server_description
import provenio.composeapp.generated.resources.settings_category_subtitles_description
import provenio.composeapp.generated.resources.compose_settings_root_downloads_description
import provenio.composeapp.generated.resources.compose_settings_root_notifications_description
import provenio.composeapp.generated.resources.settings_content_discovery_addons_description
import provenio.composeapp.generated.resources.settings_content_discovery_collections_description
import provenio.composeapp.generated.resources.settings_content_discovery_plugins_description
import org.jetbrains.compose.resources.StringResource

/**
 * The top level of Settings: the list a phone opens on, and the left half of the screen on a
 * large one. Most open a settings page; Downloads and Collections open their own screens.
 */
internal enum class SettingsCategory(
    val labelRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
) {
    Accounts(Res.string.settings_category_accounts, Res.string.settings_category_accounts_description, Icons.Rounded.AccountCircle),
    Interface(Res.string.settings_category_interface, Res.string.settings_category_interface_description, Icons.Rounded.Palette),
    Subtitles(Res.string.compose_settings_page_subtitles, Res.string.settings_category_subtitles_description, Icons.Rounded.Subtitles),
    Audio(Res.string.settings_category_audio, Res.string.settings_category_audio_description, Icons.Rounded.Audiotrack),
    Playback(Res.string.settings_category_playback, Res.string.settings_category_playback_description, Icons.Rounded.PlayArrow),
    Addons(Res.string.compose_settings_page_addons, Res.string.settings_content_discovery_addons_description, Icons.Rounded.Extension),
    Plugins(Res.string.compose_settings_page_plugins, Res.string.settings_content_discovery_plugins_description, Icons.Rounded.Hub),
    Downloads(Res.string.compose_settings_root_downloads_title, Res.string.compose_settings_root_downloads_description, Icons.Rounded.CloudDownload),
    Collections(Res.string.collections_header, Res.string.settings_content_discovery_collections_description, Icons.Rounded.CollectionsBookmark),
    Notifications(Res.string.compose_settings_page_notifications, Res.string.compose_settings_root_notifications_description, Icons.Rounded.Notifications),
    Advanced(Res.string.compose_settings_page_advanced, Res.string.settings_category_advanced_description, Icons.Rounded.Tune),
    Server(Res.string.settings_category_server, Res.string.settings_category_server_description, Icons.Rounded.Dns),
    About(Res.string.compose_settings_category_about, Res.string.settings_category_about_description, Icons.Rounded.Info),
}

/** Each category's colour in the list, so the eye can find one by its hue. */
internal val SettingsCategory.iconHue: SettingsIconHue
    get() = when (this) {
        SettingsCategory.Accounts -> SettingsIconHue.Blue
        SettingsCategory.Interface -> SettingsIconHue.Purple
        SettingsCategory.Subtitles -> SettingsIconHue.Lime
        SettingsCategory.Audio -> SettingsIconHue.Pink
        SettingsCategory.Playback -> SettingsIconHue.Red
        SettingsCategory.Addons -> SettingsIconHue.Orange
        SettingsCategory.Plugins -> SettingsIconHue.Indigo
        SettingsCategory.Downloads -> SettingsIconHue.Green
        SettingsCategory.Collections -> SettingsIconHue.Brown
        SettingsCategory.Notifications -> SettingsIconHue.Yellow
        SettingsCategory.Advanced -> SettingsIconHue.Grey
        SettingsCategory.Server -> SettingsIconHue.Cyan
        SettingsCategory.About -> SettingsIconHue.Teal
    }

/** The settings page a category opens, or null for Downloads and Collections, which are screens of their own. */
internal val SettingsCategory.page: SettingsPage?
    get() = when (this) {
        SettingsCategory.Accounts -> SettingsPage.Accounts
        SettingsCategory.Interface -> SettingsPage.Appearance
        SettingsCategory.Subtitles -> SettingsPage.Subtitles
        SettingsCategory.Audio -> SettingsPage.Audio
        SettingsCategory.Playback -> SettingsPage.Playback
        SettingsCategory.Addons -> SettingsPage.Addons
        SettingsCategory.Plugins -> SettingsPage.Plugins
        SettingsCategory.Downloads, SettingsCategory.Collections -> null
        SettingsCategory.Notifications -> SettingsPage.Notifications
        SettingsCategory.Advanced -> SettingsPage.Advanced
        SettingsCategory.Server -> SettingsPage.P2pStreaming
        SettingsCategory.About -> SettingsPage.About
    }

internal enum class SettingsPage(
    val titleRes: StringResource,
    val category: SettingsCategory,
    val parentPage: SettingsPage?,
) {
    Root(
        titleRes = Res.string.compose_settings_page_root,
        category = SettingsCategory.Accounts,
        parentPage = null,
    ),
    Accounts(
        titleRes = Res.string.settings_category_accounts,
        category = SettingsCategory.Accounts,
        parentPage = Root,
    ),
    TraktAuthentication(
        // Keep the enum name for saved navigation-state compatibility.
        titleRes = Res.string.compose_settings_page_tracking,
        category = SettingsCategory.Accounts,
        parentPage = Accounts,
    ),
    TmdbEnrichment(
        titleRes = Res.string.compose_settings_page_tmdb_enrichment,
        category = SettingsCategory.Accounts,
        parentPage = Accounts,
    ),
    MdbListRatings(
        titleRes = Res.string.compose_settings_page_mdblist_ratings,
        category = SettingsCategory.Accounts,
        parentPage = Accounts,
    ),
    StreamingAvailability(
        titleRes = Res.string.compose_settings_page_streaming_availability,
        category = SettingsCategory.Accounts,
        parentPage = Accounts,
    ),
    Debrid(
        titleRes = Res.string.compose_settings_page_debrid,
        category = SettingsCategory.Accounts,
        parentPage = Accounts,
    ),
    Appearance(
        titleRes = Res.string.settings_category_interface,
        category = SettingsCategory.Interface,
        parentPage = Root,
    ),
    Homescreen(
        titleRes = Res.string.compose_settings_page_homescreen,
        category = SettingsCategory.Interface,
        parentPage = Appearance,
    ),
    HeroCarousel(
        titleRes = Res.string.compose_settings_page_hero_carousel,
        category = SettingsCategory.Interface,
        parentPage = Homescreen,
    ),
    ContinueWatching(
        titleRes = Res.string.compose_settings_page_continue_watching,
        category = SettingsCategory.Interface,
        parentPage = Appearance,
    ),
    PosterCustomization(
        titleRes = Res.string.compose_settings_page_poster_customization,
        category = SettingsCategory.Interface,
        parentPage = Appearance,
    ),
    MetaScreen(
        titleRes = Res.string.compose_settings_page_meta_screen,
        category = SettingsCategory.Interface,
        parentPage = Appearance,
    ),
    Streams(
        titleRes = Res.string.compose_settings_page_streams,
        category = SettingsCategory.Interface,
        parentPage = Appearance,
    ),
    Subtitles(
        titleRes = Res.string.compose_settings_page_subtitles,
        category = SettingsCategory.Subtitles,
        parentPage = Root,
    ),
    Audio(
        titleRes = Res.string.settings_category_audio,
        category = SettingsCategory.Audio,
        parentPage = Root,
    ),
    Playback(
        titleRes = Res.string.settings_category_playback,
        category = SettingsCategory.Playback,
        parentPage = Root,
    ),
    Addons(
        titleRes = Res.string.compose_settings_page_addons,
        category = SettingsCategory.Addons,
        parentPage = Root,
    ),
    AddonsGuide(
        titleRes = Res.string.addons_guide_title,
        category = SettingsCategory.Addons,
        parentPage = Addons,
    ),
    Plugins(
        titleRes = Res.string.compose_settings_page_plugins,
        category = SettingsCategory.Plugins,
        parentPage = Root,
    ),
    Notifications(
        titleRes = Res.string.compose_settings_page_notifications,
        category = SettingsCategory.Notifications,
        parentPage = Root,
    ),
    Advanced(
        titleRes = Res.string.compose_settings_page_advanced,
        category = SettingsCategory.Advanced,
        parentPage = Root,
    ),
    KeyboardShortcuts(
        titleRes = Res.string.keyboard_shortcuts_title,
        category = SettingsCategory.Advanced,
        parentPage = Advanced,
    ),
    P2pStreaming(
        titleRes = Res.string.settings_category_server,
        category = SettingsCategory.Server,
        parentPage = Root,
    ),
    About(
        titleRes = Res.string.compose_settings_category_about,
        category = SettingsCategory.About,
        parentPage = Root,
    ),
    LicensesAttributions(
        titleRes = Res.string.compose_settings_page_licenses_attributions,
        category = SettingsCategory.About,
        parentPage = About,
    ),
}

internal val SettingsPage.opensInlineOnTablet: Boolean
    get() = parentPage != null

internal fun SettingsPage.previousPage(): SettingsPage? = parentPage
