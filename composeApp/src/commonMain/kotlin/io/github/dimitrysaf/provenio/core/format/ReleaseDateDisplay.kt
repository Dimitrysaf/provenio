package io.github.dimitrysaf.provenio.core.format

import io.github.dimitrysaf.provenio.core.i18n.localizedMonthName
import io.github.dimitrysaf.provenio.core.i18n.localizedRelativeDay
import io.github.dimitrysaf.provenio.core.time.daysUntilEpisodeRelease
import io.github.dimitrysaf.provenio.core.time.parseEpisodeReleaseLocalDate

/**
 * Formats ISO calendar dates (yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss…) for UI as "2025 February 1".
 * Other strings (e.g. year-only "2024", human text from addons) are returned unchanged.
 */
fun formatReleaseDateForDisplay(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = parseEpisodeReleaseLocalDate(trimmed) ?: return raw
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val year = parts[0].toIntOrNull() ?: return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return "$year ${localizedMonthName(month)} $day"
}

fun formatReleaseDateWithoutYear(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = parseEpisodeReleaseLocalDate(trimmed) ?: return raw
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return "${localizedMonthName(month)} $day"
}

/** Formats an ISO date as "March 1st, 2026". Other strings are returned unchanged. */
fun formatReleaseDateLong(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return raw
    val datePart = parseEpisodeReleaseLocalDate(trimmed) ?: return raw
    val parts = datePart.split('-')
    if (parts.size != 3) return raw
    val year = parts[0].toIntOrNull() ?: return raw
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return raw
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return raw
    return "${localizedMonthName(month)} ${day.withOrdinal()}, $year"
}

/** Formats an ISO date as "Today", "Tomorrow" or "March 1st, 2026", counted from [todayIsoDate]. */
fun formatReleaseDay(raw: String, todayIsoDate: String): String =
    daysUntilEpisodeRelease(todayIsoDate = todayIsoDate, releasedDate = raw)
        ?.let(::localizedRelativeDay)
        ?: formatReleaseDateLong(raw)

private fun Int.withOrdinal(): String {
    val suffix = if (this % 100 in 11..13) {
        "th"
    } else {
        when (this % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$this$suffix"
}

/**
 * Parses a release/air string (ISO date, year-only, or timestamp prefix) for compact UI (e.g. year chips).
 */
fun extractReleaseYearForDisplay(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    if (t.length == 4 && t.all { it.isDigit() }) {
        return t.toIntOrNull()?.takeIf { it in 1000..9999 }
    }
    val datePart = parseEpisodeReleaseLocalDate(t) ?: return null
    val yearStr = datePart.split('-').firstOrNull() ?: return null
    return yearStr.toIntOrNull()?.takeIf { it in 1000..9999 }
}
