package io.github.dimitrysaf.provenio.core.settings

fun parseHexColor(value: String): Int? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it !in '0'..'9' && it.uppercaseChar() !in 'A'..'F' }) return null
    return hex.toIntOrNull(16)
}

fun formatHexColor(color: Int): String = "#" + color.toString(16).padStart(6, '0').uppercase()
