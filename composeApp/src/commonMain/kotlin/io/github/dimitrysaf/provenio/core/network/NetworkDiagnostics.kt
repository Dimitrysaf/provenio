package io.github.dimitrysaf.provenio.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/** What kind of link a network interface is, guessed from its name. */
enum class NetworkKind { WiFi, Ethernet, Cellular, Vpn, Other }

/** One active network interface and the addresses this device has on it. */
data class NetworkConnectionInfo(
    val name: String,
    val kind: NetworkKind,
    val ipv4: List<String>,
    val ipv6: List<String>,
)

/** The address the internet sees this device at, with where Cloudflare places it. */
data class PublicIpInfo(
    val ip: String,
    val country: String?,
    val dataCenter: String?,
)

sealed interface PublicIpState {
    data object Idle : PublicIpState
    data object Loading : PublicIpState
    data class Loaded(val info: PublicIpInfo) : PublicIpState
    data object Failed : PublicIpState
}

enum class SpeedTestPhase { Latency, Download, Upload }

/** A speed test's measurements; each is null until its phase has run. */
data class SpeedTestResult(
    val latencyMs: Int? = null,
    val jitterMs: Int? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
)

sealed interface SpeedTestState {
    val result: SpeedTestResult

    data object Idle : SpeedTestState {
        override val result = SpeedTestResult()
    }

    data class Running(
        val phase: SpeedTestPhase,
        val progress: Float,
        override val result: SpeedTestResult,
    ) : SpeedTestState

    data class Done(override val result: SpeedTestResult) : SpeedTestState

    data class Failed(override val result: SpeedTestResult) : SpeedTestState
}

/** The network access the diagnostics need, which only the JVM platforms provide. */
internal expect object NetworkDiagnosticsPlatform {
    val isSupported: Boolean

    /** The active, non-loopback interfaces, with their IPv4 and global IPv6 addresses. */
    fun connections(): List<NetworkConnectionInfo>

    suspend fun getText(url: String): String

    /** How long a request for [url] takes to answer, in milliseconds. */
    suspend fun timeRequest(url: String): Long

    /**
     * Downloads [url] until it ends or [maxMillis] pass, reporting bytes and milliseconds so far.
     * Returns the bytes read and the milliseconds taken.
     */
    suspend fun download(url: String, maxMillis: Long, onProgress: (Long, Long) -> Unit): Pair<Long, Long>

    /**
     * Uploads up to [totalBytes] of filler to [url], stopping early after [maxMillis]. Returns the
     * bytes sent and the milliseconds taken.
     */
    suspend fun upload(url: String, totalBytes: Long, maxMillis: Long, onProgress: (Long, Long) -> Unit): Pair<Long, Long>
}

/**
 * The Network page's facts: which connections are up and their addresses, the public address on
 * request, and a speed test against Cloudflare's speed test servers.
 */
object NetworkDiagnosticsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val isSupported: Boolean get() = NetworkDiagnosticsPlatform.isSupported

    private val _connections = MutableStateFlow<List<NetworkConnectionInfo>>(emptyList())
    val connections: StateFlow<List<NetworkConnectionInfo>> = _connections.asStateFlow()

    private val _publicIp = MutableStateFlow<PublicIpState>(PublicIpState.Idle)
    val publicIp: StateFlow<PublicIpState> = _publicIp.asStateFlow()

    private val _speedTest = MutableStateFlow<SpeedTestState>(SpeedTestState.Idle)
    val speedTest: StateFlow<SpeedTestState> = _speedTest.asStateFlow()

    private var speedTestJob: Job? = null

    fun refreshConnections() {
        scope.launch {
            _connections.value = runCatching { NetworkDiagnosticsPlatform.connections() }.getOrDefault(emptyList())
        }
    }

    /** Asks Cloudflare which address this device reaches the internet from. */
    fun lookUpPublicIp() {
        if (_publicIp.value == PublicIpState.Loading) return
        _publicIp.value = PublicIpState.Loading
        scope.launch {
            _publicIp.value = runCatching {
                parseTrace(NetworkDiagnosticsPlatform.getText(TraceUrl))
            }.getOrNull()?.let { PublicIpState.Loaded(it) } ?: PublicIpState.Failed
        }
    }

    fun runSpeedTest() {
        if (speedTestJob?.isActive == true) return
        speedTestJob = scope.launch {
            var result = SpeedTestResult()
            try {
                _speedTest.value = SpeedTestState.Running(SpeedTestPhase.Latency, 0f, result)
                result = measureLatency(result)

                _speedTest.value = SpeedTestState.Running(SpeedTestPhase.Download, LatencyShare, result)
                val (downBytes, downMillis) = NetworkDiagnosticsPlatform.download(DownloadUrl, PhaseMillis) { bytes, millis ->
                    _speedTest.value = SpeedTestState.Running(
                        SpeedTestPhase.Download,
                        LatencyShare + DownloadShare * phaseProgress(millis),
                        result.copy(downloadMbps = mbps(bytes, millis)),
                    )
                }
                result = result.copy(downloadMbps = mbps(downBytes, downMillis))

                val uploadStart = LatencyShare + DownloadShare
                _speedTest.value = SpeedTestState.Running(SpeedTestPhase.Upload, uploadStart, result)
                val (upBytes, upMillis) = NetworkDiagnosticsPlatform.upload(UploadUrl, UploadBytes, PhaseMillis) { bytes, millis ->
                    _speedTest.value = SpeedTestState.Running(
                        SpeedTestPhase.Upload,
                        uploadStart + (1f - uploadStart) * phaseProgress(millis),
                        result.copy(uploadMbps = mbps(bytes, millis)),
                    )
                }
                result = result.copy(uploadMbps = mbps(upBytes, upMillis))
                _speedTest.value = SpeedTestState.Done(result)
            } catch (cancelled: CancellationException) {
                _speedTest.value = if (result == SpeedTestResult()) SpeedTestState.Idle else SpeedTestState.Done(result)
                throw cancelled
            } catch (_: Throwable) {
                _speedTest.value = SpeedTestState.Failed(result)
            }
        }
    }

    fun cancelSpeedTest() {
        speedTestJob?.cancel()
    }

    private suspend fun measureLatency(result: SpeedTestResult): SpeedTestResult {
        // The first request also sets up the connection, so it is left out.
        val samples = (0..LatencySamples).map { NetworkDiagnosticsPlatform.timeRequest(LatencyUrl) }.drop(1)
        val sorted = samples.sorted()
        val median = sorted[sorted.size / 2]
        val jitter = samples.zipWithNext { a, b -> abs(a - b) }.average()
        return result.copy(latencyMs = median.toInt(), jitterMs = jitter.toInt())
    }

    private fun phaseProgress(millis: Long): Float = (millis.toFloat() / PhaseMillis).coerceIn(0f, 1f)

    private fun mbps(bytes: Long, millis: Long): Double? =
        if (bytes <= 0 || millis <= 0) null else bytes * 8.0 / (millis / 1000.0) / 1_000_000.0

    private fun parseTrace(body: String): PublicIpInfo? {
        val fields = body.lineSequence()
            .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }
            .toMap()
        val ip = fields["ip"]?.takeIf(String::isNotBlank) ?: return null
        return PublicIpInfo(ip = ip, country = fields["loc"], dataCenter = fields["colo"])
    }

    private const val TraceUrl = "https://www.cloudflare.com/cdn-cgi/trace"
    private const val LatencyUrl = "https://speed.cloudflare.com/__down?bytes=0"
    private const val DownloadUrl = "https://speed.cloudflare.com/__down?bytes=100000000"
    private const val UploadUrl = "https://speed.cloudflare.com/__up"
    private const val UploadBytes = 25_000_000L
    private const val LatencySamples = 6
    private const val PhaseMillis = 8_000L
    private const val LatencyShare = 0.1f
    private const val DownloadShare = 0.45f
}
