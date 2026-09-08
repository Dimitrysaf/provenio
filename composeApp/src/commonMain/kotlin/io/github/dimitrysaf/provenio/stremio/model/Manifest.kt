package io.github.dimitrysaf.provenio.stremio.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * One entry of a manifest's `resources`. The protocol allows either a bare string
 * (`"catalog"`) or an object narrowing the resource to certain types and id prefixes.
 */
@Serializable(with = ManifestResourceSerializer::class)
data class ManifestResource(
    val name: String,
    val types: List<String>? = null,
    val idPrefixes: List<String>? = null,
)

/** Accepts both the short string form and the full object form of a resource entry. */
object ManifestResourceSerializer : KSerializer<ManifestResource> {

    @Serializable
    private data class Full(
        val name: String,
        val types: List<String>? = null,
        val idPrefixes: List<String>? = null,
    )

    override val descriptor: SerialDescriptor =
        SerialDescriptor("stremio.ManifestResource", Full.serializer().descriptor)

    override fun deserialize(decoder: Decoder): ManifestResource {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("ManifestResource can only be read from JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonPrimitive -> ManifestResource(name = element.content)
            is JsonObject -> input.json.decodeFromJsonElement(Full.serializer(), element)
                .let { ManifestResource(it.name, it.types, it.idPrefixes) }
            else -> throw SerializationException("A resources entry must be a string or an object")
        }
    }

    override fun serialize(encoder: Encoder, value: ManifestResource) {
        val output = encoder as? JsonEncoder
            ?: throw SerializationException("ManifestResource can only be written as JSON")
        if (value.types == null && value.idPrefixes == null) {
            output.encodeJsonElement(JsonPrimitive(value.name))
        } else {
            output.encodeJsonElement(
                output.json.encodeToJsonElement(
                    Full(value.name, value.types, value.idPrefixes),
                ),
            )
        }
    }
}

@Serializable
data class Manifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val resources: List<ManifestResource> = emptyList(),
    val types: List<String> = emptyList(),
    val catalogs: List<ManifestCatalog> = emptyList(),
    val addonCatalogs: List<ManifestCatalog> = emptyList(),
    val idPrefixes: List<String>? = null,
    val background: String? = null,
    val logo: String? = null,
    val contactEmail: String? = null,
    val config: List<ConfigField> = emptyList(),
    val behaviorHints: ManifestBehaviorHints? = null,
) {
    /**
     * Whether this addon claims to serve a resource for a given type and id.
     *
     * A resource entry narrows the manifest's own `types`/`idPrefixes` when it carries its
     * own; an absent `idPrefixes` on both means the addon accepts any id.
     */
    fun supports(resource: String, type: String, id: String): Boolean {
        val entry = resources.firstOrNull { it.name == resource } ?: return false
        val supportedTypes = entry.types ?: types
        if (type !in supportedTypes) return false
        val prefixes = entry.idPrefixes ?: idPrefixes ?: return true
        return prefixes.any { id.startsWith(it) }
    }
}

@Serializable
data class ManifestCatalog(
    val type: String,
    val id: String,
    val name: String? = null,
    val extra: List<CatalogExtra> = emptyList(),
    // Older addons describe their extras with these two string arrays instead of `extra`.
    val extraRequired: List<String> = emptyList(),
    val extraSupported: List<String> = emptyList(),
) {
    /** The extras this catalog accepts, with the legacy string arrays folded in. */
    fun normalizedExtra(): List<CatalogExtra> {
        if (extra.isNotEmpty()) return extra
        val required = extraRequired.map { CatalogExtra(name = it, isRequired = true) }
        val supported = extraSupported
            .filter { it !in extraRequired }
            .map { CatalogExtra(name = it, isRequired = false) }
        return required + supported
    }
}

@Serializable
data class CatalogExtra(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
    val optionsLimit: Int? = null,
)

@Serializable
data class ConfigField(
    val key: String,
    val type: String,
    val default: String? = null,
    val title: String? = null,
    val options: List<String>? = null,
    val required: Boolean? = null,
)

@Serializable
data class ManifestBehaviorHints(
    val adult: Boolean? = null,
    val p2p: Boolean? = null,
    val configurable: Boolean? = null,
    val configurationRequired: Boolean? = null,
)
