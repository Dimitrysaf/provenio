package io.github.dimitrysaf.provenio.core.tracking

import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklMutationRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklLibraryRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklProgressRepository
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklTrackingLibraryProvider
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklTrackingProgressProvider
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklWatchedSyncAdapter
import io.github.dimitrysaf.provenio.core.tracking.simkl.SimklSyncRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktAuthRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktScrobbleRepository
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktTrackingLibraryProvider
import io.github.dimitrysaf.provenio.core.tracking.trakt.TraktTrackingProgressProvider
import io.github.dimitrysaf.provenio.core.watch.watching.sync.TraktWatchedSyncAdapter

fun ensureTrackingProvidersRegistered() {
    TraktAuthRepository.descriptor
    TraktScrobbleRepository.ensureRegistered()
    SimklAuthRepository.descriptor
    SimklSyncRepository.state
    SimklLibraryRepository.uiState
    SimklProgressRepository.uiState
    SimklMutationRepository.ensureRegistered()
    TrackingProviderRegistry.registerLibraryProvider(TraktTrackingLibraryProvider)
    TrackingProviderRegistry.registerLibraryProvider(SimklTrackingLibraryProvider)
    TrackingProviderRegistry.registerWatchedProvider(TraktWatchedSyncAdapter)
    TrackingProviderRegistry.registerWatchedProvider(SimklWatchedSyncAdapter)
    TrackingProviderRegistry.registerProgressProvider(TraktTrackingProgressProvider)
    TrackingProviderRegistry.registerProgressProvider(SimklTrackingProgressProvider)
}
