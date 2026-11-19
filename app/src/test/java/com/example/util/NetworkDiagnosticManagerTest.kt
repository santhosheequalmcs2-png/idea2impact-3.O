package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NetworkDiagnosticManagerTest {

    @Test
    fun offlineDevice_updatesSharedStateFlowToCheckInternetConnection() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.OFFLINE },
            transportProvider = { "No Active Network" },
            googleDnsProber = {
                error("Google DNS probe should not run when ConnectivityManager already reports offline")
            },
            firebaseDiagnosticsRunner = {
                error("Firebase probe should not run when ConnectivityManager already reports offline")
            },
            timeLabelProvider = { "10:00:00" }
        )

        val result = manager.checkConnectivity()
        val observed = manager.state.value

        assertThat(result).isEqualTo(observed)
        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.NO_INTERNET)
        assertThat(observed.streamLabel).isEqualTo("No Internet")
        assertThat(observed.statusMessage).isEqualTo("Check your internet connection")
        assertThat(observed.googleDnsReachable).isFalse()
        assertThat(observed.firebaseReachable).isFalse()
        assertThat(observed.isChecking).isFalse()
        assertThat(manager.diagnosticsReport.value.serviceChecks.first().id).isEqualTo("google_public_dns")
    }

    @Test
    fun inFlightProbe_emitsCheckingConnectionStreamLabelBeforeResolving() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val probeStarted = CompletableDeferred<Unit>()
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            googleDnsProber = {
                probeStarted.complete(Unit)
                gate.await()
                PublicDnsProbeResult(reachable = false, detail = "Timed out")
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
                    transportProvider = { "Wi-Fi" },
                    httpsProber = { FirebaseHttpsProbeResult.DnsOrSocketUnreachable("Timed out") },
                    appCheckProber = { AppCheckProbeOutcome("network_unreachable") },
                    timeLabelProvider = { "10:02:00" }
                )
            },
            timeLabelProvider = { "10:02:00" }
        )

        val checkJob = async { manager.checkConnectivity() }
        withTimeout(2_000L) { probeStarted.await() }

        val checkingState = withTimeout(2_000L) {
            manager.state.first { it.isChecking }
        }
        assertThat(checkingState.health).isEqualTo(AiNetworkHealthStatus.CHECKING)
        assertThat(checkingState.streamLabel).isEqualTo("Checking Connection")

        gate.complete(Unit)
        val finalState = checkJob.await()
        assertThat(finalState.isChecking).isFalse()
        assertThat(finalState.health).isEqualTo(AiNetworkHealthStatus.NO_INTERNET)
        assertThat(finalState.streamLabel).isEqualTo("No Internet")
        assertThat(finalState.statusMessage).isEqualTo("Check your internet connection")
    }

    @Test
    fun googleDnsAndFirebaseBothUnreachable_updatesSharedStateFlowToCheckInternetConnection() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_UNVALIDATED },
            transportProvider = { "Wi-Fi" },
            googleDnsProber = {
                PublicDnsProbeResult(reachable = false, detail = "Socket timed out connecting to 8.8.8.8:443")
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_UNVALIDATED },
                    transportProvider = { "Wi-Fi" },
                    httpsProber = { FirebaseHttpsProbeResult.DnsOrSocketUnreachable("DNS lookup failed") },
                    appCheckProber = { AppCheckProbeOutcome("network_unreachable") },
                    timeLabelProvider = { "10:05:00" }
                )
            },
            timeLabelProvider = { "10:05:00" }
        )

        val observed = manager.checkConnectivity()

        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.NO_INTERNET)
        assertThat(observed.streamLabel).isEqualTo("No Internet")
        assertThat(observed.statusMessage).isEqualTo("Check your internet connection")
        assertThat(observed.googleDnsReachable).isFalse()
        assertThat(observed.firebaseReachable).isFalse()
        assertThat(manager.diagnosticsReport.value.overallStatus).isEqualTo(DiagnosticStatus.OFFLINE)
    }

    @Test
    fun googleDnsReachableButFirebaseUnreachable_updatesSharedStateFlowToFirebaseServerUnreachable() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            googleDnsProber = {
                PublicDnsProbeResult(reachable = true, host = "8.8.8.8", port = 443, latencyMs = 18L)
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
                    transportProvider = { "Wi-Fi" },
                    httpsProber = { url ->
                        if (url.contains("firebasevertexai")) {
                            FirebaseHttpsProbeResult.DnsOrSocketUnreachable("Timed out reaching Firebase host")
                        } else {
                            FirebaseHttpsProbeResult.Reachable(404)
                        }
                    },
                    appCheckProber = { AppCheckProbeOutcome("token_ready") },
                    timeLabelProvider = { "10:10:00" }
                )
            },
            timeLabelProvider = { "10:10:00" }
        )

        val observed = manager.checkConnectivity()

        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.FIREBASE_UNREACHABLE)
        assertThat(observed.statusMessage).isEqualTo("Firebase server unreachable")
        assertThat(observed.detailMessage).contains("Google DNS 8.8.8.8 reachable")
        assertThat(observed.detailMessage).contains("Firebase AI Logic")
        assertThat(observed.googleDnsReachable).isTrue()
        assertThat(observed.firebaseReachable).isFalse()
        assertThat(manager.diagnosticsReport.value.serviceChecks.first { it.id == "google_public_dns" }.status)
            .isEqualTo(DiagnosticStatus.PASS)
    }

    @Test
    fun firebaseHttpsBlocked_updatesSharedStateFlowToRequestsBlocked() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            googleDnsProber = {
                PublicDnsProbeResult(reachable = true, latencyMs = 22L)
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
                    transportProvider = { "Wi-Fi" },
                    httpsProber = {
                        FirebaseHttpsProbeResult.TlsOrHttpsBlocked("Captive portal intercepted HTTPS request")
                    },
                    appCheckProber = { AppCheckProbeOutcome("https_blocked") },
                    timeLabelProvider = { "10:15:00" }
                )
            },
            timeLabelProvider = { "10:15:00" }
        )

        val observed = manager.checkConnectivity()

        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.REQUESTS_BLOCKED)
        assertThat(observed.statusMessage).isEqualTo("Firebase requests blocked")
        assertThat(observed.googleDnsReachable).isTrue()
        assertThat(observed.firebaseReachable).isFalse()
    }

    @Test
    fun firebaseReachableButAppCheckRejected_updatesSharedStateFlowToAppCheckIssue() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Mobile Data" },
            googleDnsProber = {
                PublicDnsProbeResult(reachable = true, latencyMs = 25L)
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
                    transportProvider = { "Mobile Data" },
                    httpsProber = { FirebaseHttpsProbeResult.Reachable(404) },
                    appCheckProber = {
                        AppCheckProbeOutcome(
                            statusLabel = "rejected:http_403",
                            abortCategory = com.example.data.model.AiFailureCategory.APP_CHECK_OR_AUTH,
                            detail = "403 Forbidden"
                        )
                    },
                    timeLabelProvider = { "10:20:00" }
                )
            },
            timeLabelProvider = { "10:20:00" }
        )

        val observed = manager.checkConnectivity()

        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.APP_CHECK_ISSUE)
        assertThat(observed.statusMessage).isEqualTo("App Check verification failed")
        assertThat(observed.googleDnsReachable).isTrue()
        assertThat(observed.firebaseReachable).isTrue()
    }

    @Test
    fun googleDnsAndFirebaseReachable_updatesSharedStateFlowToConnected() = runBlocking {
        val manager = NetworkDiagnosticManager(
            connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
            transportProvider = { "Wi-Fi" },
            googleDnsProber = {
                PublicDnsProbeResult(reachable = true, latencyMs = 15L)
            },
            firebaseDiagnosticsRunner = { ctx ->
                NetworkDiagnostics.runFirebaseDiagnostics(
                    context = ctx,
                    connectivityProvider = { DeviceConnectivityState.ONLINE_VALIDATED },
                    transportProvider = { "Wi-Fi" },
                    httpsProber = { FirebaseHttpsProbeResult.Reachable(404) },
                    appCheckProber = { AppCheckProbeOutcome("token_ready") },
                    initStatusProvider = {
                        AppCheckInitStatus(
                            initialized = true,
                            buildType = "debug",
                            projectId = "idea2impact-prod"
                        )
                    },
                    timeLabelProvider = { "10:25:00" }
                )
            },
            timeLabelProvider = { "10:25:00" }
        )

        val observed = manager.checkConnectivity()

        assertThat(observed.health).isEqualTo(AiNetworkHealthStatus.CONNECTED)
        assertThat(observed.streamLabel).isEqualTo("Connected")
        assertThat(observed.statusMessage).isEqualTo("Connected · Firebase & AI ready")
        assertThat(observed.googleDnsReachable).isTrue()
        assertThat(observed.firebaseReachable).isTrue()
    }

    @Test
    fun startAndStopMonitoring_handlesAndroidContextSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = NetworkDiagnosticManager.getInstance(context)

        manager.startMonitoring(context)
        manager.stopMonitoring()
    }
}
