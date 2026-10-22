package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import io.github.dimitrysaf.provenio.core.profiles.AvatarCatalogItem
import io.github.dimitrysaf.provenio.core.profiles.Profile
import io.github.dimitrysaf.provenio.core.profiles.avatarImageUrl

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

/**
 * Marks a profile whose letter tile is in the colour picked for it, [Profile.avatarColorHex],
 * rather than the one its name gives it. It takes the place of the avatar catalog's ids, which
 * no longer exist.
 */
const val CHOSEN_COLOR_AVATAR_ID = "color"

/** The colour picked for the profile's letter tile, or null when its name gives it one. */
fun profileTileColor(profile: Profile): Color? =
    profile.avatarColorHex.takeIf { profile.avatarId == CHOSEN_COLOR_AVATAR_ID }?.let(::parseHexColor)

fun normalizedAvatarUrl(url: String?): String? =
    url?.trim()?.takeIf { it.isValidAvatarUrl() }

/** A link to a picture, or a picture picked from a file, which the profile carries in itself. */
fun String.isValidAvatarUrl(): Boolean {
    val value = trim()
    if (value.startsWith(AvatarPictureDataUriPrefix)) return value.length <= MaxAvatarPictureLength
    return value.length <= 2048 &&
        !value.any { it.isWhitespace() } &&
        (value.startsWith("https://") || value.startsWith("http://"))
}

private const val MaxAvatarPictureLength = 200_000

fun profileAvatarImageUrl(profile: Profile, avatar: AvatarCatalogItem?): String? =
    normalizedAvatarUrl(profile.avatarUrl)
        ?: avatar?.let(::avatarImageUrl)
