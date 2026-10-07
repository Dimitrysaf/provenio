package io.github.dimitrysaf.provenio.desktop

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString

internal const val AfterUpdateArgument = "--after-update"

/** Coordinates the app with the Windows installer through the mutexes both sides agree on. */
internal object WindowsInstallerLock {
    private const val AppMutexName = "ProvenioAppMutex"
    private const val SetupMutexName = "ProvenioSetupMutex"
    private const val Synchronize = 0x00100000
    private const val SetupWaitMillis = 10 * 60 * 1000L
    private const val PollMillis = 250L

    private val isWindows = System.getProperty("os.name").orEmpty().lowercase().startsWith("windows")
    private var appMutex: Pointer? = null

    /**
     * Returns false when the installer is running and this launch should not go ahead. The launch
     * the installer makes after an update waits for it to finish instead.
     */
    fun mayStart(args: Array<String>): Boolean {
        if (!isWindows || !isSetupRunning()) return true
        if (AfterUpdateArgument !in args) return false
        val deadline = System.currentTimeMillis() + SetupWaitMillis
        while (isSetupRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(PollMillis)
        }
        return true
    }

    /** Holds the app mutex for the life of the process, so the installer can wait for the app to close. */
    fun holdAppMutex() {
        if (!isWindows || appMutex != null) return
        appMutex = runCatching { kernel32.CreateMutexW(null, false, WString(AppMutexName)) }.getOrNull()
    }

    private fun isSetupRunning(): Boolean = runCatching {
        val handle = kernel32.OpenMutexW(Synchronize, false, WString(SetupMutexName)) ?: return@runCatching false
        kernel32.CloseHandle(handle)
        true
    }.getOrDefault(false)

    private val kernel32: Kernel32 by lazy { Native.load("kernel32", Kernel32::class.java) }

    @Suppress("FunctionName")
    private interface Kernel32 : Library {
        fun CreateMutexW(attributes: Pointer?, initialOwner: Boolean, name: WString): Pointer?
        fun OpenMutexW(desiredAccess: Int, inheritHandle: Boolean, name: WString): Pointer?
        fun CloseHandle(handle: Pointer): Boolean
    }
}
