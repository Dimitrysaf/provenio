package io.github.dimitrysaf.provenio.shell.screens.profiles

import androidx.compose.ui.graphics.Color

/**
 * Google's generated letter avatar, following AOSP Contacts' `LetterTileDrawable`.
 *
 * The colour comes from hashing a stable identifier rather than the display name, so renaming a
 * profile keeps its colour, and the letter comes from the display name. That split is deliberate
 * in the original: `setLetterAndColorFromContactDetails(displayName, identifier)`.
 */
internal object LetterTile {

    /**
     * `res/values/colors.xml` → `letter_tile_colors`. AOSP notes these are picked so that white
     * text clears its contrast requirement on every one of them, which is why the font colour
     * below is a single value rather than something derived per tile.
     */
    private val Colors = listOf(
        Color(0xFFDB4437),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF673AB7),
        Color(0xFF3F51B5),
        Color(0xFF4285F4),
        Color(0xFF039BE5),
        Color(0xFF0097A7),
        Color(0xFF009688),
        Color(0xFF0F9D58),
        Color(0xFF689F38),
        Color(0xFFEF6C00),
        Color(0xFFFF5722),
        Color(0xFF757575),
    )

    /** Grey 600, the tile colour when there is no identifier to hash. */
    private val DefaultColor = Color(0xFF757575)

    /** `res/color/letter_tile_font_color`. */
    val FontColor = Color.White

    /** `res/values/dimens.xml` → `letter_to_tile_ratio`, 67% of the tile's smaller dimension. */
    const val LetterToTileRatio = 0.67f

    /**
     * `abs(identifier.hashCode()) % colors.length`. Kotlin's [String.hashCode] is Java's, and
     * that contract is stable across versions, so an identifier maps to the same colour forever.
     *
     * [kotlin.math.abs] of [Int.MIN_VALUE] is still negative, which the original doesn't guard,
     * so the remainder is taken before the sign is dropped.
     */
    fun colorFor(identifier: String): Color {
        if (identifier.isEmpty()) return DefaultColor
        val index = identifier.hashCode() % Colors.size
        return Colors[if (index < 0) index + Colors.size else index]
    }

    /**
     * The first character uppercased, but only when it is an English letter — anything else
     * falls back to the person icon, exactly as `isEnglishLetter` gates it upstream.
     */
    fun letterFor(displayName: String?): String? = displayName
        ?.firstOrNull()
        ?.takeIf { it in 'A'..'Z' || it in 'a'..'z' }
        ?.uppercaseChar()
        ?.toString()
}
