package com.nuvio.app.shell.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("com.nuvio.app.navigation.AppRoute")
sealed interface AppRoute : NavKey {
    val title: String?
        get() = null

    val subtitle: String?
        get() = null

    /** Full-screen destinations such as the video player keep native navigation chrome hidden. */
    val hidesNavigationBar: Boolean
        get() = false

    /** Stable enough to apply Navigation 3 launchSingleTop semantics in SwiftUI. */
    val navigationIdentity: String
        get() = toString()

    /** Lets an explicitly cross-tab route select its native SwiftUI stack. */
    val preferredTabName: String?
        get() = null
}

@Serializable
@SerialName("com.nuvio.app.navigation.SettingsDestinationRoute")
sealed interface SettingsDestinationRoute : AppRoute {
    override val preferredTabName: String
        get() = "Settings"
}

@Serializable
@SerialName("com.nuvio.app.navigation.TabsRoute")
data object TabsRoute : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.DetailRoute")
data class DetailRoute(
    val type: String,
    val id: String,
    override val title: String? = null,
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.PersonDetailRoute")
data class PersonDetailRoute(
    val personId: Int,
    val personName: String,
    val personPhoto: String? = null,
    val castAvatarTransitionKey: String? = null,
    val preferCrew: Boolean = false,
) : AppRoute {
    override val title: String
        get() = personName
}

@Serializable
@SerialName("com.nuvio.app.navigation.EntityBrowseRoute")
data class EntityBrowseRoute(
    val entityKind: String,
    val entityId: Int,
    val entityName: String,
    val sourceType: String = "tv",
) : AppRoute {
    override val title: String
        get() = entityName
}

/** A settings leaf promoted from the former in-screen page state machine. */
@Serializable
@SerialName("com.nuvio.app.navigation.SettingsPageRoute")
data class SettingsPageRoute(
    val pageName: String,
    override val title: String,
) : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.HomescreenSettingsRoute")
data class HomescreenSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.MetaScreenSettingsRoute")
data class MetaScreenSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.ContinueWatchingSettingsRoute")
data class ContinueWatchingSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.DownloadsSettingsRoute")
data class DownloadsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.DownloadShowRoute")
data class DownloadShowRoute(
    val showId: String,
    override val title: String,
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.AddonsSettingsRoute")
data class AddonsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.PluginsSettingsRoute")
data class PluginsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.AccountSettingsRoute")
data class AccountSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.SupportersContributorsSettingsRoute")
data class SupportersContributorsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.LicensesAttributionsSettingsRoute")
data class LicensesAttributionsSettingsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.CollectionsRoute")
data class CollectionsRoute(override val title: String = "") : SettingsDestinationRoute

@Serializable
@SerialName("com.nuvio.app.navigation.CollectionEditorRoute")
data class CollectionEditorRoute(
    val collectionId: String? = null,
    override val title: String = "",
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.CollectionEditorPageRoute")
data class CollectionEditorPageRoute(
    val collectionId: String? = null,
    val pageName: String,
    override val title: String,
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.FolderDetailRoute")
data class FolderDetailRoute(
    val collectionId: String,
    val folderId: String,
    override val title: String = "",
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.StreamRoute")
data class StreamRoute(
    val launchId: Long,
    override val title: String = "",
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.CatalogRoute")
data class CatalogRoute(
    val launchId: Long,
    override val title: String = "",
    override val subtitle: String? = null,
) : AppRoute

@Serializable
@SerialName("com.nuvio.app.navigation.PlayerRoute")
data class PlayerRoute(
    val launchId: Long,
    override val title: String = "",
) : AppRoute {
    override val hidesNavigationBar: Boolean
        get() = true
}
