package io.github.dimitrysaf.provenio.desktop

import co.touchlab.kermit.Logger

/** android.util.Log for code shared with the Android app, routed to the app's logger. */
object Log {
    fun d(tag: String, message: String): Int = 0.also { Logger.withTag(tag).d { message } }
    fun i(tag: String, message: String): Int = 0.also { Logger.withTag(tag).i { message } }
    fun w(tag: String, message: String): Int = 0.also { Logger.withTag(tag).w { message } }
    fun w(tag: String, message: String, error: Throwable?): Int =
        0.also { Logger.withTag(tag).w(error) { message } }
    fun e(tag: String, message: String): Int = 0.also { Logger.withTag(tag).e { message } }
    fun e(tag: String, message: String, error: Throwable?): Int =
        0.also { Logger.withTag(tag).e(error) { message } }
}

/** android.os.SystemClock's monotonic clock. */
object SystemClock {
    fun elapsedRealtime(): Long = System.nanoTime() / 1_000_000L
}
