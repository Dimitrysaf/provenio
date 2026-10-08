package io.github.dimitrysaf.provenio.core.localsync

import io.github.dimitrysaf.provenio.core.addons.AddonRepository
import io.github.dimitrysaf.provenio.core.collection.Collection
import io.github.dimitrysaf.provenio.core.collection.CollectionRepository
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsRepository
import io.github.dimitrysaf.provenio.core.debrid.DebridSettingsStorage
import io.github.dimitrysaf.provenio.core.home.HomeCatalogSettingsRepository
import io.github.dimitrysaf.provenio.core.home.SyncHomeCatalogPayload
import io.github.dimitrysaf.provenio.core.library.LibraryItem
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsRepository
import io.github.dimitrysaf.provenio.core.notifications.EpisodeReleaseNotificationsStorage
import io.github.dimitrysaf.provenio.core.profiles.ProfilePushPayload
import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository
import io.github.dimitrysaf.provenio.core.search.SearchHistoryRepository
import io.github.dimitrysaf.provenio.core.search.SearchHistoryStorage
import io.github.dimitrysaf.provenio.core.library.LibraryRepository
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.mdblist.MdbListSettingsStorage
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettingsRepository
import io.github.dimitrysaf.provenio.core.streams.availability.StreamingAvailabilitySettingsStorage
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsRepository
import io.github.dimitrysaf.provenio.core.metadata.tmdb.TmdbSettingsStorage
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsRepository
import io.github.dimitrysaf.provenio.core.playback.PlayerSettingsStorage
import io.github.dimitrysaf.provenio.core.tracking.TrackingSettingsRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsSettings
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktCommentsStorage
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktSettingsStorage
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesRepository
import io.github.dimitrysaf.provenio.core.watch.progress.ContinueWatchingPreferencesStorage
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressEntry
import io.github.dimitrysaf.provenio.core.watch.progress.WatchProgressRepository
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedItem
import io.github.dimitrysaf.provenio.core.watch.watched.WatchedRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val syncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// One kind of data this device syncs. Every key it exports starts with [prefix], which is how a value that disappeared is told apart from one another source owns.
internal interface LocalSyncSource {
    val prefix: String

    fun snapshot(): Map<String, JsonElement>

    /** Applies the other device's winners; a null value deletes the key. */
    fun apply(changes: Map<String, JsonElement?>)
}

/**
 * Everything that is synced for the active profile: what the person has and what the app does,
 * such as add-ons, catalogs, library, progress, integrations and playback. How the app looks
 * (theme, poster cards, page and shelf layouts, badges) stays on each device, since it suits that
 * device's screen.
 */
internal fun localSyncSources(): List<LocalSyncSource> = listOf(
    SettingsPayloadSource(
        name = "player",
        export = PlayerSettingsStorage::exportToSyncPayload,
        replace = { payload ->
            // The IntroDB key is not part of the payload, so replacing must not lose it.
            val introDbApiKey = PlayerSettingsStorage.loadIntroDbApiKey()
            PlayerSettingsStorage.replaceFromSyncPayload(payload)
            introDbApiKey?.let(PlayerSettingsStorage::saveIntroDbApiKey)
        },
        reload = PlayerSettingsRepository::onProfileChanged,
    ),
    SettingsPayloadSource(
        name = "debrid",
        export = DebridSettingsStorage::exportToSyncPayload,
        replace = DebridSettingsStorage::replaceFromSyncPayload,
        reload = DebridSettingsRepository::onProfileChanged,
    ),
    SettingsPayloadSource(
        name = "tmdb",
        export = TmdbSettingsStorage::exportToSyncPayload,
        replace = TmdbSettingsStorage::replaceFromSyncPayload,
        reload = TmdbSettingsRepository::onProfileChanged,
    ),
    SettingsPayloadSource(
        name = "mdblist",
        export = MdbListSettingsStorage::exportToSyncPayload,
        replace = MdbListSettingsStorage::replaceFromSyncPayload,
        reload = MdbListSettingsRepository::onProfileChanged,
    ),
    SettingsPayloadSource(
        name = "streaming_availability",
        export = StreamingAvailabilitySettingsStorage::exportToSyncPayload,
        replace = StreamingAvailabilitySettingsStorage::replaceFromSyncPayload,
        reload = StreamingAvailabilitySettingsRepository::onProfileChanged,
    ),
    SettingsPayloadSource(
        name = "trakt_comments",
        export = TraktCommentsStorage::exportToSyncPayload,
        replace = TraktCommentsStorage::replaceFromSyncPayload,
        reload = TraktCommentsSettings::onProfileChanged,
    ),
    // Continue Watching's behaviour, and the up-next items dismissed; how the row looks stays here.
    SettingsFieldsSource(
        name = "up_next",
        fields = setOf("upNextFromFurthestEpisode", "show_unaired_next_up", "dismissedNextUpKeys", "sort_mode"),
        load = ContinueWatchingPreferencesStorage::loadPayload,
        save = ContinueWatchingPreferencesStorage::savePayload,
        reload = ContinueWatchingPreferencesRepository::onProfileChanged,
    ),
    SettingsTextSource(
        name = "tracking",
        load = TraktSettingsStorage::loadPayload,
        save = TraktSettingsStorage::savePayload,
        reload = TrackingSettingsRepository::onProfileChanged,
    ),
    SettingsTextSource(
        name = "episode_alerts",
        load = EpisodeReleaseNotificationsStorage::loadPayload,
        save = EpisodeReleaseNotificationsStorage::savePayload,
        reload = EpisodeReleaseNotificationsRepository::onProfileChanged,
    ),
    SettingsTextSource(
        name = "search_history",
        load = SearchHistoryStorage::loadPayload,
        save = SearchHistoryStorage::savePayload,
        reload = SearchHistoryRepository::onProfileChanged,
    ),
    ProfilesSyncSource,
    HomeCatalogSyncSource,
    AddonsSyncSource,
    CollectionsSyncSource,
    LibrarySyncSource,
    WatchedSyncSource,
    ProgressSyncSource,
)

/**
 * Keys earlier versions synced for how the app looks, which now stay on each device. Their old
 * records are dropped from the ledger, so this device neither offers them again nor keeps what
 * another device sends under them.
 */
private val RetiredSyncKeys = setOf(
    "settings/poster_card_style",
    "settings/meta_screen",
    "settings/collection_mobile",
    "settings/continue_watching",
    "settings/library_display",
)
private val RetiredSyncPrefixes = listOf(
    "settings/theme/",
    "settings/stream_badges/",
)

internal fun isRetiredSyncKey(key: String): Boolean =
    key in RetiredSyncKeys || RetiredSyncPrefixes.any(key::startsWith)

/**
 * Some fields of a settings store kept as one JSON payload, each synced on its own; the rest of the
 * payload stays on the device.
 */
private class SettingsFieldsSource(
    name: String,
    private val fields: Set<String>,
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val reload: () -> Unit,
) : LocalSyncSource {
    override val prefix: String = "settings/$name/"

    private fun stored(): JsonObject =
        load()?.trim()?.takeIf(String::isNotEmpty)
            ?.let { runCatching { syncJson.parseToJsonElement(it) as? JsonObject }.getOrNull() }
            ?: JsonObject(emptyMap())

    override fun snapshot(): Map<String, JsonElement> =
        stored().filterKeys { it in fields }.mapKeys { (field, _) -> prefix + field }

    override fun apply(changes: Map<String, JsonElement?>) {
        if (changes.isEmpty()) return
        val payload = stored().toMutableMap()
        changes.forEach { (key, value) ->
            val field = key.removePrefix(prefix)
            if (field !in fields) return@forEach
            if (value == null) payload.remove(field) else payload[field] = value
        }
        save(syncJson.encodeToString(JsonObject.serializer(), JsonObject(payload)))
        reload()
    }
}

/** A settings store that exports a JSON object; each of its keys is synced on its own. */
private class SettingsPayloadSource(
    name: String,
    private val export: () -> JsonObject,
    private val replace: (JsonObject) -> Unit,
    private val reload: () -> Unit,
) : LocalSyncSource {
    override val prefix: String = "settings/$name/"

    override fun snapshot(): Map<String, JsonElement> =
        export().mapKeys { (field, _) -> prefix + field }

    override fun apply(changes: Map<String, JsonElement?>) {
        if (changes.isEmpty()) return
        val payload = export().toMutableMap()
        changes.forEach { (key, value) ->
            val field = key.removePrefix(prefix)
            if (value == null) payload.remove(field) else payload[field] = value
        }
        replace(JsonObject(payload))
        reload()
    }
}

/** A settings store kept as one serialized payload, synced as a single value. */
private class SettingsTextSource(
    name: String,
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val reload: () -> Unit,
) : LocalSyncSource {
    override val prefix: String = "settings/$name"

    override fun snapshot(): Map<String, JsonElement> {
        val payload = load()?.trim().orEmpty()
        return if (payload.isEmpty()) emptyMap() else mapOf(prefix to JsonPrimitive(payload))
    }

    override fun apply(changes: Map<String, JsonElement?>) {
        val value = changes[prefix] ?: return
        save(value.jsonPrimitive.contentOrNull.orEmpty())
        reload()
    }
}

// Profiles are shared by the whole device, so they travel with whichever profile is syncing.
private object ProfilesSyncSource : LocalSyncSource {
    override val prefix: String = "profiles/"

    // Profiles not read yet must not look like deleted ones, so this source counts as unreadable until they load.
    override fun snapshot(): Map<String, JsonElement> {
        check(ProfileRepository.state.value.isLoaded) { "Profiles are not loaded yet" }
        return ProfileRepository.profilesForSync().associate { profile ->
            prefix + profile.profileIndex to syncJson.encodeToJsonElement(profile)
        }
    }

    override fun apply(changes: Map<String, JsonElement?>) {
        val upserts = mutableListOf<ProfilePushPayload>()
        val removals = mutableListOf<Int>()
        changes.forEach { (key, value) ->
            if (value == null) {
                key.removePrefix(prefix).toIntOrNull()?.let { removals += it }
            } else {
                upserts += syncJson.decodeFromJsonElement<ProfilePushPayload>(value)
            }
        }
        ProfileRepository.applySyncedProfiles(upserts, removals)
    }
}

private object HomeCatalogSyncSource : LocalSyncSource {
    override val prefix: String = "home/catalogs"

    override fun snapshot(): Map<String, JsonElement> =
        mapOf(prefix to syncJson.encodeToJsonElement(HomeCatalogSettingsRepository.exportToSyncPayload()))

    override fun apply(changes: Map<String, JsonElement?>) {
        val value = changes[prefix] ?: return
        HomeCatalogSettingsRepository.applyFromRemote(syncJson.decodeFromJsonElement<SyncHomeCatalogPayload>(value))
    }
}

/** The addon list, in order; it is synced as one value because its order matters. */
private object AddonsSyncSource : LocalSyncSource {
    override val prefix: String = "addons"

    override fun snapshot(): Map<String, JsonElement> = mapOf(
        prefix to buildJsonArray {
            AddonRepository.addonsForSync().forEach { (manifestUrl, enabled) ->
                add(
                    buildJsonObject {
                        put("url", manifestUrl)
                        put("enabled", enabled)
                    },
                )
            }
        },
    )

    override fun apply(changes: Map<String, JsonElement?>) {
        val value = changes[prefix] as? JsonArray ?: return
        val addons = value.mapNotNull { element ->
            val entry = element as? JsonObject ?: return@mapNotNull null
            val url = entry["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            url to (entry["enabled"]?.jsonPrimitive?.booleanOrNull ?: true)
        }
        AddonRepository.applySyncedAddons(addons)
    }
}

private object CollectionsSyncSource : LocalSyncSource {
    override val prefix: String = "collection/"

    // Loaded first, or an unread store would look like every collection had been deleted.
    override fun snapshot(): Map<String, JsonElement> =
        CollectionRepository.also { it.initialize() }.collections.value.associate { collection ->
            prefix + collection.id to syncJson.encodeToJsonElement(collection)
        }

    override fun apply(changes: Map<String, JsonElement?>) {
        if (changes.isEmpty()) return
        CollectionRepository.initialize()
        val collections = CollectionRepository.collections.value.toMutableList()
        changes.forEach { (key, value) ->
            val id = key.removePrefix(prefix)
            val index = collections.indexOfFirst { it.id == id }
            if (value == null) {
                if (index >= 0) collections.removeAt(index)
            } else {
                val collection = syncJson.decodeFromJsonElement<Collection>(value)
                if (index >= 0) collections[index] = collection else collections += collection
            }
        }
        CollectionRepository.applyFromRemote(collections, syncJson.encodeToJsonElement(collections))
    }
}

private object LibrarySyncSource : LocalSyncSource {
    override val prefix: String = "library/"

    override fun snapshot(): Map<String, JsonElement> =
        LibraryRepository.localItemsForSync().associate { item ->
            "$prefix${item.type}/${item.id}" to syncJson.encodeToJsonElement(item)
        }

    override fun apply(changes: Map<String, JsonElement?>) {
        val upserts = mutableListOf<LibraryItem>()
        val removals = mutableListOf<Pair<String, String>>()
        changes.forEach { (key, value) ->
            if (value == null) {
                val (type, id) = key.removePrefix(prefix).split('/', limit = 2).takeIf { it.size == 2 }
                    ?: return@forEach
                removals += id to type
            } else {
                upserts += syncJson.decodeFromJsonElement<LibraryItem>(value)
            }
        }
        LibraryRepository.applySyncedChanges(upserts, removals)
    }
}

private object WatchedSyncSource : LocalSyncSource {
    override val prefix: String = "watched/"

    override fun snapshot(): Map<String, JsonElement> =
        WatchedRepository.localItemsForSync().entries.associate { (key, item) ->
            prefix + key to syncJson.encodeToJsonElement(item)
        }

    override fun apply(changes: Map<String, JsonElement?>) {
        val upserts = linkedMapOf<String, WatchedItem>()
        val removals = mutableListOf<String>()
        changes.forEach { (key, value) ->
            val storeKey = key.removePrefix(prefix)
            if (value == null) {
                removals += storeKey
            } else {
                upserts[storeKey] = syncJson.decodeFromJsonElement<WatchedItem>(value)
            }
        }
        WatchedRepository.applySyncedChanges(upserts, removals)
    }
}

private object ProgressSyncSource : LocalSyncSource {
    override val prefix: String = "progress/"

    override fun snapshot(): Map<String, JsonElement> =
        WatchProgressRepository.localEntriesForSync().entries.associate { (key, entry) ->
            prefix + key to syncJson.encodeToJsonElement(entry)
        }

    override fun apply(changes: Map<String, JsonElement?>) {
        val upserts = mutableListOf<WatchProgressEntry>()
        val removals = mutableListOf<String>()
        changes.forEach { (key, value) ->
            if (value == null) {
                removals += key.removePrefix(prefix)
            } else {
                upserts += syncJson.decodeFromJsonElement<WatchProgressEntry>(value)
            }
        }
        WatchProgressRepository.applySyncedChanges(upserts, removals)
    }
}
