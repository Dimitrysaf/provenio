package io.github.dimitrysaf.provenio.core.streams.availability

data class StreamingAvailabilitySettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    val isActive: Boolean
        get() = enabled && hasApiKey
}
