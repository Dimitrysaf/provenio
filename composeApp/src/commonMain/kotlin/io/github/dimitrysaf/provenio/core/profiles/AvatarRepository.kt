package io.github.dimitrysaf.provenio.core.profiles

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The avatar catalog lived on the account server; profiles now use letter tiles or an image link.
object AvatarRepository {
    private val _avatars = MutableStateFlow<List<AvatarCatalogItem>>(emptyList())
    val avatars: StateFlow<List<AvatarCatalogItem>> = _avatars.asStateFlow()

    suspend fun fetchAvatars() = Unit

    suspend fun refreshAvatars(force: Boolean = false) = Unit
}
