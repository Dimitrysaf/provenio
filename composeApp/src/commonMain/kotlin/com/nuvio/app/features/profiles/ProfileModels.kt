package com.nuvio.app.features.profiles

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import com.nuvio.app.core.profiles.AvatarCatalogItem
import com.nuvio.app.core.profiles.NuvioProfile
import com.nuvio.app.core.profiles.avatarImageUrl

@Serializable
data class ProfilePushPayload(
    @SerialName("profile_index") val profileIndex: Int,
    val name: String,
    @SerialName("avatar_color_hex") val avatarColorHex: String,
    @SerialName("uses_primary_addons") val usesPrimaryAddons: Boolean = false,
    @SerialName("uses_primary_plugins") val usesPrimaryPlugins: Boolean = false,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("profile_background_id") val profileBackgroundId: String? = null,
    @SerialName("profile_background_url") val profileBackgroundUrl: String? = null,
)

@Serializable
data class PinVerifyResult(
    val unlocked: Boolean = false,
    @SerialName("retry_after_seconds") val retryAfterSeconds: Int = 0,
    val message: String? = null,
)

data class ProfileState(
    val profiles: List<NuvioProfile> = emptyList(),
    val activeProfile: NuvioProfile? = null,
    val isLoaded: Boolean = false,
    val hasEverSelectedProfile: Boolean = false,
    val rememberLastProfileEnabled: Boolean = false,
)

fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    return runCatching {
        when (cleaned.length) {
            6 -> Color(("FF$cleaned").toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> Color(0xFF1E88E5)
        }
    }.getOrDefault(Color(0xFF1E88E5))
}

val PROFILE_COLORS = listOf(
    "#1E88E5", "#E53935", "#43A047", "#FB8C00",
    "#8E24AA", "#00ACC1", "#F4511E", "#3949AB",
    "#C0CA33", "#D81B60", "#00897B", "#5E35B1",
    "#7CB342", "#039BE5", "#FFB300", "#6D4C41",
)

fun normalizedAvatarUrl(url: String?): String? =
    url?.trim()?.takeIf { it.isValidAvatarUrl() }

fun String.isValidAvatarUrl(): Boolean {
    val value = trim()
    return value.length <= 2048 &&
        !value.any { it.isWhitespace() } &&
        (value.startsWith("https://") || value.startsWith("http://"))
}

fun profileAvatarImageUrl(profile: NuvioProfile, avatar: AvatarCatalogItem?): String? =
    normalizedAvatarUrl(profile.avatarUrl)
        ?: avatar?.let(::avatarImageUrl)
