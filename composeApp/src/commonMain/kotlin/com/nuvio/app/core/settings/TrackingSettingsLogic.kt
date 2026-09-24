package com.nuvio.app.core.settings

import com.nuvio.app.core.tracking.trakt.MoreLikeThisSourcePreference

internal fun effectiveTrackingRecommendationsSource(
    source: MoreLikeThisSourcePreference,
    traktConnected: Boolean,
): MoreLikeThisSourcePreference =
    if (source == MoreLikeThisSourcePreference.TRAKT && !traktConnected) {
        MoreLikeThisSourcePreference.TMDB
    } else {
        source
    }
