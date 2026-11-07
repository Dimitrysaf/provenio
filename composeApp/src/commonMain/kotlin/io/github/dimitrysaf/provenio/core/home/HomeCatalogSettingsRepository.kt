package io.github.dimitrysaf.provenio.core.home

import io.github.dimitrysaf.provenio.core.playback.DeviceLanguagePreferences
import io.github.dimitrysaf.provenio.core.addons.ManagedAddon
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class HomeCatalogSettingsItem(
    val key: String,
    val defaultTitle: String,
    val addonName: String,
    val customTitle: String = "",
    val enabled: Boolean = true,
    val heroSourceEnabled: Boolean = true,
    val order: Int = 0,
    val isCollection: Boolean = false,
    val collectionId: String? = null,
    val isPinnedToTop: Boolean = false,
) {
    val displayTitle: String
        get() = customTitle.ifBlank { defaultTitle }
}

/** How a shelf lays out its titles. Continue Watching is always a row, whatever this says. */
enum class HomeShelfLayout {
    /** Titles wrap into rows that fit the width, so the page only scrolls down. */
    Grid,

    /** One row per shelf that scrolls sideways. */
    Horizontal,
}

data class HomeCatalogSettingsUiState(
    val heroEnabled: Boolean = true,
    val showCatalogType: Boolean = true,
    val hideUnreleasedContent: Boolean = false,
    val shelfLayout: HomeShelfLayout = HomeShelfLayout.Grid,
    val shelvesExpandedByDefault: Boolean = true,
    val items: List<HomeCatalogSettingsItem> = emptyList(),
) {
    /** The Hero Carousel shows only while it is on and at least one catalog feeds it; none chosen is off. */
    val heroShown: Boolean
        get() = heroEnabled && items.any { it.heroSourceEnabled }

    val signature: String
        get() = buildString {
            append(heroEnabled)
            append('|')
            append(showCatalogType)
            append('|')
            append(hideUnreleasedContent)
            append('|')
            append(
                items.joinToString(separator = "|") { item ->
                    "${item.key}:${item.order}:${item.enabled}:${item.heroSourceEnabled}:${item.customTitle}"
                }
            )
        }
}

@Serializable
private data class StoredHomeCatalogPreference(
    val key: String,
    val customTitle: String = "",
    val enabled: Boolean = true,
    val heroSourceEnabled: Boolean = true,
    val order: Int = 0,
)

@Serializable
private data class StoredHomeCatalogSettingsPayload(
    val heroEnabled: Boolean = true,
    val showCatalogType: Boolean = true,
    val hideUnreleasedContent: Boolean = false,
    // How shelves look suits the device's screen, so these stay on the device and are left out
    // of the synced payload.
    val shelfLayout: HomeShelfLayout = HomeShelfLayout.Grid,
    val shelvesExpandedByDefault: Boolean = true,
    val items: List<StoredHomeCatalogPreference> = emptyList(),
)

object HomeCatalogSettingsRepository {
    const val HERO_SOURCE_SELECTION_LIMIT = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(HomeCatalogSettingsUiState())
    val uiState: StateFlow<HomeCatalogSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var definitions: List<HomeCatalogDefinition> = emptyList()
    private var lastCatalogSync: Pair<List<ManagedAddon>, String>? = null
    private val preferencesRef = atomic<Map<String, StoredHomeCatalogPreference>>(emptyMap())
    private var preferences: Map<String, StoredHomeCatalogPreference>
        get() = preferencesRef.value
        set(value) {
            preferencesRef.value = value
        }
    private var heroEnabled = true
    private var showCatalogType = true
    private var hideUnreleasedContent = false
    private var shelfLayout = HomeShelfLayout.Grid
    private var shelvesExpandedByDefault = true

    fun onProfileChanged() {
        hasLoaded = false
        preferences = emptyMap()
        heroEnabled = true
        showCatalogType = true
        hideUnreleasedContent = false
        shelfLayout = HomeShelfLayout.Grid
        shelvesExpandedByDefault = true
        definitions = emptyList()
        lastCatalogSync = null
        _uiState.value = HomeCatalogSettingsUiState()
    }

    fun clearLocalState() {
        hasLoaded = false
        definitions = emptyList()
        lastCatalogSync = null
        preferences = emptyMap()
        heroEnabled = true
        showCatalogType = true
        hideUnreleasedContent = false
        shelfLayout = HomeShelfLayout.Grid
        shelvesExpandedByDefault = true
        _uiState.value = HomeCatalogSettingsUiState()
    }

    fun syncCatalogs(addons: List<ManagedAddon>) {
        ensureLoaded()
        // Collections live in the Library, so Home's rows are the addons' catalogs alone.
        val syncInput = addons to currentLocaleKey()
        if (lastCatalogSync == syncInput) return
        definitions = buildHomeCatalogDefinitions(addons)
        lastCatalogSync = syncInput
        if (definitions.isEmpty()) {
            publish()
            return
        }
        normalizePreferences()
        publish()
        persist()
    }

    internal fun snapshot(): HomeCatalogSettingsSnapshot {
        ensureLoaded()
        return HomeCatalogSettingsSnapshot(
            heroEnabled = heroEnabled,
            showCatalogType = showCatalogType,
            hideUnreleasedContent = hideUnreleasedContent,
            preferences = preferences.mapValues { (_, value) ->
                HomeCatalogPreference(
                    customTitle = value.customTitle,
                    enabled = value.enabled,
                    heroSourceEnabled = value.heroSourceEnabled,
                    order = value.order,
                )
            },
        )
    }

    fun setHeroEnabled(enabled: Boolean) {
        ensureLoaded()
        heroEnabled = enabled
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    fun setShowCatalogType(enabled: Boolean) {
        ensureLoaded()
        if (showCatalogType == enabled) return
        showCatalogType = enabled
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    fun setShelfLayout(layout: HomeShelfLayout) {
        ensureLoaded()
        if (shelfLayout == layout) return
        shelfLayout = layout
        publish()
        persist()
    }

    fun setShelvesExpandedByDefault(expanded: Boolean) {
        ensureLoaded()
        if (shelvesExpandedByDefault == expanded) return
        shelvesExpandedByDefault = expanded
        publish()
        persist()
    }

    fun setHideUnreleasedContent(enabled: Boolean) {
        ensureLoaded()
        if (hideUnreleasedContent == enabled) return
        hideUnreleasedContent = enabled
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    fun setHeroSourceEnabled(key: String, enabled: Boolean) {
        updatePreference(key) { preference ->
            if (!enabled) {
                preference.copy(heroSourceEnabled = false)
            } else if (selectedHeroSourceCount(excludingKey = key) >= HERO_SOURCE_SELECTION_LIMIT) {
                preference
            } else {
                preference.copy(heroSourceEnabled = true)
            }
        }
    }

    fun setEnabled(key: String, enabled: Boolean) {
        updatePreference(key) { preference ->
            preference.copy(enabled = enabled)
        }
    }

    fun setCustomTitle(key: String, title: String) {
        updatePreference(key) { preference ->
            preference.copy(customTitle = title)
        }
    }

    /**
     * Restores the catalogs and collections list: visibility, order and custom titles.
     *
     * Only that list. The Home Layout toggles are a section of their own, and the hero source
     * picks are another, so neither is touched — even though the hero picks are stored in this
     * same per-catalog map, which is what made the old reset take the whole page with it.
     */
    fun resetCatalogsToDefaults() {
        ensureLoaded()
        val heroEnabledKeys = preferences.filterValues { it.heroSourceEnabled }.keys

        preferences = emptyMap()
        normalizePreferences()
        // Normalising rebuilt every entry from its default, so the hero picks are put back on top.
        preferences = preferences.mapValues { (key, preference) ->
            preference.copy(heroSourceEnabled = key in heroEnabledKeys)
        }

        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    fun moveUp(key: String) {
        move(key = key, direction = -1)
    }

    fun moveDown(key: String) {
        move(key = key, direction = 1)
    }

    fun moveByIndex(fromIndex: Int, toIndex: Int) {
        ensureLoaded()
        val allKeys = allOrderedKeys()
        if (allKeys.isEmpty()) return
        if (fromIndex !in allKeys.indices || toIndex !in allKeys.indices) return
        if (fromIndex == toIndex) return
        val orderedKeys = allKeys.toMutableList()
        orderedKeys.add(toIndex, orderedKeys.removeAt(fromIndex))
        val updatedPreferences = preferences.toMutableMap()
        orderedKeys.forEachIndexed { index, itemKey ->
            val current = updatedPreferences[itemKey] ?: return@forEachIndexed
            updatedPreferences[itemKey] = current.copy(order = index)
        }
        preferences = updatedPreferences
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    private fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true

        val payload = HomeCatalogSettingsStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) return

        val parsedPayload = runCatching {
            json.decodeFromString<StoredHomeCatalogSettingsPayload>(payload)
        }.getOrNull()

        if (parsedPayload != null) {
            heroEnabled = parsedPayload.heroEnabled
            showCatalogType = parsedPayload.showCatalogType
            hideUnreleasedContent = parsedPayload.hideUnreleasedContent
            shelfLayout = parsedPayload.shelfLayout
            shelvesExpandedByDefault = parsedPayload.shelvesExpandedByDefault
            preferences = parsedPayload.items.associateBy { it.key }
            publish()
            return
        }

        val legacyItems = runCatching {
            json.decodeFromString<List<StoredHomeCatalogPreference>>(payload)
        }.getOrDefault(emptyList())

        preferences = legacyItems.associateBy { it.key }
        publish()
    }

    private fun normalizePreferences() {
        val current = preferences
        val allEntries = definitions.map { it.key }
        val knownKeys = allEntries.toSet()
        var nextOrder = (current.values.maxOfOrNull(StoredHomeCatalogPreference::order) ?: -1) + 1

        val orderedEntries = allEntries.mapIndexed { defaultIndex, key ->
            Triple(
                key,
                current[key]?.order ?: (nextOrder + defaultIndex),
                defaultIndex,
            )
        }.sortedWith(
            compareBy<Triple<String, Int, Int>>(
                { it.second },
                { it.third },
            ),
        ).map { it.first }

        val normalized = current
            .filterKeys { it !in knownKeys }
            .toMutableMap()
        var enabledHeroSourceCount = 0
        orderedEntries.forEach { key ->
            val stored = current[key]
            val heroSourceEnabled = (stored?.heroSourceEnabled ?: true) &&
                enabledHeroSourceCount < HERO_SOURCE_SELECTION_LIMIT
            if (heroSourceEnabled) {
                enabledHeroSourceCount += 1
            }
            normalized[key] = StoredHomeCatalogPreference(
                key = key,
                customTitle = stored?.customTitle.orEmpty(),
                enabled = stored?.enabled ?: true,
                heroSourceEnabled = heroSourceEnabled,
                order = stored?.order ?: nextOrder++,
            )
        }
        preferences = normalized.toMap()
    }

    private fun publish() {
        val items = definitions
            .map { definition ->
                val preference = preferences[definition.key]
                HomeCatalogSettingsItem(
                    key = definition.key,
                    defaultTitle = definition.defaultTitle,
                    addonName = definition.addonName,
                    customTitle = preference?.customTitle.orEmpty(),
                    enabled = preference?.enabled ?: true,
                    heroSourceEnabled = preference?.heroSourceEnabled ?: true,
                    order = preference?.order ?: 0,
                )
            }
            .sortedBy { it.order }

        _uiState.value = HomeCatalogSettingsUiState(
            heroEnabled = heroEnabled,
            showCatalogType = showCatalogType,
            hideUnreleasedContent = hideUnreleasedContent,
            shelfLayout = shelfLayout,
            shelvesExpandedByDefault = shelvesExpandedByDefault,
            items = items,
        )
    }

    private fun persist() {
        HomeCatalogSettingsStorage.savePayload(
            json.encodeToString(
                StoredHomeCatalogSettingsPayload(
                    heroEnabled = heroEnabled,
                    showCatalogType = showCatalogType,
                    hideUnreleasedContent = hideUnreleasedContent,
                    shelfLayout = shelfLayout,
                    shelvesExpandedByDefault = shelvesExpandedByDefault,
                    items = preferences.values.sortedBy { it.order },
                ),
            ),
        )
    }

    private fun updatePreference(
        key: String,
        transform: (StoredHomeCatalogPreference) -> StoredHomeCatalogPreference,
    ) {
        ensureLoaded()
        val current = preferences[key] ?: defaultPreferenceForMissingKey(key) ?: return
        val updated = transform(current)
        if (updated == current) return
        preferences = preferences + (key to updated)
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    private fun selectedHeroSourceCount(excludingKey: String? = null): Int {
        val catalogKeys = definitions.mapTo(mutableSetOf()) { it.key }
        return preferences.count { (itemKey, preference) ->
            itemKey != excludingKey && itemKey in catalogKeys && preference.heroSourceEnabled
        }
    }

    private fun move(
        key: String,
        direction: Int,
    ) {
        ensureLoaded()
        val orderedKeys = allOrderedKeys().toMutableList()
        if (orderedKeys.isEmpty()) return

        val currentIndex = orderedKeys.indexOf(key)
        if (currentIndex == -1) return

        val targetIndex = currentIndex + direction
        if (targetIndex !in orderedKeys.indices) return

        val movingKey = orderedKeys.removeAt(currentIndex)
        orderedKeys.add(targetIndex, movingKey)

        val updatedPreferences = preferences.toMutableMap()
        orderedKeys.forEachIndexed { index, itemKey ->
            val current = updatedPreferences[itemKey] ?: return@forEachIndexed
            updatedPreferences[itemKey] = current.copy(order = index)
        }
        preferences = updatedPreferences

        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    fun exportToSyncPayload(): SyncHomeCatalogPayload {
        ensureLoaded()
        val catalogDefinitionsByKey = definitions.associateBy { it.key }
        val items = preferences.values.sortedBy { it.order }.map { pref ->
            val catalogDefinition = catalogDefinitionsByKey[pref.key]
            // Kept for the devices that still show collections on Home.
            val isCollection = pref.key.startsWith("collection_")
            if (isCollection) {
                SyncCatalogItem(
                    addonId = "",
                    type = "",
                    catalogId = "",
                    enabled = pref.enabled,
                    order = pref.order,
                    customTitle = pref.customTitle,
                    isCollection = true,
                    collectionId = pref.key.removePrefix("collection_"),
                    key = pref.key,
                )
            } else {
                val legacyParts = pref.key.split(':', limit = 3)
                SyncCatalogItem(
                    addonId = catalogDefinition?.addonIdForSync() ?: legacyParts.getOrElse(0) { "" },
                    type = catalogDefinition?.type ?: legacyParts.getOrElse(1) { "" },
                    catalogId = catalogDefinition?.catalogId ?: legacyParts.getOrElse(2) { "" },
                    enabled = pref.enabled,
                    order = pref.order,
                    customTitle = pref.customTitle,
                    isCollection = false,
                    key = pref.key,
                    heroSourceEnabled = pref.heroSourceEnabled,
                )
            }
        }
        return SyncHomeCatalogPayload(
            showCatalogType = showCatalogType,
            hideUnreleasedContent = hideUnreleasedContent,
            items = items,
        )
    }

    fun applyFromRemote(payload: SyncHomeCatalogPayload) {
        ensureLoaded()
        // Showing the catalog type and hiding unreleased titles are how this device's Home looks,
        // so the other device's choice is not taken.
        if (payload.items.isNotEmpty()) {
            val existingHeroState = preferences.mapValues { it.value.heroSourceEnabled }
            val remotePreferences = payload.items.associate { item ->
                val key = item.preferenceKey()
                key to StoredHomeCatalogPreference(
                    key = key,
                    customTitle = item.customTitle,
                    enabled = item.enabled,
                    heroSourceEnabled = item.heroSourceEnabled ?: existingHeroState[key] ?: true,
                    order = item.order,
                )
            }
            val remoteKeys = remotePreferences.keys
            val knownKeys = knownPreferenceKeys()
            val preservedPreferences = preferences.filterKeys { key ->
                key !in remoteKeys && (key in knownKeys || key.requiresExplicitSyncKey())
            }
            preferences = preservedPreferences + remotePreferences
            normalizePreferences()
        }
        hasLoaded = true
        publish()
        persist()
        HomeRepository.applyCurrentSettings()
    }

    private fun allOrderedKeys(): List<String> =
        definitions.map { it.key }.sortedBy { key -> preferences[key]?.order ?: Int.MAX_VALUE }

    private fun defaultPreferenceForMissingKey(key: String): StoredHomeCatalogPreference? {
        if (definitions.none { it.key == key }) return null

        return StoredHomeCatalogPreference(
            key = key,
            enabled = true,
            heroSourceEnabled = selectedHeroSourceCount(excludingKey = key) < HERO_SOURCE_SELECTION_LIMIT,
            order = _uiState.value.items.firstOrNull { it.key == key }?.order
                ?: ((preferences.values.maxOfOrNull { it.order } ?: -1) + 1),
        )
    }

    private fun knownPreferenceKeys(): Set<String> = definitions.mapTo(mutableSetOf()) { it.key }

    private fun HomeCatalogDefinition.addonIdForSync(): String {
        val suffix = ":$type:$catalogId"
        return key.removeSuffix(suffix)
    }

    private fun SyncCatalogItem.preferenceKey(): String =
        key.ifBlank {
            if (isCollection) {
                "collection_$collectionId"
            } else {
                "$addonId:$type:$catalogId"
            }
        }

    private fun String.requiresExplicitSyncKey(): Boolean =
        !startsWith("collection_") && count { it == ':' } > 2
}

// Localised catalog titles are rebuilt whenever the device languages change.
private fun currentLocaleKey(): String = DeviceLanguagePreferences.preferredLanguageCodes().joinToString(",")
