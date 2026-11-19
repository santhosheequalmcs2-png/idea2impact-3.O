package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.model.AiFailureCategory
import com.example.data.repository.SanaPromptEngine
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

enum class DeviceConnectivityState {
    ONLINE_VALIDATED,
    ONLINE_UNVALIDATED,
    OFFLINE,
    UNKNOWN
}

sealed interface FirebaseHttpsProbeResult {
    data class Reachable(val httpCode: Int) : FirebaseHttpsProbeResult
    data class DnsOrSocketUnreachable(val reason: String, val cause: Throwable? = null) : FirebaseHttpsProbeResult
    data class TlsOrHttpsBlocked(val reason: String, val cause: Throwable? = null) : FirebaseHttpsProbeResult
}

data class AppCheckProbeOutcome(
    val statusLabel: String,
    val abortCategory: AiFailureCategory? = null,
    val detail: String = "",
    val cause: Throwable? = null
)

enum class DiagnosticStatus {
    CHECKING,
    PASS,
    WARN,
    OFFLINE,
    BLOCKED,
    ERROR
}

data class FirebaseServiceEndpoint(
    val id: String,
    val name: String,
    val host: String,
    val url: String
)

data class ServiceCheckResult(
    val id: String,
    val name: String,
    val host: String,
    val status: DiagnosticStatus,
    val badgeText: String,
    val detail: String,
    val latencyMs: Long? = null
)

data class FirebaseDiagnosticsReport(
    val isRunning: Boolean = false,
    val hasRun: Boolean = false,
    val connectivityState: DeviceConnectivityState = DeviceConnectivityState.UNKNOWN,
    val transportLabel: String = "Not checked",
    val overallStatus: DiagnosticStatus = DiagnosticStatus.CHECKING,
    val headline: String = "Connectivity check not run yet",
    val errorBanner: String? = null,
    val actionHint: String = "Tap Check Network to test device internet and Firebase reachability.",
    val appCheckSummary: String = "",
    val serviceChecks: List<ServiceCheckResult> = emptyList(),
    val checkedAtTimestamp: String = ""
)

object NetworkDiagnostics {

    private const val FIREBASE_APP_CHECK_PROBE_URL = "https://firebaseappcheck.googleapis.com/"
    private const val APP_CHECK_TIMEOUT_MS = 3_500L

    val CORE_FIREBASE_ENDPOINTS: List<FirebaseServiceEndpoint> = listOf(
        FirebaseServiceEndpoint(
            id = "firebase_app_check",
            name = "Firebase App Check",
            host = "firebaseappcheck.googleapis.com",
            url = "https://firebaseappcheck.googleapis.com/"
        ),
        FirebaseServiceEndpoint(
            id = "firebase_ai",
            name = "Firebase AI Logic",
            host = "firebasevertexai.googleapis.com",
            url = "https://firebasevertexai.googleapis.com/"
        ),
        FirebaseServiceEndpoint(
            id = "firebase_firestore",
            name = "Cloud Firestore",
            host = "firestore.googleapis.com",
            url = "https://firestore.googleapis.com/"
        ),
        FirebaseServiceEndpoint(
            id = "firebase_auth",
            name = "Firebase Auth",
            host = "identitytoolkit.googleapis.com",
            url = "https://identitytoolkit.googleapis.com/"
        )
    )

    fun checkDeviceConnectivity(context: Context?): DeviceConnectivityState {
        if (context == null) return DeviceConnectivityState.UNKNOWN
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return DeviceConnectivityState.UNKNOWN
            val activeNetwork = cm.activeNetwork ?: return DeviceConnectivityState.OFFLINE
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return DeviceConnectivityState.OFFLINE
            val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            if (!hasInternet) return DeviceConnectivityState.OFFLINE
            val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (isValidated) {
                DeviceConnectivityState.ONLINE_VALIDATED
            } else {
                DeviceConnectivityState.ONLINE_UNVALIDATED
            }
        } catch (_: SecurityException) {
            DeviceConnectivityState.UNKNOWN
        } catch (_: Exception) {
            DeviceConnectivityState.UNKNOWN
        }
    }

    fun describeActiveTransport(context: Context?): String {
        if (context == null) return "Unknown"
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Unknown"
            val activeNetwork = cm.activeNetwork ?: return "No Active Network"
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "No Network Capabilities"
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                return "Offline (No Internet Capability)"
            }
            val transports = buildList {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) add("Wi-Fi")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) add("Mobile Data")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) add("Ethernet")
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) add("VPN")
            }
            if (transports.isEmpty()) "Network Active" else transports.joinToString(" + ")
        } catch (_: Exception) {
            "Unknown"
        }
    }

    suspend fun probeFirebaseHttps(
        targetUrl: String = FIREBASE_APP_CHECK_PROBE_URL,
        timeoutMs: Int = 2_500
    ): FirebaseHttpsProbeResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(targetUrl)
            connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                useCaches = false
            }
            val code = connection.responseCode
            // Any HTTP response code from Google/Firebase (including 403/404) proves DNS, TCP, and TLS succeeded.
            val location = connection.getHeaderField("Location").orEmpty()
            val isGoogleRedirect = location.contains("googleapis.com") ||
                location.contains("google.com") ||
                location.contains("gstatic.com")
            if (code in 300..399 && location.isNotBlank() && !isGoogleRedirect) {
                FirebaseHttpsProbeResult.TlsOrHttpsBlocked("Unexpected redirect (HTTP $code) from captive portal or proxy")
            } else {
                FirebaseHttpsProbeResult.Reachable(code)
            }
        } catch (e: SSLHandshakeException) {
            FirebaseHttpsProbeResult.TlsOrHttpsBlocked(
                SanaPromptEngine.redactSensitiveInfo(e.message ?: "TLS handshake failed"),
                e
            )
        } catch (e: SSLException) {
            FirebaseHttpsProbeResult.TlsOrHttpsBlocked(
                SanaPromptEngine.redactSensitiveInfo(e.message ?: "SSL error"),
                e
            )
        } catch (e: UnknownHostException) {
            FirebaseHttpsProbeResult.DnsOrSocketUnreachable("DNS lookup failed for Firebase host", e)
        } catch (e: ConnectException) {
            FirebaseHttpsProbeResult.DnsOrSocketUnreachable("Connection refused to Firebase host", e)
        } catch (e: NoRouteToHostException) {
            FirebaseHttpsProbeResult.DnsOrSocketUnreachable("No route to Firebase host", e)
        } catch (e: SocketTimeoutException) {
            FirebaseHttpsProbeResult.DnsOrSocketUnreachable("Timed out reaching Firebase over HTTPS", e)
        } catch (e: Exception) {
            FirebaseHttpsProbeResult.DnsOrSocketUnreachable(
                SanaPromptEngine.redactSensitiveInfo(e.message ?: e.javaClass.simpleName),
                e
            )
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    suspend fun runFirebaseDiagnostics(
        context: Context?,
        endpoints: List<FirebaseServiceEndpoint> = CORE_FIREBASE_ENDPOINTS,
        connectivityProvider: (Context?) -> DeviceConnectivityState = ::checkDeviceConnectivity,
        transportProvider: (Context?) -> String = ::describeActiveTransport,
        httpsProber: suspend (String) -> FirebaseHttpsProbeResult = { url -> probeFirebaseHttps(url, timeoutMs = 3_000) },
        appCheckProber: suspend (DeviceConnectivityState) -> AppCheckProbeOutcome = { connectivity ->
            probeAppCheckForDiagnostics(context, connectivity)
        },
        initStatusProvider: () -> AppCheckInitStatus = { lastAppCheckInitStatus },
        timeLabelProvider: () -> String = { SecurityCryptoUtils.formatTime() },
        clockMs: () -> Long = { System.currentTimeMillis() }
    ): FirebaseDiagnosticsReport = coroutineScope {
        val connectivity = connectivityProvider(context)
        val transport = transportProvider(context)
        val initStatus = initStatusProvider()
        val checkedAt = timeLabelProvider()

        if (connectivity == DeviceConnectivityState.OFFLINE) {
            val offlineChecks = endpoints.map { endpoint ->
                ServiceCheckResult(
                    id = endpoint.id,
                    name = endpoint.name,
                    host = endpoint.host,
                    status = DiagnosticStatus.OFFLINE,
                    badgeText = "Offline",
                    detail = "Device has no active internet connection; cannot reach ${endpoint.host}."
                )
            }
            return@coroutineScope FirebaseDiagnosticsReport(
                isRunning = false,
                hasRun = true,
                connectivityState = connectivity,
                transportLabel = transport,
                overallStatus = DiagnosticStatus.OFFLINE,
                headline = "Device Is Offline",
                errorBanner = "No active internet connection ($transport). Firebase AI Logic, Firestore, Auth, and App Check cannot connect.",
                actionHint = "Turn on Wi-Fi or mobile data, disable Airplane Mode, then tap Re-check.",
                appCheckSummary = "Skipped while offline (${initStatus.buildType} · ${initStatus.provider})",
                serviceChecks = offlineChecks,
                checkedAtTimestamp = checkedAt
            )
        }

        val httpsDeferred = endpoints.map { endpoint ->
            async(Dispatchers.IO) {
                val start = clockMs()
                val probe = httpsProber(endpoint.url)
                val latency = (clockMs() - start).coerceAtLeast(1L)
                endpoint to (probe to latency)
            }
        }
        val appCheckDeferred = async(Dispatchers.IO) {
            appCheckProber(connectivity)
        }

        val probeResults = httpsDeferred.awaitAll()
        val primaryHttpsResult = probeResults.firstOrNull()?.second?.first
        val rawAppCheckOutcome = appCheckDeferred.await()
        val appCheckOutcome = if (
            rawAppCheckOutcome.cause != null &&
            rawAppCheckOutcome.abortCategory == null
        ) {
            classifyFirebaseProbeException(
                error = rawAppCheckOutcome.cause,
                connectivity = connectivity,
                httpsProbe = primaryHttpsResult
            )
        } else {
            rawAppCheckOutcome
        }

        val serviceChecks = probeResults.map { (endpoint, pair) ->
            val (probe, latency) = pair
            when (probe) {
                is FirebaseHttpsProbeResult.Reachable -> ServiceCheckResult(
                    id = endpoint.id,
                    name = endpoint.name,
                    host = endpoint.host,
                    status = DiagnosticStatus.PASS,
                    badgeText = "Reachable (${latency}ms)",
                    detail = "DNS, TCP, and TLS verified to ${endpoint.host} (HTTP ${probe.httpCode}).",
                    latencyMs = latency
                )
                is FirebaseHttpsProbeResult.TlsOrHttpsBlocked -> ServiceCheckResult(
                    id = endpoint.id,
                    name = endpoint.name,
                    host = endpoint.host,
                    status = DiagnosticStatus.BLOCKED,
                    badgeText = "Blocked",
                    detail = "HTTPS request to ${endpoint.host} was blocked: ${probe.reason}",
                    latencyMs = latency
                )
                is FirebaseHttpsProbeResult.DnsOrSocketUnreachable -> ServiceCheckResult(
                    id = endpoint.id,
                    name = endpoint.name,
                    host = endpoint.host,
                    status = DiagnosticStatus.ERROR,
                    badgeText = "Unreachable",
                    detail = "Failed to reach ${endpoint.host}: ${probe.reason}",
                    latencyMs = latency
                )
            }
        }

        val blockedChecks = serviceChecks.filter { it.status == DiagnosticStatus.BLOCKED }
        val unreachableChecks = serviceChecks.filter { it.status == DiagnosticStatus.ERROR }
        val reachableChecks = serviceChecks.filter { it.status == DiagnosticStatus.PASS }

        val appCheckFailed = appCheckOutcome.abortCategory != null ||
            appCheckOutcome.statusLabel.startsWith("rejected:") ||
            appCheckOutcome.statusLabel == "empty_token"

        val appCheckSummary = buildString {
            append("${initStatus.buildType.uppercase()} · ${initStatus.projectId} · ")
            append(
                when {
                    appCheckOutcome.statusLabel == "token_ready" -> "Token Ready"
                    appCheckOutcome.statusLabel == "not_initialized" -> "FirebaseApp not initialized"
                    appCheckOutcome.statusLabel.startsWith("rejected:") -> "Rejected (${appCheckOutcome.statusLabel.removePrefix("rejected:")})"
                    appCheckOutcome.statusLabel == "https_blocked" -> "Blocked over HTTPS"
                    appCheckOutcome.statusLabel == "network_unreachable" -> "Network unreachable"
                    appCheckOutcome.statusLabel.startsWith("probe_timeout") -> "Timed out"
                    else -> appCheckOutcome.statusLabel
                }
            )
        }

        val (overallStatus, headline, errorBanner, actionHint) = when {
            blockedChecks.isNotEmpty() -> {
                val hosts = blockedChecks.joinToString(", ") { it.name }
                Quad(
                    DiagnosticStatus.BLOCKED,
                    "Firebase Requests Blocked",
                    "HTTPS requests to $hosts were blocked (${blockedChecks.first().detail}).",
                    "Complete any Wi-Fi captive portal sign-in, disable interfering VPN/Private DNS, or switch networks and tap Re-check."
                )
            }
            unreachableChecks.size == serviceChecks.size && serviceChecks.isNotEmpty() -> {
                Quad(
                    DiagnosticStatus.OFFLINE,
                    "No Internet Access to Firebase",
                    "Device reports $transport, but all Firebase endpoints are unreachable (${unreachableChecks.first().detail}).",
                    "Check router/carrier internet access, switch between Wi-Fi and mobile data, and tap Re-check."
                )
            }
            unreachableChecks.isNotEmpty() -> {
                val names = unreachableChecks.joinToString(", ") { it.name }
                Quad(
                    DiagnosticStatus.ERROR,
                    "Partial Firebase Reachability Failure",
                    "Reachable: ${reachableChecks.size}/${serviceChecks.size}. Unreachable: $names.",
                    "A firewall, DNS filter, or regional route may be blocking specific Firebase hosts. Try switching networks."
                )
            }
            appCheckOutcome.abortCategory == AiFailureCategory.APP_CHECK_OR_AUTH || appCheckFailed -> {
                val buildGuidance = if (initStatus.buildType == "debug") {
                    "For debug builds on project ${initStatus.projectId}, copy the 'DebugAppCheckProvider' UUID from Logcat into Firebase Console > App Check > Apps > Manage debug tokens."
                } else {
                    "For release builds on project ${initStatus.projectId}, verify the reCAPTCHA Enterprise site key and Play App Signing SHA-256 in Firebase Console."
                }
                val safeDetail = SanaPromptEngine.redactSensitiveInfo(appCheckOutcome.detail).ifBlank {
                    appCheckOutcome.statusLabel
                }
                Quad(
                    DiagnosticStatus.ERROR,
                    "Firebase Reachable · App Check Rejected",
                    "All ${serviceChecks.size} Firebase HTTPS endpoints responded, but App Check attestation failed ($safeDetail).",
                    buildGuidance
                )
            }
            connectivity == DeviceConnectivityState.ONLINE_UNVALIDATED -> {
                Quad(
                    DiagnosticStatus.WARN,
                    "Connected (Unvalidated Network)",
                    "Firebase endpoints responded, though Android flagged $transport as unvalidated (possible captive portal or restricted DNS).",
                    "If requests stall later, confirm your Wi-Fi or cellular data plan has full internet access."
                )
            }
            !initStatus.initialized -> {
                Quad(
                    DiagnosticStatus.WARN,
                    "Firebase Reachable · App Check Not Ready",
                    "Network and Firebase HTTPS endpoints are reachable, but App Check reported '${initStatus.tokenSource}'.",
                    "Verify App Check provider setup (${initStatus.provider}) for project ${initStatus.projectId}."
                )
            }
            else -> {
                Quad(
                    DiagnosticStatus.PASS,
                    "All Firebase Services Reachable",
                    null,
                    "Device network ($transport) and all ${serviceChecks.size} Firebase endpoints verified."
                )
            }
        }

        FirebaseDiagnosticsReport(
            isRunning = false,
            hasRun = true,
            connectivityState = connectivity,
            transportLabel = transport,
            overallStatus = overallStatus,
            headline = headline,
            errorBanner = errorBanner,
            actionHint = actionHint,
            appCheckSummary = appCheckSummary,
            serviceChecks = serviceChecks,
            checkedAtTimestamp = checkedAt
        )
    }

    private suspend fun probeAppCheckForDiagnostics(
        context: Context?,
        connectivity: DeviceConnectivityState
    ): AppCheckProbeOutcome {
        val hasFirebaseApp = context != null && runCatching { FirebaseApp.getApps(context).isNotEmpty() }.getOrDefault(false)
        if (!hasFirebaseApp) {
            return AppCheckProbeOutcome(
                statusLabel = "not_initialized",
                detail = "FirebaseApp is not initialized in this context."
            )
        }
        return try {
            val tokenResult = withTimeoutOrNull(APP_CHECK_TIMEOUT_MS) {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            }
            when {
                tokenResult == null -> AppCheckProbeOutcome(
                    statusLabel = "probe_timeout_${APP_CHECK_TIMEOUT_MS}ms",
                    detail = "App Check token fetch timed out after ${APP_CHECK_TIMEOUT_MS}ms."
                )
                tokenResult.token.isNotBlank() -> AppCheckProbeOutcome(statusLabel = "token_ready")
                else -> AppCheckProbeOutcome(
                    statusLabel = "empty_token",
                    detail = "App Check returned a blank token."
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            val httpsProbe = probeFirebaseHttps(FIREBASE_APP_CHECK_PROBE_URL)
            classifyFirebaseProbeException(e, connectivity, httpsProbe)
        }
    }

    fun classifyFirebaseProbeException(
        error: Throwable,
        connectivity: DeviceConnectivityState,
        httpsProbe: FirebaseHttpsProbeResult?
    ): AppCheckProbeOutcome {
        val redactedChain = collectSafeExceptionSummary(error)
        val lower = redactedChain.lowercase()

        if (connectivity == DeviceConnectivityState.OFFLINE) {
            return AppCheckProbeOutcome(
                statusLabel = "device_offline",
                abortCategory = AiFailureCategory.NETWORK_OFFLINE,
                detail = "No active internet connection on device ($redactedChain)",
                cause = error
            )
        }

        if (httpsProbe is FirebaseHttpsProbeResult.TlsOrHttpsBlocked ||
            hasCauseOf<SSLHandshakeException>(error) ||
            hasCauseOf<SSLException>(error) ||
            lower.contains("sslhandshake") ||
            lower.contains("certpathvalidatorexception") ||
            lower.contains("trust anchor") ||
            lower.contains("captive portal")
        ) {
            val blockReason = (httpsProbe as? FirebaseHttpsProbeResult.TlsOrHttpsBlocked)?.reason ?: redactedChain
            return AppCheckProbeOutcome(
                statusLabel = "https_blocked",
                abortCategory = AiFailureCategory.NETWORK_BLOCKED,
                detail = "HTTPS to Firebase was blocked ($blockReason)",
                cause = error
            )
        }

        val isNetworkError = httpsProbe is FirebaseHttpsProbeResult.DnsOrSocketUnreachable ||
            hasCauseOf<UnknownHostException>(error) ||
            hasCauseOf<ConnectException>(error) ||
            hasCauseOf<NoRouteToHostException>(error) ||
            lower.contains("unable to resolve host") ||
            lower.contains("network error") ||
            lower.contains("network is unreachable") ||
            lower.contains("failed to connect") ||
            lower.contains("no address associated") ||
            lower.contains("connection reset")

        if (isNetworkError) {
            val reachReason = (httpsProbe as? FirebaseHttpsProbeResult.DnsOrSocketUnreachable)?.reason ?: redactedChain
            return AppCheckProbeOutcome(
                statusLabel = "network_unreachable",
                abortCategory = AiFailureCategory.NETWORK_OFFLINE,
                detail = "Unable to reach Firebase over HTTPS ($reachReason)",
                cause = error
            )
        }

        val httpCode = extractHttpCode(redactedChain)
        val statusCodePart = httpCode?.let { "http_$it" } ?: error.javaClass.simpleName
        return AppCheckProbeOutcome(
            statusLabel = "rejected:$statusCodePart",
            abortCategory = AiFailureCategory.APP_CHECK_OR_AUTH,
            detail = redactedChain,
            cause = error
        )
    }

    fun collectSafeExceptionSummary(error: Throwable): String {
        val parts = mutableListOf<String>()
        val visited = hashSetOf<Throwable>()
        var current: Throwable? = error
        var depth = 0
        while (current != null && visited.add(current) && depth < 4) {
            val msg = SanaPromptEngine.redactSensitiveInfo(current.message.orEmpty())
                .replace('\n', ' ')
                .trim()
                .take(160)
            val entry = if (msg.isNotBlank()) "${current.javaClass.simpleName}: $msg" else current.javaClass.simpleName
            parts.add(entry)
            current = current.cause
            depth++
        }
        return parts.joinToString(" -> ")
    }

    private fun extractHttpCode(summary: String): Int? {
        val match = Regex("""\b(400|401|403|404|429|500|502|503|504)\b""").find(summary)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private inline fun <reified T : Throwable> hasCauseOf(error: Throwable): Boolean {
        val visited = hashSetOf<Throwable>()
        var current: Throwable? = error
        while (current != null && visited.add(current)) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
