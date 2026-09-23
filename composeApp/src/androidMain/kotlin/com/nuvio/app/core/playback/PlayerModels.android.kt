package com.nuvio.app.core.playback

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import nuvio.composeapp.generated.resources.*
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

internal class SubtitleRequestHeaderDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val externalSubtitles: List<com.nuvio.app.core.streams.StreamSubtitle>,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        SubtitleRequestHeaderDataSource(
            upstream = upstreamFactory.createDataSource(),
            externalSubtitles = externalSubtitles,
        )
}

internal class SubtitleRequestHeaderDataSource(
    private val upstream: DataSource,
    private val externalSubtitles: List<com.nuvio.app.core.streams.StreamSubtitle>,
) : DataSource {
    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val url = dataSpec.uri.toString()
        val subtitle = externalSubtitles.find { it.url == url }
        val headers = subtitle?.headers
        
        return if (headers.isNullOrEmpty()) {
            upstream.open(dataSpec)
        } else {
            val mergedHeaders = dataSpec.httpRequestHeaders.toMutableMap()
            headers.forEach { (key, value) ->
                mergedHeaders[key] = value
            }
            upstream.open(dataSpec.buildUpon().setHttpRequestHeaders(mergedHeaders).build())
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        upstream.close()
    }
}
