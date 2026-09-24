package io.github.dimitrysaf.provenio.core.settings

import io.github.dimitrysaf.provenio.core.tracking.trakt.MoreLikeThisSourcePreference

internal fun effectiveTrackingRecommendationsSource(
    source: MoreLikeThisSourcePreference,
    traktConnected: Boolean,
): MoreLikeThisSourcePreference =
    if (source == MoreLikeThisSourcePreference.TRAKT && !traktConnected) {
        MoreLikeThisSourcePreference.TMDB
    } else {
        source
    }
