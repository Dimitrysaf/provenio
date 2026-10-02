package io.github.dimitrysaf.provenio.core.downloads

// A record of every finished download kept beside the files, where clearing the app's data
// cannot reach it, so the downloads list can be rebuilt from the files that are still there.
internal expect object DownloadsArchive {
    fun save(fileName: String, record: String)

    fun remove(fileName: String)

    fun records(): List<String>

    // The finished video files in the downloads folder.
    fun mediaFileUris(): List<String>
}
