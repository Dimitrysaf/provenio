package io.github.dimitrysaf.provenio.core.tracking.simkl

import io.github.dimitrysaf.provenio.core.catalog.CatalogPage
import io.github.dimitrysaf.provenio.core.collection.CollectionSource
import io.github.dimitrysaf.provenio.core.home.MetaPreview
import io.github.dimitrysaf.provenio.core.home.PosterShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jetbrains.compose.resources.getString
import provenio.composeapp.generated.resources.Res
import provenio.composeapp.generated.resources.collections_simkl_invalid_list
import provenio.composeapp.generated.resources.collections_simkl_premium_only

/**
 * A public Simkl list as a folder's source, through `GET /lists/{id}`. Simkl only hands a list's
 * items to a signed-in PRO or VIP account; anyone else gets a `premium_only` answer, which is
 * raised as an error the folder shows in place of the titles.
 */
object SimklPublicListSourceResolver {
    const val PAGE_LIMIT = 100

    private val json = Json { ignoreUnknownKeys = true }
    private val listIdInLink = Regex("""simkl\.com/(?:\d+/)?list/(\d+)""", RegexOption.IGNORE_CASE)

    /** The list's id from a simkl.com list link, or from the bare number. */
    fun parseListId(input: String): Long? {
        val trimmed = input.trim()
        trimmed.toLongOrNull()?.takeIf { it > 0L }?.let { return it }
        return listIdInLink.find(trimmed)?.groupValues?.get(1)?.toLongOrNull()?.takeIf { it > 0L }
    }

    /** The list's own name, for a source added without a title of its own. */
    suspend fun listTitle(listId: Long): String? = withContext(Dispatchers.Default) {
        val body = request(listId, page = 1, limit = 1)
        body.objectAt("list")?.stringAt("name") ?: body.stringAt("name")
    }

    suspend fun resolve(source: CollectionSource, page: Int = 1): CatalogPage = withContext(Dispatchers.Default) {
        val listId = source.simklListId?.takeIf { it > 0L } ?: error(getString(Res.string.collections_simkl_invalid_list))
        val body = request(listId, page = page, limit = PAGE_LIMIT)
        val rawItems = (body["items"] as? JsonArray).orEmpty()
        val items = rawItems.mapNotNull { it.toMetaPreview() }
        val totalPages = body.objectAt("pagination")?.stringAt("page_count")?.toIntOrNull()
        CatalogPage(
            items = items,
            rawItemCount = rawItems.size,
            nextSkip = (page + 1).takeIf { totalPages != null && page < totalPages },
        )
    }

    private suspend fun request(listId: Long, page: Int, limit: Int): JsonObject {
        val response = SimklApi.client.execute(
            SimklApiRequest(
                method = SimklHttpMethod.GET,
                path = "/lists/$listId",
                query = mapOf(
                    "extended" to "full",
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                ),
            ),
        )
        val body = json.parseToJsonElement(response.body) as? JsonObject
            ?: error(getString(Res.string.collections_simkl_invalid_list))
        // Simkl answers a free account with 200 and this body rather than an error status.
        if (body.stringAt("error") == "premium_only") error(getString(Res.string.collections_simkl_premium_only))
        return body
    }

    /** An item is either the title itself or the title wrapped under its kind, as Simkl's sync lists are. */
    private fun JsonElement.toMetaPreview(): MetaPreview? {
        val item = this as? JsonObject ?: return null
        val kind = listOf("movie", "show", "anime").firstOrNull { item[it] is JsonObject }
        val media = kind?.let { item.objectAt(it) } ?: item
        val ids = media.objectAt("ids") ?: return null
        val type = when (kind ?: item.stringAt("type") ?: media.stringAt("type")) {
            "movie", "movies" -> "movie"
            else -> "series"
        }
        val id = ids.stringAt("imdb")?.takeIf { it.startsWith("tt") }
            ?: ids.stringAt("tmdb")?.let { "tmdb:$it" }
            ?: (ids.stringAt("simkl_id") ?: ids.stringAt("simkl"))?.let { "simkl:$it" }
            ?: return null
        return MetaPreview(
            id = id,
            type = type,
            name = media.stringAt("title") ?: return null,
            poster = simklPosterUrl(media.stringAt("poster")),
            posterShape = PosterShape.Poster,
            releaseInfo = media.stringAt("year"),
        )
    }

    private fun JsonObject.objectAt(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.stringAt(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
}
