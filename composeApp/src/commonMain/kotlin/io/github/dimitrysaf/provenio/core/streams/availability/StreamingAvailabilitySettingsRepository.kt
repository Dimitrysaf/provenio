package io.github.dimitrysaf.provenio.core.streams.availability

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StreamingAvailabilitySettingsRepository {
    private val _uiState = MutableStateFlow(StreamingAvailabilitySettings())
    val uiState: StateFlow<StreamingAvailabilitySettings> = _uiState.asStateFlow()

    private var hasLoaded = false

    private var enabled = false
    private var apiKey = ""

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): StreamingAvailabilitySettings {
        ensureLoaded()
        return _uiState.value
    }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        if (value && apiKey.isBlank()) return
        if (enabled == value) return
        enabled = value
        publish()
        StreamingAvailabilitySettingsStorage.saveEnabled(value)
    }

    fun setApiKey(value: String) {
        ensureLoaded()
        val normalized = value.trim()
        if (apiKey == normalized) return
        apiKey = normalized
        if (apiKey.isBlank()) {
            enabled = false
            StreamingAvailabilitySettingsStorage.saveEnabled(false)
        } else if (!enabled) {
            enabled = true
            StreamingAvailabilitySettingsStorage.saveEnabled(true)
        }
        publish()
        StreamingAvailabilitySettingsStorage.saveApiKey(normalized)
    }

    private fun loadFromDisk() {
        hasLoaded = true
        apiKey = StreamingAvailabilitySettingsStorage.loadApiKey().orEmpty().trim()
        enabled = (StreamingAvailabilitySettingsStorage.loadEnabled() ?: false) && apiKey.isNotBlank()
        publish()
    }

    private fun publish() {
        _uiState.value = StreamingAvailabilitySettings(
            enabled = enabled,
            apiKey = apiKey,
        )
    }
}
