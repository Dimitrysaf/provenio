package com.nuvio.app.features.collection

import androidx.compose.runtime.Immutable
import com.nuvio.app.core.home.PosterShape
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class SupabaseCollectionBlob(
    @SerialName("profile_id") val profileId: Int = 1,
    @SerialName("collections_json") val collectionsJson: kotlinx.serialization.json.JsonElement = kotlinx.serialization.json.JsonArray(emptyList()),
    @SerialName("updated_at") val updatedAt: String? = null,
)
