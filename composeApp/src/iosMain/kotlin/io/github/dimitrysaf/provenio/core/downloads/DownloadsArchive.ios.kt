package io.github.dimitrysaf.provenio.core.downloads

// Deleting an iOS app's data deletes its downloads with it, so there is nothing to rebuild from.
internal actual object DownloadsArchive {
    actual fun save(fileName: String, record: String) = Unit

    actual fun remove(fileName: String) = Unit

    actual fun records(): List<String> = emptyList()

    actual fun mediaFileUris(): List<String> = emptyList()
}
