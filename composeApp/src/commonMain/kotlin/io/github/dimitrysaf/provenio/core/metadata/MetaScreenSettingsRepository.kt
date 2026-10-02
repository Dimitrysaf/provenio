package io.github.dimitrysaf.provenio.core.metadata

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import provenio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

enum class MetaScreenSectionKey {
    ACTIONS,
    OVERVIEW,
    PARENTS_GUIDE,
    PRODUCTION,
    CAST,
    COMMENTS,
    TRAILERS,
    EPISODES,
    DETAILS,
    COLLECTION,
    MORE_LIKE_THIS,
}

data class MetaScreenSectionItem(
    val key: MetaScreenSectionKey,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val order: Int,
)

data class MetaScreenSettingsUiState(
    val items: List<MetaScreenSectionItem> = emptyList(),
    val backgroundMode: MetaScreenBackgroundMode = MetaScreenBackgroundMode.Normal,
    val heroTrailerPlayback: Boolean = false,
    val blurUnwatchedEpisodes: Boolean = false,
    val posterTransitionEnabled: Boolean = false,
)

/** What sits behind the Detail and person pages. */
enum class MetaScreenBackgroundMode {
    Normal,
    /** The title's artwork, blurred across the whole page. */
    Cinematic,
    ;

    companion object {
        fun parse(raw: String?): MetaScreenBackgroundMode? = when (raw?.lowercase()) {
            "normal" -> Normal
            "cinematic" -> Cinematic
            // The removed dominant colour mode also drew from the artwork, so it carries on as Cinematic.
            "dominant_color" -> Cinematic
            else -> null
        }

        fun persist(mode: MetaScreenBackgroundMode): String = when (mode) {
            Normal -> "normal"
            Cinematic -> "cinematic"
        }
    }
}

@Serializable
private data class StoredMetaScreenSectionPreference(
    val key: String,
    val enabled: Boolean = true,
    val order: Int = 0,
)

@Serializable
private data class StoredMetaScreenSettingsPayload(
    val items: List<StoredMetaScreenSectionPreference> = emptyList(),
    @SerialName("background_mode")
    val backgroundMode: String? = null,
    /** Older payloads stored the background as this flag before there was a choice of modes. */
    val cinematicBackground: Boolean = false,
    @SerialName("hero_trailer_playback")
    val heroTrailerPlayback: Boolean = false,
    @SerialName("blur_unwatched_episodes")
    val blurUnwatchedEpisodes: Boolean = false,
    @SerialName("poster_transition_enabled")
    val posterTransitionEnabled: Boolean = false,
)

private data class MetaScreenSectionDefinition(
    val key: MetaScreenSectionKey,
    val titleRes: StringResource,
    val descriptionRes: StringResource,
)

object MetaScreenSettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val definitions = listOf(
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.ACTIONS,
            titleRes = Res.string.meta_section_actions_title,
            descriptionRes = Res.string.meta_section_actions_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.OVERVIEW,
            titleRes = Res.string.meta_section_overview_title,
            descriptionRes = Res.string.meta_section_overview_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.PARENTS_GUIDE,
            titleRes = Res.string.meta_section_parents_guide_title,
            descriptionRes = Res.string.meta_section_parents_guide_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.PRODUCTION,
            titleRes = Res.string.meta_section_production_title,
            descriptionRes = Res.string.meta_section_production_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.CAST,
            titleRes = Res.string.settings_meta_cast,
            descriptionRes = Res.string.meta_section_cast_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.COMMENTS,
            titleRes = Res.string.settings_meta_comments,
            descriptionRes = Res.string.meta_section_comments_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.TRAILERS,
            titleRes = Res.string.settings_meta_trailers,
            descriptionRes = Res.string.meta_section_trailers_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.EPISODES,
            titleRes = Res.string.settings_meta_episodes,
            descriptionRes = Res.string.meta_section_episodes_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.DETAILS,
            titleRes = Res.string.meta_section_details_title,
            descriptionRes = Res.string.meta_section_details_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.COLLECTION,
            titleRes = Res.string.meta_section_collection_title,
            descriptionRes = Res.string.meta_section_collection_description,
        ),
        MetaScreenSectionDefinition(
            key = MetaScreenSectionKey.MORE_LIKE_THIS,
            titleRes = Res.string.meta_section_more_like_this_title,
            descriptionRes = Res.string.meta_section_more_like_this_description,
        ),
    )

    private val _uiState = MutableStateFlow(MetaScreenSettingsUiState())
    val uiState: StateFlow<MetaScreenSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var preferences: MutableMap<MetaScreenSectionKey, StoredMetaScreenSectionPreference> = mutableMapOf()
    private var backgroundMode: MetaScreenBackgroundMode = MetaScreenBackgroundMode.Normal
    private var heroTrailerPlayback: Boolean = false
    private var blurUnwatchedEpisodes: Boolean = false
    private var posterTransitionEnabled: Boolean = false
    private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true

        val payload = MetaScreenSettingsStorage.loadPayload().orEmpty().trim()
        if (payload.isNotEmpty()) {
            val parsed = runCatching {
                json.decodeFromString<StoredMetaScreenSettingsPayload>(payload)
            }.getOrNull()
            if (parsed != null) {
                backgroundMode = MetaScreenBackgroundMode.parse(parsed.backgroundMode)
                    ?: if (parsed.cinematicBackground) MetaScreenBackgroundMode.Cinematic else MetaScreenBackgroundMode.Normal
                heroTrailerPlayback = parsed.heroTrailerPlayback
                blurUnwatchedEpisodes = parsed.blurUnwatchedEpisodes
                posterTransitionEnabled = parsed.posterTransitionEnabled
                preferences = parsed.items.mapNotNull { item ->
                    val key = runCatching { MetaScreenSectionKey.valueOf(item.key) }.getOrNull() ?: return@mapNotNull null
                    key to item
                }.toMap().toMutableMap()
            }
        }

        normalizePreferences()
        publish()
        persist()
    }

    fun onProfileChanged() {
        hasLoaded = false
        preferences.clear()
        backgroundMode = MetaScreenBackgroundMode.Normal
        heroTrailerPlayback = false
        blurUnwatchedEpisodes = false
        posterTransitionEnabled = false
        _uiState.value = MetaScreenSettingsUiState()
        ensureLoaded()
    }

    fun setBackgroundMode(mode: MetaScreenBackgroundMode) {
        ensureLoaded()
        backgroundMode = mode
        publish()
        persist()
    }

    fun setHeroTrailerPlayback(enabled: Boolean) {
        ensureLoaded()
        heroTrailerPlayback = enabled
        publish()
        persist()
    }

    fun setBlurUnwatchedEpisodes(enabled: Boolean) {
        ensureLoaded()
        blurUnwatchedEpisodes = enabled
        publish()
        persist()
    }

    fun setPosterTransitionEnabled(enabled: Boolean) {
        ensureLoaded()
        posterTransitionEnabled = enabled
        publish()
        persist()
    }

    fun clearLocalState() {
        hasLoaded = false
        preferences.clear()
        backgroundMode = MetaScreenBackgroundMode.Normal
        heroTrailerPlayback = false
        blurUnwatchedEpisodes = false
        posterTransitionEnabled = false
        _uiState.value = MetaScreenSettingsUiState()
    }

    fun setEnabled(key: MetaScreenSectionKey, enabled: Boolean) {
        updatePreference(key) { preference ->
            preference.copy(enabled = enabled)
        }
    }

    /**
     * Restores the sections' order and visibility.
     *
     * Reset sits with the sections list and acts on it alone: the appearance settings above it
     * are separate choices, and clearing them from here would undo work the button does not name.
     */
    fun resetSectionsToDefaults() {
        ensureLoaded()
        preferences.clear()
        normalizePreferences()
        publish()
        persist()
    }

    fun moveByIndex(fromIndex: Int, toIndex: Int) {
        ensureLoaded()
        val orderedKeys = definitions
            .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
            .map { it.key }
            .toMutableList()
        if (fromIndex !in orderedKeys.indices || toIndex !in orderedKeys.indices) return
        if (fromIndex == toIndex) return
        orderedKeys.add(toIndex, orderedKeys.removeAt(fromIndex))
        orderedKeys.forEachIndexed { newIndex, sectionKey ->
            val current = preferences[sectionKey] ?: return@forEachIndexed
            preferences[sectionKey] = current.copy(order = newIndex)
        }
        publish()
        persist()
    }

    private fun updatePreference(
        key: MetaScreenSectionKey,
        transform: (StoredMetaScreenSectionPreference) -> StoredMetaScreenSectionPreference,
    ) {
        ensureLoaded()
        val current = preferences[key] ?: return
        preferences[key] = transform(current)
        publish()
        persist()
    }

    private fun normalizePreferences() {
        val normalized = mutableMapOf<MetaScreenSectionKey, StoredMetaScreenSectionPreference>()
        definitions.sortedBy { definition -> storedSortOrder(definition.key) }
            .forEachIndexed { index, definition ->
                val stored = preferences[definition.key]
                normalized[definition.key] = StoredMetaScreenSectionPreference(
                    key = definition.key.name,
                    enabled = stored?.enabled ?: true,
                    order = index,
                )
            }
        preferences = normalized
    }

    private fun storedSortOrder(key: MetaScreenSectionKey): Double {
        preferences[key]?.let { return it.order.toDouble() }
        if (preferences.isEmpty()) return Double.MAX_VALUE
        val index = definitions.indexOfFirst { it.key == key }
        val previous = definitions.take(index).lastOrNull { it.key in preferences } ?: return -1.0
        return preferences.getValue(previous.key).order + 0.5
    }

    private fun publish() {
        _uiState.value = MetaScreenSettingsUiState(
            items = definitions
                .sortedBy { definition -> preferences[definition.key]?.order ?: Int.MAX_VALUE }
                .map { definition ->
                    val preference = preferences[definition.key]
                    MetaScreenSectionItem(
                        key = definition.key,
                        title = localizedString(definition.titleRes),
                        description = localizedString(definition.descriptionRes),
                        enabled = preference?.enabled ?: true,
                        order = preference?.order ?: 0,
                    )
                },
            backgroundMode = backgroundMode,
            heroTrailerPlayback = heroTrailerPlayback,
            blurUnwatchedEpisodes = blurUnwatchedEpisodes,
            posterTransitionEnabled = posterTransitionEnabled,
        )
    }

    private fun persist() {
        MetaScreenSettingsStorage.savePayload(
            json.encodeToString(
                StoredMetaScreenSettingsPayload(
                    items = preferences.values.sortedBy { it.order },
                    backgroundMode = MetaScreenBackgroundMode.persist(backgroundMode),
                    cinematicBackground = backgroundMode == MetaScreenBackgroundMode.Cinematic,
                    heroTrailerPlayback = heroTrailerPlayback,
                    blurUnwatchedEpisodes = blurUnwatchedEpisodes,
                    posterTransitionEnabled = posterTransitionEnabled,
                ),
            ),
        )
    }
}
