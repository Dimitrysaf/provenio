package io.github.dimitrysaf.provenio.core.membership

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileBackgroundCatalogItem(
    val id: String,
    val displayName: String,
    val landscapeImageBytes: ByteArray? = null,
    val portraitImageBytes: ByteArray? = null,
    val assetVersion: Int,
)

// Supporter backgrounds were served by the account server, so the local catalog is empty.
object ProfileBackgroundRepository {
    private val _catalog = MutableStateFlow<List<ProfileBackgroundCatalogItem>>(emptyList())
    val catalog: StateFlow<List<ProfileBackgroundCatalogItem>> = _catalog.asStateFlow()

    fun ensureLoaded() = Unit

    fun loadSelectedAndPreload(id: String, portrait: Boolean) = Unit

    fun preloadLandscapeImages() = Unit

    fun invalidate() {
        _catalog.value = emptyList()
    }
}
