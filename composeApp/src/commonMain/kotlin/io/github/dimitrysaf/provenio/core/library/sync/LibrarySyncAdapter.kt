package io.github.dimitrysaf.provenio.core.library.sync

import io.github.dimitrysaf.provenio.core.library.LibraryItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibrarySyncKey(
    @SerialName("content_id") val contentId: String,
    @SerialName("content_type") val contentType: String,
)

data class LibraryDeltaEvent(
    val eventId: Long,
    val operation: String,
    val item: LibraryItem,
)

fun LibraryItem.toLibrarySyncKey(): LibrarySyncKey =
    LibrarySyncKey(
        contentId = id,
        contentType = type,
    )
