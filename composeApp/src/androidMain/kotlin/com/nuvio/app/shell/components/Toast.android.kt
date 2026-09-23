package com.nuvio.app.shell.components

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Android's own toast. Holds the application context, not an activity's, so a message raised while
 * the activity is being recreated still has somewhere to go.
 */
object PlatformToast {
    private var context: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var current: Toast? = null

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    internal fun show(message: String) {
        val appContext = context ?: return
        mainHandler.post {
            // One at a time: without cancelling the last, several in a row queue up and the final
            // one lands long after whatever caused it.
            current?.cancel()
            current = Toast.makeText(appContext, message, Toast.LENGTH_SHORT).also { it.show() }
        }
    }

    internal fun dismiss() {
        mainHandler.post {
            current?.cancel()
            current = null
        }
    }
}

internal actual fun platformShowToast(message: String) = PlatformToast.show(message)

internal actual fun platformDismissToast() = PlatformToast.dismiss()
