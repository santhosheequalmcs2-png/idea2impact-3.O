package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.data.repository.SanaPromptEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

enum class AiNetworkHealthStatus {
    UNKNOWN,
    CHECKING,
    CONNECTED,
    NO_INTERNET,
    FIREBASE_UNREACHABLE,
    REQUESTS_BLOCKED,
    APP_CHECK_ISSUE
}

data class PublicDnsProbeResult(
    val reachable: Boolean,
    val host: String = GOOGLE_PUBLIC_DNS_PRIMARY,
    val port: Int = GOOGLE_PUBLIC_DNS_TLS_PORT,
    val latencyMs: Long? = null,
    val detail: String = ""
)

data class NetworkDiagnosticState(
    val health: AiNetworkHealthStatus = AiNetworkHealthStatus.UNKNOWN,
    val streamLabel: String = NetworkDiagnosticManager.LABEL_CONNECTED,
    val statusMessage: String = "Connection ready",
    val detailMessage: String = "",
    val googleDnsReachable: Boolean? = null,
    val firebaseReachable: Boolean? = null,
    val transportLabel: String = "Unknown",
    val isChecking: Boolean = false,
    val lastCheckedAt: String = ""
)

internal const val GOOGLE_PUBLIC_DNS_PRIMARY = "8.8.8.8"
internal const val GOOGLE_PUBLIC_DNS_SECONDARY = "8.8.4.4"
internal const val GOOGLE_PUBLIC_DNS_TLS_PORT = 443
internal const val GOOGLE_PUBLIC_DNS_TCP_PORT = 53

/**
 * Shared connectivity and Firebase reachability diagnostic manager.
 *
 * Performs probes against Google's Public DNS (8.8.8.8 / 8.8.4.4) and Firebase services
 * (`firebaseappcheck.googleapis.com`, `firebasevertexai.googleapis.com`,
 * `firestore.googleapis.com`, `identitytoolkit.googleapis.com`) and publishes updates to a
 * shared [StateFlow] so the AI Sana UI can distinguish general offline states
 * ("Check your internet connection") from Firebase-specific reachability failures
 * ("Firebase server unreachable").
 */
class NetworkDiagnosticManager(
    private val appContext: Context? = null,
    private val connectivityProvider: (Context?) -> DeviceConnectivityState = NetworkDiagnostics::checkDeviceConnectivity,
    private val transportProvider: (Context?) -> String = NetworkDiagnostics::describeActiveTransport,
    private val googleDnsProber: suspend () -> PublicDnsProbeResult = { probeGooglePublicDns() },
    private val firebaseDiagnosticsRunner: suspend (Context?) -> FirebaseDiagnosticsReport = { ctx ->
        NetworkDiagnostics.runFirebaseDiagnostics(
            context = ctx,
            connectivityProvider = connectivityProvider,
            transportProvider = transportProvider
        )
    },
    private val timeLabelProvider: () -> String = { SecurityCryptoUtils.formatTime() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    @Volatile
    private var appContextRef: Context? = appContext?.applicationContext

    private val checkMutex = Mutex()
    private val isMonitoringStarted = AtomicBoolean(false)
    private var registeredCallback: ConnectivityManager.NetworkCallback? = null
    private var debouncedCheckJob: Job? = null

    private val _state = MutableStateFlow(NetworkDiagnosticState())
    val state: StateFlow<NetworkDiagnosticState> = _state.asStateFlow()

    private val _diagnosticsReport = MutableStateFlow(FirebaseDiagnosticsReport())
    val diagnosticsReport: StateFlow<FirebaseDiagnosticsReport> = _diagnosticsReport.asStateFlow()

    fun startMonitoring(context: Context? = appContextRef) {
        val ctx = (context ?: appContextRef)?.applicationContext ?: return
        appContextRef = ctx
        if (!isMonitoringStarted.compareAndSet(false, true)) return

        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            isMonitoringStarted.set(false)
            return
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scheduleDebouncedCheck(ctx, delayMs = 350L)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (!hasInternet) {
                    markOfflineImmediately(transportProvider(ctx))
                } else {
                    scheduleDebouncedCheck(ctx, delayMs = 450L)
                }
            }

            override fun onLost(network: Network) {
                val currentConnectivity = connectivityProvider(ctx)
                if (currentConnectivity == DeviceConnectivityState.OFFLINE) {
                    markOfflineImmediately(transportProvider(ctx))
                } else {
                    scheduleDebouncedCheck(ctx, delayMs = 300L)
                }
            }

            override fun onUnavailable() {
                markOfflineImmediately(transportProvider(ctx))
            }
        }

        try {
            cm.registerDefaultNetworkCallback(callback)
            registeredCallback = callback
        } catch (_: SecurityException) {
            isMonitoringStarted.set(false)
        } catch (_: Exception) {
            isMonitoringStarted.set(false)
        }
    }

    fun stopMonitoring() {
        debouncedCheckJob?.cancel()
        debouncedCheckJob = null
        val ctx = appContextRef ?: appContext?.applicationContext
        val callback = registeredCallback
        registeredCallback = null
        isMonitoringStarted.set(false)
        if (ctx != null && callback != null) {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            runCatching { cm?.unregisterNetworkCallback(callback) }
        }
    }

    fun scheduleCheck(context: Context? = appContextRef) {
        val ctx = context ?: appContextRef
        scope.launch {
            runCatching { checkConnectivity(ctx) }
        }
    }

    private fun scheduleDebouncedCheck(context: Context, delayMs: Long) {
        debouncedCheckJob?.cancel()
        debouncedCheckJob = scope.launch {
            delay(delayMs)
            runCatching { checkConnectivity(context) }
        }
    }

    private fun markOfflineImmediately(transportLabel: String) {
        val checkedAt = timeLabelProvider()
        _state.update { current ->
            current.copy(
                health = AiNetworkHealthStatus.NO_INTERNET,
                streamLabel = LABEL_NO_INTERNET,
                statusMessage = STATUS_CHECK_INTERNET,
                detailMessage = "No active internet connection ($transportLabel). Enable Wi-Fi or mobile data.",
                googleDnsReachable = false,
                firebaseReachable = false,
                transportLabel = transportLabel,
                isChecking = false,
                lastCheckedAt = checkedAt
            )
        }
        _diagnosticsReport.update { current ->
            val offlineServices = (listOf(
                ServiceCheckResult(
                    id = "google_public_dns",
                    name = "Google Public DNS",
                    host = GOOGLE_PUBLIC_DNS_PRIMARY,
                    status = DiagnosticStatus.OFFLINE,
                    badgeText = "Offline",
                    detail = "Device has no active internet connection; cannot reach $GOOGLE_PUBLIC_DNS_PRIMARY."
                )
            ) + NetworkDiagnostics.CORE_FIREBASE_ENDPOINTS.map { endpoint ->
                ServiceCheckResult(
                    id = endpoint.id,
                    name = endpoint.name,
                    host = endpoint.host,
                    status = DiagnosticStatus.OFFLINE,
                    badgeText = "Offline",
                    detail = "Device has no active internet connection; cannot reach ${endpoint.host}."
                )
            })
            current.copy(
                isRunning = false,
                hasRun = true,
                connectivityState = DeviceConnectivityState.OFFLINE,
                transportLabel = transportLabel,
                overallStatus = DiagnosticStatus.OFFLINE,
                headline = STATUS_CHECK_INTERNET,
                errorBanner = "No active internet connection ($transportLabel). Google Public DNS and Firebase services cannot be reached.",
                actionHint = "Turn on Wi-Fi or mobile data, disable Airplane Mode, then tap Re-check.",
                serviceChecks = offlineServices,
                checkedAtTimestamp = checkedAt
            )
        }
    }

    suspend fun checkConnectivity(context: Context? = appContextRef): NetworkDiagnosticState = checkMutex.withLock {
        val resolvedContext = context ?: appContextRef
        if (resolvedContext != null && appContextRef == null) {
            appContextRef = resolvedContext.applicationContext
        }
        val checkedAt = timeLabelProvider()
        val transport = transportProvider(resolvedContext)

        _state.update { current ->
            current.copy(
                health = AiNetworkHealthStatus.CHECKING,
                streamLabel = LABEL_CHECKING_CONNECTION,
                statusMessage = LABEL_CHECKING_CONNECTION,
                isChecking = true
            )
        }
        _diagnosticsReport.update { current ->
            current.copy(
                isRunning = true,
                errorBanner = null,
                actionHint = "Checking Google Public DNS (8.8.8.8), Firebase HTTPS endpoints, and App Check..."
            )
        }

        val connectivity = connectivityProvider(resolvedContext)
        if (connectivity == DeviceConnectivityState.OFFLINE) {
            markOfflineImmediately(transport)
            return@withLock _state.value
        }

        val (dnsProbe, firebaseReport) = coroutineScope {
            val dnsDeferred = async(Dispatchers.IO) { googleDnsProber() }
            val firebaseDeferred = async(Dispatchers.IO) { firebaseDiagnosticsRunner(resolvedContext) }
            dnsDeferred.await() to firebaseDeferred.await()
        }

        val dnsServiceCheck = if (dnsProbe.reachable) {
            val latencyText = dnsProbe.latencyMs?.let { " (${it}ms)" }.orEmpty()
            ServiceCheckResult(
                id = "google_public_dns",
                name = "Google Public DNS",
                host = "${dnsProbe.host}:${dnsProbe.port}",
                status = DiagnosticStatus.PASS,
                badgeText = "Reachable$latencyText",
                detail = dnsProbe.detail.ifBlank {
                    "Internet verified via TCP connect to ${dnsProbe.host}:${dnsProbe.port}."
                },
                latencyMs = dnsProbe.latencyMs
            )
        } else {
            ServiceCheckResult(
                id = "google_public_dns",
                name = "Google Public DNS",
                host = "${dnsProbe.host}:${dnsProbe.port}",
                status = DiagnosticStatus.ERROR,
                badgeText = "Unreachable",
                detail = dnsProbe.detail.ifBlank {
                    "Could not reach Google Public DNS (${dnsProbe.host})."
                },
                latencyMs = dnsProbe.latencyMs
            )
        }

        val combinedChecks = listOf(dnsServiceCheck) + firebaseReport.serviceChecks
        val firebaseServices = firebaseReport.serviceChecks
        val anyFirebaseReachable = firebaseServices.any { it.status == DiagnosticStatus.PASS }
        val blockedFirebase = firebaseServices.filter { it.status == DiagnosticStatus.BLOCKED }
        val unreachableFirebase = firebaseServices.filter {
            it.status == DiagnosticStatus.ERROR || it.status == DiagnosticStatus.OFFLINE
        }
        val allFirebaseReachable = firebaseServices.isNotEmpty() && unreachableFirebase.isEmpty() && blockedFirebase.isEmpty()

        val nextState: NetworkDiagnosticState
        val enrichedReport: FirebaseDiagnosticsReport

        when {
            !dnsProbe.reachable && !anyFirebaseReachable && blockedFirebase.isEmpty() -> {
                val detail = "Device reports $transport, but neither Google Public DNS (${dnsProbe.host}) nor Firebase endpoints responded."
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.NO_INTERNET,
                    streamLabel = LABEL_NO_INTERNET,
                    statusMessage = STATUS_CHECK_INTERNET,
                    detailMessage = detail,
                    googleDnsReachable = false,
                    firebaseReachable = false,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    overallStatus = DiagnosticStatus.OFFLINE,
                    headline = STATUS_CHECK_INTERNET,
                    errorBanner = detail,
                    actionHint = "Verify Wi-Fi router or cellular data access, disable Airplane Mode, and tap Re-check.",
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }

            blockedFirebase.isNotEmpty() -> {
                val blockedNames = blockedFirebase.joinToString(", ") { it.name }
                val detail = "Internet ($transport) is active, but HTTPS requests to $blockedNames were blocked."
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.REQUESTS_BLOCKED,
                    streamLabel = LABEL_REQUESTS_BLOCKED,
                    statusMessage = STATUS_FIREBASE_BLOCKED,
                    detailMessage = detail,
                    googleDnsReachable = dnsProbe.reachable,
                    firebaseReachable = false,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    overallStatus = DiagnosticStatus.BLOCKED,
                    headline = STATUS_FIREBASE_BLOCKED,
                    errorBanner = firebaseReport.errorBanner ?: detail,
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }

            unreachableFirebase.isNotEmpty() -> {
                val downNames = unreachableFirebase.joinToString(", ") { it.name }
                val detail = if (dnsProbe.reachable) {
                    "Internet connection works (Google DNS ${dnsProbe.host} reachable), but $downNames could not be reached."
                } else {
                    "Firebase service check failed for $downNames on $transport."
                }
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.FIREBASE_UNREACHABLE,
                    streamLabel = LABEL_FIREBASE_UNREACHABLE,
                    statusMessage = STATUS_FIREBASE_UNREACHABLE,
                    detailMessage = detail,
                    googleDnsReachable = dnsProbe.reachable,
                    firebaseReachable = false,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    overallStatus = DiagnosticStatus.ERROR,
                    headline = STATUS_FIREBASE_UNREACHABLE,
                    errorBanner = detail,
                    actionHint = if (dnsProbe.reachable) {
                        "General internet is online; a DNS filter, firewall, VPN, or temporary Firebase outage may be blocking Firebase hosts."
                    } else {
                        firebaseReport.actionHint
                    },
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }

            firebaseReport.overallStatus == DiagnosticStatus.ERROR -> {
                val detail = firebaseReport.errorBanner
                    ?: "Firebase endpoints responded, but App Check rejected the attestation request."
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.APP_CHECK_ISSUE,
                    streamLabel = LABEL_APP_CHECK_ISSUE,
                    statusMessage = STATUS_APP_CHECK_ISSUE,
                    detailMessage = detail,
                    googleDnsReachable = dnsProbe.reachable,
                    firebaseReachable = true,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }

            allFirebaseReachable -> {
                val detail = if (dnsProbe.reachable) {
                    "Google Public DNS and all Firebase services are reachable ($transport)."
                } else {
                    "Firebase services are reachable ($transport), though Google Public DNS TCP probe timed out."
                }
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.CONNECTED,
                    streamLabel = LABEL_CONNECTED,
                    statusMessage = STATUS_CONNECTED,
                    detailMessage = detail,
                    googleDnsReachable = dnsProbe.reachable,
                    firebaseReachable = true,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }

            else -> {
                nextState = NetworkDiagnosticState(
                    health = AiNetworkHealthStatus.UNKNOWN,
                    streamLabel = LABEL_CONNECTED,
                    statusMessage = firebaseReport.headline,
                    detailMessage = firebaseReport.actionHint,
                    googleDnsReachable = dnsProbe.reachable,
                    firebaseReachable = anyFirebaseReachable,
                    transportLabel = transport,
                    isChecking = false,
                    lastCheckedAt = checkedAt
                )
                enrichedReport = firebaseReport.copy(
                    isRunning = false,
                    hasRun = true,
                    serviceChecks = combinedChecks,
                    checkedAtTimestamp = checkedAt
                )
            }
        }

        _diagnosticsReport.value = enrichedReport
        _state.value = nextState
        nextState
    }

    companion object {
        const val LABEL_CHECKING_CONNECTION = "Checking Connection"
        const val LABEL_NO_INTERNET = "No Internet"
        const val LABEL_FIREBASE_UNREACHABLE = "Firebase Server Unreachable"
        const val LABEL_REQUESTS_BLOCKED = "Requests Blocked"
        const val LABEL_APP_CHECK_ISSUE = "App Check Issue"
        const val LABEL_CONNECTED = "Connected"

        const val STATUS_CHECK_INTERNET = "Check your internet connection"
        const val STATUS_FIREBASE_UNREACHABLE = "Firebase server unreachable"
        const val STATUS_FIREBASE_BLOCKED = "Firebase requests blocked"
        const val STATUS_APP_CHECK_ISSUE = "App Check verification failed"
        const val STATUS_CONNECTED = "Connected · Firebase & AI ready"

        @Volatile
        private var SHARED_INSTANCE: NetworkDiagnosticManager? = null

        fun getInstance(context: Context? = null): NetworkDiagnosticManager {
            val appCtx = context?.applicationContext
            val existing = SHARED_INSTANCE ?: synchronized(this) {
                SHARED_INSTANCE ?: NetworkDiagnosticManager(appContext = appCtx).also {
                    SHARED_INSTANCE = it
                }
            }
            if (appCtx != null && existing.appContextRef == null) {
                existing.appContextRef = appCtx
            }
            return existing
        }

        suspend fun probeGooglePublicDns(
            timeoutMs: Int = 2_000,
            clockMs: () -> Long = { System.currentTimeMillis() }
        ): PublicDnsProbeResult = withContext(Dispatchers.IO) {
            val targets = listOf(
                GOOGLE_PUBLIC_DNS_PRIMARY to GOOGLE_PUBLIC_DNS_TLS_PORT,
                GOOGLE_PUBLIC_DNS_PRIMARY to GOOGLE_PUBLIC_DNS_TCP_PORT,
                GOOGLE_PUBLIC_DNS_SECONDARY to GOOGLE_PUBLIC_DNS_TCP_PORT
            )
            var lastReason = "Google Public DNS unreachable"
            for ((host, port) in targets) {
                val startMs = clockMs()
                var socket: Socket? = null
                try {
                    socket = Socket()
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    val latency = (clockMs() - startMs).coerceAtLeast(1L)
                    return@withContext PublicDnsProbeResult(
                        reachable = true,
                        host = host,
                        port = port,
                        latencyMs = latency,
                        detail = "Google Public DNS ($host:$port) responded in ${latency}ms."
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastReason = SanaPromptEngine.redactSensitiveInfo(
                        e.message ?: e.javaClass.simpleName
                    )
                } finally {
                    runCatching { socket?.close() }
                }
            }
            PublicDnsProbeResult(
                reachable = false,
                host = GOOGLE_PUBLIC_DNS_PRIMARY,
                port = GOOGLE_PUBLIC_DNS_TLS_PORT,
                detail = lastReason
            )
        }
    }
}
