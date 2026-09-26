package io.github.dimitrysaf.provenio.core.downloads

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.IOException

// Finished downloads move from private storage into a Provenio folder file managers and other players can see.
internal object AndroidDownloadPublisher {
    private const val FolderName = "Provenio"

    // Movies/Provenio from Android 11, where apps may add videos there without a permission; before that, the app's own Movies folder.
    fun directory(context: Context): File =
        if (Build.VERSION.SDK_INT >= 30) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), FolderName)
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(context.filesDir, "downloads"), FolderName)
        }

    // The folder as the system file browser addresses it.
    fun documentId(context: Context): String =
        if (Build.VERSION.SDK_INT >= 30) {
            "primary:${Environment.DIRECTORY_MOVIES}/$FolderName"
        } else {
            "primary:Android/data/${context.packageName}/files/${Environment.DIRECTORY_MOVIES}/$FolderName"
        }

    // Moves the finished file out and returns where it landed, which carries a suffix when the name is taken.
    fun publish(context: Context, source: File): File {
        val directory = directory(context)
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Cannot create the downloads folder")
        var target = File(directory, source.name)
        var index = 1
        while (target.exists()) {
            target = File(directory, "${source.nameWithoutExtension} ($index).${source.extension}")
            index++
        }
        // Shared storage is a separate file system, so this is usually a copy.
        if (!source.renameTo(target)) {
            try {
                source.copyTo(target)
            } catch (error: Exception) {
                target.delete()
                throw error
            }
            source.delete()
        }
        return target
    }
}
