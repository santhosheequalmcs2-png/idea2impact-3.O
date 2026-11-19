package com.example.util

import com.example.data.model.AiFailureCategory
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NetworkDiagnosticsTest {

    @Test
    fun checkDeviceConnectivity_nullContext_returnsUnknown() {
        assertThat(NetworkDiagnostics.checkDeviceConnectivity(null)).isEqualTo(DeviceConnectivityState.UNKNOWN)
        assertThat(NetworkDiagnostics.describeActiveTransport(null)).isEqualTo("Unknown")
    }

    @Test
    fun runFirebaseDiagnostics_offlineDevice_shortCircuitsWithOfflineBannerAndAllServicesOffline() = runBlocking {
        var httpsProbeCalled = false
        var appCheckProbeCalled = false

        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.OFFLINE },
            transportProvider = { "No Active Network" },
            httpsProber = {
                httpsProbeCalled = true
                FirebaseHttpsProbeResult.Reachable(200)
            },
            appCheckProber = {
                appCheckProbeCalled = true
                AppCheckProbeOutcome("token_ready")
            },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "debug",
                    projectId = "idea2impact-prod",
                    provider = "DebugAppCheckProviderFactory",
                    tokenSource = "persisted_debug_token"
                )
            },
            timeLabelProvider = { "12:00:00" }
        )

        assertThat(httpsProbeCalled).isFalse()
        assertThat(appCheckProbeCalled).isFalse()
        assertThat(report.isRunning).isFalse()
        assertThat(report.hasRun).isTrue()
        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.OFFLINE)
        assertThat(report.headline).isEqualTo("Device Is Offline")
        assertThat(report.errorBanner).contains("No active internet connection")
        assertThat(report.actionHint).contains("Wi-Fi or mobile data")
        assertThat(report.serviceChecks).hasSize(NetworkDiagnostics.CORE_FIREBASE_ENDPOINTS.size)
        assertThat(report.serviceChecks.all { it.status == DiagnosticStatus.OFFLINE }).isTrue()
    }

    @Test
    fun runFirebaseDiagnostics_blockedTlsOrCaptivePortal_surfacesBlockedErrorBanner() = runBlocking {
        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            httpsProber = { url ->
                if (url.contains("firebasevertexai")) {
                    FirebaseHttpsProbeResult.TlsOrHttpsBlocked(
                        "Unexpected redirect (HTTP 302) from captive portal or proxy",
                        SSLHandshakeException("CertPathValidatorException")
                    )
                } else {
                    FirebaseHttpsProbeResult.Reachable(404)
                }
            },
            appCheckProber = { AppCheckProbeOutcome("token_ready") },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "debug",
                    projectId = "idea2impact-prod",
                    provider = "DebugAppCheckProviderFactory",
                    tokenSource = "persisted_debug_token"
                )
            },
            timeLabelProvider = { "12:05:00" },
            clockMs = { 1000L }
        )

        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.BLOCKED)
        assertThat(report.headline).isEqualTo("Firebase Requests Blocked")
        assertThat(report.errorBanner).contains("Firebase AI Logic")
        assertThat(report.errorBanner).contains("blocked")
        assertThat(report.actionHint).contains("captive portal")
        val aiCheck = report.serviceChecks.first { it.id == "firebase_ai" }
        assertThat(aiCheck.status).isEqualTo(DiagnosticStatus.BLOCKED)
        assertThat(aiCheck.badgeText).isEqualTo("Blocked")
    }

    @Test
    fun runFirebaseDiagnostics_allEndpointsUnreachable_surfacesOfflineNoInternetBanner() = runBlocking {
        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.ONLINE_UNVALIDATED },
            transportProvider = { "Wi-Fi" },
            httpsProber = {
                FirebaseHttpsProbeResult.DnsOrSocketUnreachable(
                    "DNS lookup failed for Firebase host",
                    UnknownHostException("Unable to resolve host")
                )
            },
            appCheckProber = { AppCheckProbeOutcome("network_unreachable", AiFailureCategory.NETWORK_OFFLINE) },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "debug",
                    projectId = "idea2impact-prod"
                )
            },
            timeLabelProvider = { "12:10:00" }
        )

        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.OFFLINE)
        assertThat(report.headline).isEqualTo("No Internet Access to Firebase")
        assertThat(report.errorBanner).contains("all Firebase endpoints are unreachable")
        assertThat(report.serviceChecks.all { it.status == DiagnosticStatus.ERROR }).isTrue()
    }

    @Test
    fun runFirebaseDiagnostics_partialEndpointUnreachable_identifiesBlockedServices() = runBlocking {
        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Mobile Data" },
            httpsProber = { url ->
                if (url.contains("firestore")) {
                    FirebaseHttpsProbeResult.DnsOrSocketUnreachable("Timed out reaching Firebase over HTTPS")
                } else {
                    FirebaseHttpsProbeResult.Reachable(404)
                }
            },
            appCheckProber = { AppCheckProbeOutcome("token_ready") },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "debug",
                    projectId = "idea2impact-prod"
                )
            },
            timeLabelProvider = { "12:12:00" }
        )

        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.ERROR)
        assertThat(report.headline).isEqualTo("Partial Firebase Reachability Failure")
        assertThat(report.errorBanner).contains("Reachable: 3/4")
        assertThat(report.errorBanner).contains("Cloud Firestore")
    }

    @Test
    fun runFirebaseDiagnostics_reachableHttpsButAppCheckRejected_reportsDebugGuidanceWithoutLeakingSecrets() = runBlocking {
        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            httpsProber = { FirebaseHttpsProbeResult.Reachable(404) },
            appCheckProber = {
                AppCheckProbeOutcome(
                    statusLabel = "rejected:http_403",
                    abortCategory = AiFailureCategory.APP_CHECK_OR_AUTH,
                    detail = "FirebaseException: Error returned from API. code: 403 body: App attestation failed token=AIzaSySecretLeak1234567890"
                )
            },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "debug",
                    projectId = "idea2impact-prod",
                    provider = "DebugAppCheckProviderFactory",
                    tokenSource = "generated_or_default_debug_token"
                )
            },
            timeLabelProvider = { "12:15:00" }
        )

        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.ERROR)
        assertThat(report.headline).isEqualTo("Firebase Reachable · App Check Rejected")
        assertThat(report.errorBanner).contains("App Check attestation failed")
        assertThat(report.actionHint).contains("DebugAppCheckProvider")
        assertThat(report.actionHint).contains("Manage debug tokens")
        assertThat(report.appCheckSummary).contains("DEBUG")
        assertThat(report.appCheckSummary).contains("Rejected")
        // Verify raw FirebaseException passed directly into classifyFirebaseProbeException stays redacted too
        val classified = NetworkDiagnostics.classifyFirebaseProbeException(
            error = FirebaseException("403 rejected AIzaSyFakeKey12345678901234567"),
            connectivity = DeviceConnectivityState.ONLINE_VALIDATED,
            httpsProbe = FirebaseHttpsProbeResult.Reachable(404)
        )
        assertThat(classified.abortCategory).isEqualTo(AiFailureCategory.APP_CHECK_OR_AUTH)
        assertThat(classified.detail).doesNotContain("AIzaSyFakeKey12345678901234567")
    }

    @Test
    fun runFirebaseDiagnostics_allEndpointsReachableAndAppCheckReady_returnsPass() = runBlocking {
        var tick = 100L
        val report = NetworkDiagnostics.runFirebaseDiagnostics(
            context = null,
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi + VPN" },
            httpsProber = { FirebaseHttpsProbeResult.Reachable(404) },
            appCheckProber = { AppCheckProbeOutcome("token_ready") },
            initStatusProvider = {
                AppCheckInitStatus(
                    initialized = true,
                    buildType = "release",
                    projectId = "idea2impact-prod",
                    provider = "RecaptchaAppCheckProviderFactory",
                    tokenSource = "recaptcha_configured"
                )
            },
            timeLabelProvider = { "12:20:00" },
            clockMs = {
                tick += 15L
                tick
            }
        )

        assertThat(report.overallStatus).isEqualTo(DiagnosticStatus.PASS)
        assertThat(report.headline).isEqualTo("All Firebase Services Reachable")
        assertThat(report.errorBanner).isNull()
        assertThat(report.transportLabel).isEqualTo("Wi-Fi + VPN")
        assertThat(report.appCheckSummary).contains("Token Ready")
        assertThat(report.serviceChecks).hasSize(NetworkDiagnostics.CORE_FIREBASE_ENDPOINTS.size)
        assertThat(report.serviceChecks.all { it.status == DiagnosticStatus.PASS && it.latencyMs != null }).isTrue()
    }
}
