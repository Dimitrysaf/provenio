package io.github.dimitrysaf.provenio.desktop

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val ShowCommand = "provenio-show-window"
private const val AckCommand = "provenio-ack"
private const val ConnectTimeoutMs = 1_000
private const val ClaimTimeoutMs = 20_000L
private const val ClaimRetryMs = 200L

object SingleInstance {
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val showRequests: SharedFlow<Unit> = requests.asSharedFlow()

    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

    private val directory: File by lazy {
        val home = System.getProperty("user.home")
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val base = when {
            os.contains("win") -> System.getenv("LOCALAPPDATA")?.let(::File) ?: File(home, "AppData/Local")
            os.contains("mac") -> File(home, "Library/Application Support")
            else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, ".local/share")
        }
        File(base, "provenio").apply { mkdirs() }
    }

    fun requestShow() {
        requests.tryEmit(Unit)
    }

    fun claim(args: Array<String>): Boolean {
        val deadline = System.currentTimeMillis() + ClaimTimeoutMs
        while (true) {
            when (tryLock()) {
                LockResult.Acquired -> {
                    runCatching { portFile.delete() }
                    return true
                }
                LockResult.Unavailable -> return true
                LockResult.HeldElsewhere -> if (forwardToRunningInstance(args)) return false
            }
            if (System.currentTimeMillis() >= deadline) return false
            Thread.sleep(ClaimRetryMs)
        }
    }

    private enum class LockResult { Acquired, HeldElsewhere, Unavailable }

    private fun tryLock(): LockResult = runCatching {
        val file = RandomAccessFile(File(directory, "instance.lock"), "rw").channel
        val fileLock = file.tryLock()
        if (fileLock == null) {
            file.close()
            LockResult.HeldElsewhere
        } else {
            channel = file
            lock = fileLock
            LockResult.Acquired
        }
    }.getOrDefault(LockResult.Unavailable)

    private val portFile: File
        get() = File(directory, "instance.port")

    fun listen(onUrl: (String) -> Unit) {
        if (lock == null || server != null) return
        val socket = runCatching {
            ServerSocket().apply { bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0)) }
        }.getOrNull() ?: return
        server = socket
        val port = socket.localPort.toString()
        runCatching {
            val pending = File(directory, "instance.port.tmp")
            pending.writeText(port)
            Files.move(pending.toPath(), portFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }.recoverCatching {
            portFile.writeText(port)
        }
        Thread({
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                runCatching {
                    client.use { connection ->
                        connection.soTimeout = ConnectTimeoutMs
                        connection.getInputStream().bufferedReader().readLines().forEach { line ->
                            val value = line.trim()
                            when {
                                value == ShowCommand -> Unit
                                value.startsWith("provenio:") || value.startsWith("stremio:") -> onUrl(value)
                            }
                        }
                        connection.getOutputStream().bufferedWriter().apply {
                            appendLine(AckCommand)
                            flush()
                        }
                    }
                }
                requests.tryEmit(Unit)
            }
        }, "single-instance").apply { isDaemon = true }.start()
    }

    private fun forwardToRunningInstance(args: Array<String>): Boolean = runCatching {
        val port = portFile.readText().trim().toInt()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), ConnectTimeoutMs)
            socket.soTimeout = ConnectTimeoutMs
            val writer = socket.getOutputStream().bufferedWriter()
            args.forEach { writer.appendLine(it) }
            writer.appendLine(ShowCommand)
            writer.flush()
            socket.shutdownOutput()
            socket.getInputStream().bufferedReader().readLine()?.trim() == AckCommand
        }
    }.getOrDefault(false)
}
