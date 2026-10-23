package io.github.dimitrysaf.provenio.shell.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.settings_root_section_about
import provenio.composeapp.generated.resources.settings_root_section_account
import provenio.composeapp.generated.resources.settings_root_section_content
import provenio.composeapp.generated.resources.settings_root_section_user_interface
import provenio.composeapp.generated.resources.settings_category_subtitles_audio
import provenio.composeapp.generated.resources.collections_header
import provenio.composeapp.generated.resources.compose_settings_category_about
import provenio.composeapp.generated.resources.compose_settings_page_addons
import provenio.composeapp.generated.resources.compose_settings_page_advanced
import provenio.composeapp.generated.resources.compose_settings_page_continue_watching
import provenio.composeapp.generated.resources.compose_settings_page_debrid
import provenio.composeapp.generated.resources.compose_settings_page_hero_carousel
import provenio.composeapp.generated.resources.compose_settings_page_homescreen
import provenio.composeapp.generated.resources.compose_settings_page_licenses_attributions
import provenio.composeapp.generated.resources.settings_documentation
import provenio.composeapp.generated.resources.settings_category_documentation_description
import provenio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import provenio.composeapp.generated.resources.compose_settings_page_meta_screen
import provenio.composeapp.generated.resources.compose_settings_page_notifications
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
    Subtitles(Res.string.settings_category_subtitles_audio, Res.string.settings_category_subtitles_description, Icons.Rounded.Subtitles),
    Playback(Res.string.settings_category_playback, Res.string.settings_category_playback_description, Icons.Rounded.PlayArrow),
    Addons(Res.string.compose_settings_page_addons, Res.string.settings_content_discovery_addons_description, Icons.Rounded.Extension),
    Downloads(Res.string.compose_settings_root_downloads_title, Res.string.compose_settings_root_downloads_description, Icons.Rounded.DataUsage),
    Collections(Res.string.collections_header, Res.string.settings_content_discovery_collections_description, Icons.Rounded.CollectionsBookmark),
    Notifications(Res.string.compose_settings_page_notifications, Res.string.compose_settings_root_notifications_description, Icons.Rounded.Notifications),
    Documentation(Res.string.settings_documentation, Res.string.settings_category_documentation_description, Icons.AutoMirrored.Rounded.MenuBook),
    Advanced(Res.string.compose_settings_page_advanced, Res.string.settings_category_advanced_description, Icons.Rounded.Tune),
    Server(Res.string.settings_category_server, Res.string.settings_category_server_description, Icons.Rounded.Dns),
    About(Res.string.compose_settings_category_about, Res.string.settings_category_about_description, Icons.Rounded.Info),
}

/** The sections the Settings categories are grouped into on the first page. */
internal enum class SettingsRootSection(val labelRes: StringResource) {
    Account(Res.string.settings_root_section_account),
    UserInterface(Res.string.settings_root_section_user_interface),
    Content(Res.string.settings_root_section_content),
    About(Res.string.settings_root_section_about),
}

/** The section a category is listed under on the first Settings page. */
internal val SettingsCategory.rootSection: SettingsRootSection
    get() = when (this) {
        SettingsCategory.Accounts -> SettingsRootSection.Account
        SettingsCategory.Interface,
        SettingsCategory.Subtitles,
        SettingsCategory.Playback,
        SettingsCategory.Notifications,
        -> SettingsRootSection.UserInterface
        SettingsCategory.Addons,
        SettingsCategory.Downloads,
        SettingsCategory.Collections,
        SettingsCategory.Server,
        -> SettingsRootSection.Content
        SettingsCategory.Documentation,
        SettingsCategory.Advanced,
        SettingsCategory.About,
        -> SettingsRootSection.About
    }

/** [categories] grouped under their sections, in section order, leaving out sections with none. */
internal fun groupedSettingsCategories(categories: List<SettingsCategory>): List<Pair<SettingsRootSection, List<SettingsCategory>>> =
    SettingsRootSection.entries.mapNotNull { section ->
        categories.filter { it.rootSection == section }.takeIf { it.isNotEmpty() }?.let { section to it }
    }

/** Each category's colour in the list, so the eye can find one by its hue. */
internal val SettingsCategory.iconHue: SettingsIconHue
    get() = when (this) {
        SettingsCategory.Accounts -> SettingsIconHue.Blue
        SettingsCategory.Interface -> SettingsIconHue.Purple
        SettingsCategory.Subtitles -> SettingsIconHue.Lime
        SettingsCategory.Playback -> SettingsIconHue.Red
        SettingsCategory.Addons -> SettingsIconHue.Orange
        SettingsCategory.Downloads -> SettingsIconHue.Green
        SettingsCategory.Collections -> SettingsIconHue.Brown
        SettingsCategory.Notifications -> SettingsIconHue.Yellow
        SettingsCategory.Documentation -> SettingsIconHue.Pink
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
        SettingsCategory.Playback -> SettingsPage.Playback
        SettingsCategory.Addons -> SettingsPage.Addons
        SettingsCategory.Downloads, SettingsCategory.Collections -> null
        SettingsCategory.Notifications -> SettingsPage.Notifications
        SettingsCategory.Documentation -> SettingsPage.Documentation
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
        titleRes = Res.string.settings_category_subtitles_audio,
        category = SettingsCategory.Subtitles,
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
    Documentation(
        titleRes = Res.string.settings_documentation,
        category = SettingsCategory.Documentation,
        parentPage = Root,
    ),
}

internal val SettingsPage.opensInlineOnTablet: Boolean
    get() = parentPage != null

internal fun SettingsPage.previousPage(): SettingsPage? = parentPage
