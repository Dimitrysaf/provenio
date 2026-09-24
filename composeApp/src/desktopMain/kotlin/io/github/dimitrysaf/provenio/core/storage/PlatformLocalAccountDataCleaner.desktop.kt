package io.github.dimitrysaf.provenio.core.storage

import io.github.dimitrysaf.provenio.desktop.Context

internal actual object PlatformLocalAccountDataCleaner {
    private val preferenceNames = listOf(
        "provenio_addons",
        "provenio_library",
        "provenio_library_display_settings",
        "provenio_home_catalog_settings",
        "provenio_player_settings",
        "torrent_settings",
        "provenio_profile_cache",
        "provenio_avatar_cache",
        "provenio_profile_pin_cache",
        "provenio_theme_settings",
        "provenio_poster_card_style",
        "provenio_debrid_settings",
        "provenio_mdblist_settings",
        "provenio_auth",
        "provenio_trakt_auth",
        "provenio_simkl_auth",
        "provenio_simkl_sync",
        "provenio_trakt_library",
        "provenio_trakt_settings",
        "provenio_watched",
        "provenio_stream_link_cache",
        "provenio_stream_badge_settings",
        "provenio_continue_watching_preferences",
        "provenio_cw_enrichment",
        "provenio_episode_release_notifications",
        "provenio_episode_release_notifications_platform",
        "provenio_watch_progress",
        "provenio_discover_selection",
        "provenio_collection_mobile_settings",
        "provenio_collections",
        "provenio_plugins",
        "provenio_member_access",
    )

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun wipe() {
        val context = appContext ?: return
        preferenceNames.forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
        context.filesDir.resolve("provenio_plugin_scrapers").deleteRecursively()
    }
}
