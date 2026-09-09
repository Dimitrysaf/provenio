package io.github.dimitrysaf.provenio.simkl

/**
 * Simkl returns image paths, not URLs, and each kind has its own folder, size suffix and
 * extension. Building these by hand at call sites is how you end up with silently broken
 * images, so every one goes through here.
 *
 * https://api.simkl.org/conventions/images.md
 */
object SimklImages {

    private const val Origin = "https://simkl.in"

    private fun proxied(path: String): String = "https://wsrv.nl/?url=$path&q=90"

    /** 340px wide, the right size for a shelf card. */
    fun poster(path: String?): String? =
        path?.takeIf { it.isNotBlank() }?.let { proxied("$Origin/posters/${it}_m.webp") }

    /** 1920x1080, used as a page backdrop. */
    fun fanart(path: String?): String? =
        path?.takeIf { it.isNotBlank() }?.let { proxied("$Origin/fanart/${it}_medium.webp") }

    /** Avatars are served straight from the origin, without the proxy. */
    fun avatar(path: String?): String? = path?.takeIf { it.isNotBlank() }?.let { value ->
        if (value.startsWith("http")) value else "$Origin/avatars/${value}_256.jpg"
    }
}
