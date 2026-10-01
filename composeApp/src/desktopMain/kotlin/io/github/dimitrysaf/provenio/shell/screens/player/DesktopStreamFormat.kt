package io.github.dimitrysaf.provenio.shell.screens.player

import java.net.HttpURLConnection
import java.net.URI

internal const val HlsFormat = "hls"
internal const val DashFormat = "dash"

internal fun declaredStreamFormat(
    url: String,
    streamType: String?,
    responseHeaders: Map<String, String>,
): String? {
    formatFromName(streamType?.trim()?.lowercase())?.let { return it }
    responseHeaders.entries
        .firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }
        ?.let { formatFromMimeType(it.value) }
        ?.let { return it }
    return formatFromUrl(url)
}

internal fun sniffStreamFormat(url: String, headers: Map<String, String>): String? {
    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
        return null
    }
    return runCatching {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = SniffTimeoutMs
            readTimeout = SniffTimeoutMs
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Mozilla/5.0")
            setRequestProperty("Accept", "*/*")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            setRequestProperty("Range", "bytes=0-${SniffBytes - 1}")
        }
        try {
            if (connection.responseCode !in 200..299) return@runCatching null
            formatFromMimeType(connection.contentType)?.let { return@runCatching it }
            val bytes = connection.inputStream.use { input -> input.readNBytes(SniffBytes) }
            formatFromContent(bytes.decodeToString())
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}

private fun formatFromContent(content: String): String? {
    val text = content.trimStart('﻿', ' ', '\t', '\r', '\n')
    return when {
        text.startsWith("#EXTM3U") -> HlsFormat
        text.startsWith("<") && text.contains("<MPD", ignoreCase = true) -> DashFormat
        else -> null
    }
}

private fun formatFromName(value: String?): String? = when (value) {
    "hls", "m3u8", "m3u" -> HlsFormat
    "dash", "mpd" -> DashFormat
    else -> value?.let(::formatFromMimeType)
}

private fun formatFromMimeType(value: String?): String? =
    when (value?.substringBefore(';')?.trim()?.lowercase()) {
        "application/vnd.apple.mpegurl",
        "application/mpegurl",
        "application/x-mpegurl",
        "audio/mpegurl",
        "audio/x-mpegurl",
        "application/m3u8" -> HlsFormat
        "application/dash+xml",
        "video/vnd.mpeg.dash.mpd" -> DashFormat
        else -> null
    }

private fun formatFromUrl(url: String): String? {
    val withoutFragment = url.substringBefore('#').lowercase()
    val path = withoutFragment.substringBefore('?')
    val query = withoutFragment.substringAfter('?', missingDelimiterValue = "")
    when (path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "")) {
        "m3u8", "m3u" -> return HlsFormat
        "mpd" -> return DashFormat
    }
    query.split('&').forEach { parameter ->
        val key = parameter.substringBefore('=', missingDelimiterValue = "").trim()
        val value = parameter.substringAfter('=', missingDelimiterValue = "").trim()
        if (key in FormatQueryKeys) {
            formatFromName(value.substringAfterLast('/').substringAfterLast('.'))?.let { return it }
        }
        if (value == HlsFormat || value == DashFormat) return value
        formatFromMimeType(value)?.let { return it }
    }
    return when {
        DelimitedM3u8.containsMatchIn(withoutFragment) || DelimitedHls.containsMatchIn(withoutFragment) -> HlsFormat
        DelimitedMpd.containsMatchIn(withoutFragment) -> DashFormat
        else -> null
    }
}

private const val SniffTimeoutMs = 5_000
private const val SniffBytes = 4_096
private val FormatQueryKeys = setOf(
    "format", "mime", "mime_type", "contenttype", "content_type", "type", "ext", "extension", "output",
)
private val DelimitedM3u8 = Regex("(^|[=/_.?&%-])m3u8($|[=/_.?&%-])")
private val DelimitedHls = Regex("(^|[=/_.?&%-])hls($|[=/_.?&%-])")
private val DelimitedMpd = Regex("(^|[=/_.?&%-])mpd($|[=/_.?&%-])")
