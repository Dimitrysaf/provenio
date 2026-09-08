package io.github.dimitrysaf.provenio.navigation

import kotlinx.serialization.Serializable

/**
 * A title's detail page.
 *
 * Type-safe route rather than a string with placeholders, because reading path arguments
 * back out means going through SavedState, which the multiplatform navigation host does
 * not expose the way the Android-only one does.
 */
@Serializable
data class DetailRoute(val type: String, val id: String)
