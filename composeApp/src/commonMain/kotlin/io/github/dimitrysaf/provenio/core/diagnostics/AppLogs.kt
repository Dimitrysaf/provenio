package io.github.dimitrysaf.provenio.core.diagnostics

// A file ready to save: one .log, or a .zip when the logs span several files.
class AppLogBundle(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)

sealed interface AppLogSaveResult {
    data object Saved : AppLogSaveResult

    data object Cancelled : AppLogSaveResult

    /**
     * Nothing was saved, for [reason]; [detail] is what the system said, where it said anything.
     * A file left empty is deleted, and [emptyFileLeft] says when even that could not be done.
     */
    data class Failed(
        val reason: AppLogSaveFailure,
        val detail: String? = null,
        val emptyFileLeft: Boolean = false,
    ) : AppLogSaveResult
}

enum class AppLogSaveFailure {
    /** There were no logs to gather. */
    NothingToSave,

    /** The logs were gone by the time the place to save them was chosen. */
    Lost,

    /** The file could not be written. */
    WriteFailed,

    /** The file was written, but nothing is in it. */
    Empty,
}

/** A crash the app went through: when, and what the system recorded of it. */
class AppCrash(
    val timeMillis: Long,
    val report: String,
)

// Everything the app has logged, at every level, gathered for the user to save.
expect object AppLogs {
    val isSupported: Boolean

    suspend fun collect(): AppLogBundle?

    /** The most recent crash recorded, to show when the logs cannot be saved. */
    suspend fun latestCrash(): AppCrash?

    /** The most recent crash since the user was last told of one, to ask once to save the logs. */
    suspend fun unseenCrash(): AppCrash?

    /** The user has been told of the crashes so far. */
    fun markCrashesSeen()
}

internal const val AppLogBaseName = "provenio-logs"
