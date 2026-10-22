package io.github.dimitrysaf.provenio.core.stremio

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

internal fun inflateZlib(data: ByteArray): ByteArray? = runCatching {
    val inflater = Inflater()
    try {
        inflater.setInput(data)
        val output = ByteArrayOutputStream(data.size * 4)
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0) break
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } finally {
        inflater.end()
    }
}.getOrNull()
