package io.github.dimitrysaf.provenio.core.profiles

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.age_system_au
import provenio.composeapp.generated.resources.age_system_de
import provenio.composeapp.generated.resources.age_system_fr
import provenio.composeapp.generated.resources.age_system_gb
import provenio.composeapp.generated.resources.age_system_gr
import provenio.composeapp.generated.resources.age_system_nl
import provenio.composeapp.generated.resources.age_system_us_movies
import provenio.composeapp.generated.resources.age_system_us_tv

/**
 * What a profile may watch: the age ratings it is allowed, each as `COUNTRY:RATING`, from as many
 * rating boards as the parent wants, and whether titles no board has rated are allowed.
 */
@Serializable
data class ProfileAgeLimit(
    @SerialName("allowed_ratings") val allowedRatings: Set<String> = emptySet(),
    @SerialName("allow_unrated") val allowUnrated: Boolean = false,
)

/** One country's rating board, with its ratings from youngest to oldest. */
class AgeRatingSystem(
    val country: String,
    val labelRes: StringResource,
    val ratings: List<String>,
)

/** The rating boards a parent can pick ratings from. */
val AgeRatingSystems: List<AgeRatingSystem> = listOf(
    AgeRatingSystem("US", Res.string.age_system_us_movies, listOf("G", "PG", "PG-13", "R", "NC-17")),
    AgeRatingSystem("US", Res.string.age_system_us_tv, listOf("TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14", "TV-MA")),
    AgeRatingSystem("GB", Res.string.age_system_gb, listOf("U", "PG", "12A", "12", "15", "18")),
    AgeRatingSystem("DE", Res.string.age_system_de, listOf("0", "6", "12", "16", "18")),
    AgeRatingSystem("FR", Res.string.age_system_fr, listOf("U", "10", "12", "16", "18")),
    AgeRatingSystem("GR", Res.string.age_system_gr, listOf("K", "K-12", "K-15", "K-18")),
    AgeRatingSystem("NL", Res.string.age_system_nl, listOf("AL", "6", "9", "12", "14", "16", "18")),
    AgeRatingSystem("AU", Res.string.age_system_au, listOf("G", "PG", "M", "MA15+", "R18+")),
)

/** The key a rating is stored under in [ProfileAgeLimit.allowedRatings]. */
fun ageRatingKey(country: String, rating: String): String = "${country.uppercase()}:${normalizeAgeRating(rating)}"

private fun normalizeAgeRating(rating: String): String = rating.trim().uppercase().replace(" ", "")

object ProfileAgeLimits {
    /**
     * Whether [limit] lets a title through. With the title's ratings per country, every rating it
     * has from a board the parent picked ratings from must be one of the allowed ones. Without
     * them, its single [fallbackRating] must be allowed by some board. A title with neither is
     * allowed only when unrated titles are.
     */
    fun allows(
        limit: ProfileAgeLimit?,
        certifications: Map<String, Set<String>>?,
        fallbackRating: String?,
    ): Boolean {
        if (limit == null) return true
        val allowed = limit.allowedRatings
        val chosenCountries = allowed.map { it.substringBefore(':') }.toSet()
        if (certifications != null) {
            val relevant = certifications
                .filterKeys { it.uppercase() in chosenCountries }
                .flatMap { (country, ratings) -> ratings.map { ageRatingKey(country, it) } }
            if (relevant.isNotEmpty()) return relevant.all { it in allowed }
        }
        val rating = fallbackRating?.takeIf { it.isNotBlank() }?.let(::normalizeAgeRating)
            ?: return limit.allowUnrated
        return allowed.any { it.substringAfter(':') == rating }
    }
}
