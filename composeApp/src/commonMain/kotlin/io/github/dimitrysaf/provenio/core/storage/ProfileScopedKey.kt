package io.github.dimitrysaf.provenio.core.storage

import io.github.dimitrysaf.provenio.core.profiles.ProfileRepository


object ProfileScopedKey {
    fun of(baseKey: String): String = "${baseKey}_${ProfileRepository.activeProfileId}"
    fun of(baseKey: String, profileId: Int): String = "${baseKey}_$profileId"
}
