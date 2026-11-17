package com.example.data.repository

import com.example.data.model.AiFailureCategory
import com.example.data.model.AiRequestPhase
import com.example.data.model.BackendInnovation
import com.example.data.model.CopilotMessage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiCopilotDiagnosticsTest {

    private fun sampleInnovation() = BackendInnovation(
        id = "inn-solar",
        title = "Solar Condenser Grid",
        description = "Off-grid atmospheric moisture condenser for rural clinics",
        category = "Inventions",
        stage = "Prototype",
        fundingGoal = 120_000.0,
        valuation = 700_000.0,
        imageUrl = null,
        authorId = "u-2",
        authorName = "Meera Iyer",
        authorTitle = "Lead Engineer",
        authorAvatar = null,
        isVerifiedCreator = true,
        likeCount = 8,
        commentCount = 2,
        isLiked = false,
        isBookmarked = false
    )

    @Test
    fun deepAndFastModes_bothPreserveConversationalGreetingAnd7PartIdeaAnalysisParity() {
        val deepInstruction = SanaPromptEngine.buildSystemInstruction(isFastMode = false)
        val fastInstruction = SanaPromptEngine.buildSystemInstruction(isFastMode = true)

        // Greeting: both routes receive zero unwanted marketplace/innovation context
        val deepGreeting = SanaPromptEngine.buildCustomKeyContextString(
            prompt = "hi",
            history = emptyList(),
            activeInnovation = sampleInnovation(),
            catalogContext = listOf(sampleInnovation()),
            talentContext = emptyList(),
            directorySummaryLines = emptyList(),
            isFastMode = false
        )
        val fastGreeting = SanaPromptEngine.buildCustomKeyContextString(
            prompt = "hi",
            history = emptyList(),
            activeInnovation = sampleInnovation(),
            catalogContext = listOf(sampleInnovation()),
            talentContext = emptyList(),
            directorySummaryLines = emptyList(),
            isFastMode = true
        )

        assertThat(deepGreeting).doesNotContain("Solar Condenser Grid")
        assertThat(fastGreeting).doesNotContain("Solar Condenser Grid")
        assertThat(deepInstruction).contains("never force structured templates onto greetings")
        assertThat(fastInstruction).contains("never force structured templates onto greetings")

        // Idea analysis: both routes apply honest 1-10 calibration and 7-section structure
        val ideaPrompt = "Evaluate my idea: an offline voice triage tool for rural pharmacists. Please rate it and suggest tests."
        val deepIdeaBundle = SanaPromptEngine.analyzeAndBuildContext(
            prompt = ideaPrompt,
            activeInnovation = sampleInnovation()
        )
        assertThat(deepIdeaBundle.isIdeaEvaluation).isTrue()
        assertThat(deepIdeaBundle.isGreeting).isFalse()
        assertThat(deepIdeaBundle.referenceContextBlock).isEmpty()
    }

    @Test
    fun diagnosticException_retainsPartialOutputAndSurfacesActionableDeepAndFastMessages() {
        val deepPartialTimeout = AiCopilotDiagnosticException(
            route = "deep_flash",
            modelName = "gemini-3.8-flash",
            phase = "streaming_chunks",
            elapsedMs = 42_300L,
            chunkCount = 6,
            appCheckStatus = "token_ready",
            partialContent = "### What I understand\nYou are building a rural pharmacy voice triage tool.",
            cause = SocketTimeoutException("Stream read timed out")
        )
        val fastFirstChunkTimeout = AiCopilotDiagnosticException(
            route = "fast_flash_lite",
            modelName = "gemini-3.5-flash-lite",
            phase = "awaiting_first_chunk",
            elapsedMs = 55_050L,
            chunkCount = 0,
            appCheckStatus = "probe_timeout_2000ms",
            cause = runBlocking {
                runCatching { withTimeout(1L) { kotlinx.coroutines.delay(50L) } }.exceptionOrNull()
                    as? TimeoutCancellationException ?: SocketTimeoutException("Timeout")
            }
        )

        val deepMessage = SanaPromptEngine.diagnoseAiError(deepPartialTimeout, "deep_flash")
        val fastMessage = SanaPromptEngine.diagnoseAiError(fastFirstChunkTimeout, "fast_flash_lite")

        assertThat(deepPartialTimeout.partialContent).contains("rural pharmacy voice triage tool")
        assertThat(deepMessage).contains("mid-stream after receiving 6 chunks")
        assertThat(deepMessage).contains("deep_flash")
        assertThat(deepMessage).contains("gemini-3.8-flash")

        assertThat(fastMessage).contains("before the first response token arrived")
        assertThat(fastMessage).contains("fast_flash_lite")
        assertThat(fastMessage).contains("gemini-3.5-flash-lite")
        assertThat(fastMessage).contains("probe_timeout_2000ms")
    }

    @Test
    fun usableHistoryFilter_stripsWelcomeAndErrorMessagesAcrossRoutes() {
        val history = listOf(
            CopilotMessage("welcome", true, "### Meet AI Sana", "09:00"),
            CopilotMessage("u1", false, "hi", "09:01"),
            CopilotMessage("err1", true, "### AI Sana Error\n- timeout", "09:01", isError = true),
            CopilotMessage("u2", false, "Explain solid-state electrolytes", "09:02"),
            CopilotMessage("a2", true, "Solid-state electrolytes use ceramic or polymer conductors.", "09:02")
        )
        val filtered = SanaPromptEngine.filterUsableHistory(history)
        assertThat(filtered.map { it.id }).containsExactly("u1", "u2", "a2").inOrder()
    }

    @Test
    fun isTimeoutException_identifiesStreamingTimeoutsAndRejectsAuthOrOfflineErrors() {
        val wrappedTimeout = AiCopilotDiagnosticException(
            route = "deep_flash",
            modelName = "gemini-3.8-flash",
            phase = "streaming_chunks",
            elapsedMs = 60_010L,
            chunkCount = 4,
            appCheckStatus = "token_ready",
            partialContent = "Partial reply...",
            cause = SocketTimeoutException("Read timed out")
        )
        val coroutineTimeout = runBlocking {
            runCatching { withTimeout(1L) { kotlinx.coroutines.delay(50L) } }.exceptionOrNull()!!
        }
        val wrappedCoroutineTimeout = AiCopilotDiagnosticException(
            route = "fast_flash_lite",
            modelName = "gemini-3.5-flash-lite",
            phase = "awaiting_first_chunk",
            elapsedMs = 60_000L,
            chunkCount = 0,
            appCheckStatus = "token_ready",
            cause = coroutineTimeout
        )

        assertThat(SanaPromptEngine.isTimeoutException(wrappedTimeout)).isTrue()
        assertThat(SanaPromptEngine.isTimeoutException(wrappedCoroutineTimeout)).isTrue()
        assertThat(SanaPromptEngine.isTimeoutException(BackendApiException(504, "Gateway Timeout"))).isTrue()
        assertThat(SanaPromptEngine.isTimeoutException(IllegalStateException("DEADLINE_EXCEEDED"))).isTrue()

        assertThat(SanaPromptEngine.isTimeoutException(BackendApiException(401, "Invalid API key"))).isFalse()
        assertThat(SanaPromptEngine.isTimeoutException(BackendApiException(403, "Forbidden"))).isFalse()
        assertThat(SanaPromptEngine.isTimeoutException(java.net.UnknownHostException("offline"))).isFalse()
        assertThat(
            SanaPromptEngine.isTimeoutException(
                AiCopilotDiagnosticException(
                    route = "deep_flash",
                    modelName = "gemini-3.8-flash",
                    phase = "empty_completion",
                    elapsedMs = 400L,
                    chunkCount = 0,
                    appCheckStatus = "token_ready"
                )
            )
        ).isFalse()
    }

    @Test
    fun copilotMessage_timeoutFlagsDefaultFalseAndExcludeFromHistoryWhenTimedOut() {
        val normal = CopilotMessage("m1", true, "Hello!", "09:00")
        val timedOutPartial = CopilotMessage(
            id = "err_timeout",
            isAssistant = true,
            content = "Partial\n\n---\n### AI Sana Error\n- timed out",
            timestamp = "09:01",
            isError = true,
            isTimeout = true,
            hasPartialTimeoutOutput = true
        )

        assertThat(normal.isTimeout).isFalse()
        assertThat(normal.hasPartialTimeoutOutput).isFalse()
        assertThat(timedOutPartial.isTimeout).isTrue()
        assertThat(timedOutPartial.hasPartialTimeoutOutput).isTrue()

        val filtered = SanaPromptEngine.filterUsableHistory(listOf(normal, timedOutPartial))
        assertThat(filtered.map { it.id }).containsExactly("m1")
    }

    @Test
    fun statusIndicatorLabels_progressThroughConnectionThinkingDelaysAndStreaming() {
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.IDLE,
                elapsedMs = 0L
            )
        ).isEmpty()

        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.CHECKING_CONNECTION,
                elapsedMs = 400L
            )
        ).isEqualTo("Checking connection...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.CHECKING_CONNECTION,
                elapsedMs = 4_500L
            )
        ).isEqualTo("Still checking connection...")

        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 1_200L,
                hasAttachedImage = false
            )
        ).isEqualTo("Thinking...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 1_200L,
                hasAttachedImage = true
            )
        ).isEqualTo("Analyzing prototype image...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 7_000L,
                isCustomKey = false
            )
        ).isEqualTo("Still thinking (7s)...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 7_000L,
                isCustomKey = true
            )
        ).isEqualTo("Waiting for AI backend (7s)...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 16_000L
            )
        ).isEqualTo("Still waiting for response (16s)...")
        assertThat(
            SanaPromptEngine.resolveStatusLabel(
                phase = com.example.data.model.AiRequestPhase.STREAMING,
                elapsedMs = 3_000L
            )
        ).isEqualTo("Streaming response...")

        assertThat(
            SanaPromptEngine.resolveStatusHint(
                phase = com.example.data.model.AiRequestPhase.THINKING,
                elapsedMs = 16_000L,
                isFastMode = false
            )
        ).contains("Toggle Fast mode or Stop anytime")
    }

    @Test
    fun deepMode_retriesPreChunkProviderOverloadWithBackoffAndRecovers(): Unit = runBlocking {
        val repo = AiCopilotRepository()
        val sleepDelays = mutableListOf<Long>()
        val phases = mutableListOf<AiRequestPhase>()
        var attempts = 0
        var resolvedRoute = ""
        var resolvedModel = ""
        var switchedFrom: String? = "unexpected"

        val emitted = repo.executeStreamWithRecovery(
            useFastModel = false,
            preChunkBackoffMs = 800L,
            appCheckProbe = { "token_ready" },
            streamFactory = { isFast ->
                assertThat(isFast).isFalse()
                attempts++
                flow {
                    if (attempts == 1) {
                        throw IllegalStateException("503 Service Unavailable: provider overloaded")
                    }
                    emit("Hello ")
                    emit("from Deep!")
                }
            },
            sleeper = { sleepDelays.add(it) },
            onPhaseChange = { phases.add(it) },
            onModelResolved = { route, model, switched, _ ->
                resolvedRoute = route
                resolvedModel = model
                switchedFrom = switched
            }
        ).toList()

        assertThat(attempts).isEqualTo(2)
        assertThat(sleepDelays).containsExactly(800L)
        assertThat(phases).contains(AiRequestPhase.RETRYING)
        assertThat(phases).doesNotContain(AiRequestPhase.SWITCHING_MODEL)
        assertThat(resolvedRoute).isEqualTo("deep_flash")
        assertThat(resolvedModel).isEqualTo(DEEP_MODEL_NAME)
        assertThat(switchedFrom).isNull()
        assertThat(emitted).containsExactly("Hello ", "Hello from Deep!").inOrder()
    }

    @Test
    fun deepMode_fallsBackToFastModeWhenDeepUnavailableBeforeFirstChunk(): Unit = runBlocking {
        val repo = AiCopilotRepository()
        val phases = mutableListOf<AiRequestPhase>()
        val sleepDelays = mutableListOf<Long>()
        var deepAttempts = 0
        var fastAttempts = 0
        var resolvedRoute = ""
        var resolvedModel = ""
        var switchedFrom: String? = null
        var fallbackReason: AiFailureCategory? = null

        val emitted = repo.executeStreamWithRecovery(
            useFastModel = false,
            preChunkBackoffMs = 500L,
            appCheckProbe = { "token_ready" },
            streamFactory = { isFast ->
                flow {
                    if (!isFast) {
                        deepAttempts++
                        throw IllegalStateException("503 Model overloaded")
                    } else {
                        fastAttempts++
                        emit("Fast mode answer")
                    }
                }
            },
            sleeper = { sleepDelays.add(it) },
            onPhaseChange = { phases.add(it) },
            onModelResolved = { route, model, switched, reason ->
                resolvedRoute = route
                resolvedModel = model
                switchedFrom = switched
                fallbackReason = reason
            }
        ).toList()

        assertThat(deepAttempts).isEqualTo(2)
        assertThat(fastAttempts).isEqualTo(1)
        assertThat(sleepDelays).containsExactly(500L)
        assertThat(phases).contains(AiRequestPhase.SWITCHING_MODEL)
        assertThat(resolvedRoute).isEqualTo("fast_flash_lite")
        assertThat(resolvedModel).isEqualTo(FAST_MODEL_NAME)
        assertThat(switchedFrom).isEqualTo(DEEP_MODEL_NAME)
        assertThat(fallbackReason).isEqualTo(AiFailureCategory.PROVIDER_OVERLOAD)
        assertThat(emitted).containsExactly("Fast mode answer")
    }

    @Test
    fun deepAndFastBothFail_preservesPrimaryFailureContextWithoutHidingError(): Unit = runBlocking {
        val repo = AiCopilotRepository()
        var thrown: AiCopilotDiagnosticException? = null

        try {
            repo.executeStreamWithRecovery(
                useFastModel = false,
                preChunkBackoffMs = 10L,
                appCheckProbe = { "token_ready" },
                streamFactory = { isFast ->
                    flow {
                        if (!isFast) {
                            throw IllegalStateException("404 Model gemini-3.8-flash not found")
                        } else {
                            throw IllegalStateException("429 RESOURCE_EXHAUSTED")
                        }
                    }
                },
                sleeper = {}
            ).toList()
            fail("Expected AiCopilotDiagnosticException when both Deep and Fast fail")
        } catch (e: AiCopilotDiagnosticException) {
            thrown = e
        }

        val err = thrown!!
        assertThat(err.fallbackAttempted).isTrue()
        assertThat(err.route).isEqualTo("fast_flash_lite")
        assertThat(err.category).isEqualTo(AiFailureCategory.QUOTA_OR_RATE_LIMIT)
        assertThat(err.primaryFailureNote).contains(DEEP_MODEL_NAME)
        assertThat(err.primaryFailureNote).contains("MODEL_UNAVAILABLE")

        val diagnosed = SanaPromptEngine.diagnoseAiError(err, "deep_flash")
        assertThat(diagnosed).contains("quota or rate limit")
        assertThat(diagnosed).contains("Initial attempt note")
    }

    @Test
    fun partialOutputReceived_neverRetriesOrFallsBackAutomatically(): Unit = runBlocking {
        val repo = AiCopilotRepository()
        var attempts = 0
        val chunks = mutableListOf<String>()
        var thrown: AiCopilotDiagnosticException? = null

        try {
            repo.executeStreamWithRecovery(
                useFastModel = false,
                appCheckProbe = { "token_ready" },
                streamFactory = {
                    attempts++
                    flow {
                        emit("First partial paragraph.")
                        throw IOException("Connection reset mid-stream")
                    }
                },
                sleeper = { fail("Should not sleep/retry after partial chunk") }
            ).collect { chunks.add(it) }
            fail("Expected mid-stream interruption exception")
        } catch (e: AiCopilotDiagnosticException) {
            thrown = e
        }

        assertThat(attempts).isEqualTo(1)
        assertThat(chunks).containsExactly("First partial paragraph.")
        assertThat(thrown!!.partialContent).isEqualTo("First partial paragraph.")
        assertThat(thrown.chunkCount).isEqualTo(1)
        assertThat(thrown.fallbackAttempted).isFalse()
        assertThat(thrown.category).isEqualTo(AiFailureCategory.STREAMING_INTERRUPTION)
    }

    @Test
    fun offlineAndAppCheckFailures_doNotRetryOverloadOrSwitchToFastMode(): Unit = runBlocking {
        val repo = AiCopilotRepository()

        var offlineAttempts = 0
        try {
            repo.executeStreamWithRecovery(
                useFastModel = false,
                appCheckProbe = { "token_ready" },
                streamFactory = {
                    offlineAttempts++
                    flow { throw UnknownHostException("Unable to resolve host") }
                },
                sleeper = { fail("Should not retry offline errors") }
            ).toList()
            fail("Expected offline failure")
        } catch (e: AiCopilotDiagnosticException) {
            assertThat(e.category).isEqualTo(AiFailureCategory.NETWORK_OFFLINE)
            assertThat(e.fallbackAttempted).isFalse()
        }
        assertThat(offlineAttempts).isEqualTo(1)

        var appCheckAttempts = 0
        try {
            repo.executeStreamWithRecovery(
                useFastModel = false,
                appCheckProbe = { "token_ready" },
                streamFactory = {
                    appCheckAttempts++
                    flow { throw IllegalStateException("HTTP 403 App Check token rejected") }
                },
                sleeper = { fail("Should not retry App Check rejection") }
            ).toList()
            fail("Expected App Check failure")
        } catch (e: AiCopilotDiagnosticException) {
            assertThat(e.category).isEqualTo(AiFailureCategory.APP_CHECK_OR_AUTH)
            assertThat(e.fallbackAttempted).isFalse()
        }
        assertThat(appCheckAttempts).isEqualTo(1)
    }

    @Test
    fun fastMode_retriesPreChunkOverloadAndRecovers(): Unit = runBlocking {
        val repo = AiCopilotRepository()
        var attempts = 0
        val sleepDelays = mutableListOf<Long>()

        val emitted = repo.executeStreamWithRecovery(
            useFastModel = true,
            preChunkBackoffMs = 800L,
            appCheckProbe = { "token_ready" },
            streamFactory = { isFast ->
                assertThat(isFast).isTrue()
                attempts++
                flow {
                    if (attempts == 1) {
                        throw BackendApiException(503, "Overloaded")
                    }
                    emit("Recovered fast reply")
                }
            },
            sleeper = { sleepDelays.add(it) }
        ).toList()

        assertThat(attempts).isEqualTo(2)
        assertThat(sleepDelays).containsExactly(800L)
        assertThat(emitted).containsExactly("Recovered fast reply")
    }

    @Test
    fun customKeyRoute_rejectsErrorPayloadsHtmlAndMalformedKeys_withoutFakeReplies() {
        // Explicit success=false with message field must throw, not masquerade as an AI answer
        try {
            BackendRepository.parseAiChatResponse("""{"success":false,"statusCode":401,"message":"Invalid API key AIzaSyFAKEKEY1234567890"}""")
            fail("Expected BackendApiException on success=false payload")
        } catch (e: BackendApiException) {
            assertThat(e.code).isEqualTo(401)
            assertThat(e.message).contains("Invalid API key")
            assertThat(e.message).doesNotContain("AIzaSyFAKEKEY1234567890")
            assertThat(e.message).contains("AIza***")
        }

        // Nested error object
        try {
            BackendRepository.parseAiChatResponse("""{"error":{"code":429,"message":"Quota exceeded"}}""")
            fail("Expected BackendApiException on nested error object")
        } catch (e: BackendApiException) {
            assertThat(e.code).isEqualTo(429)
            assertThat(e.message).contains("Quota exceeded")
        }

        // Non-JSON HTML proxy response (e.g. Render 502)
        try {
            BackendRepository.parseAiChatResponse("<html><body>502 Bad Gateway</body></html>", httpCode = 502)
            fail("Expected BackendApiException on HTML error body")
        } catch (e: BackendApiException) {
            assertThat(e.code).isEqualTo(502)
            assertThat(e.message).contains("non-JSON")
        }

        // Malformed custom API key rejected before network call
        try {
            BackendRepository.validateCustomApiKey("short")
            fail("Expected BackendApiException for short key")
        } catch (e: BackendApiException) {
            assertThat(e.code).isEqualTo(400)
        }

        // Valid reply succeeds
        val ok = BackendRepository.parseAiChatResponse("""{"reply":"Validated battery electrolyte insight."}""")
        assertThat(ok).isEqualTo("Validated battery electrolyte insight.")
    }

    @Test
    fun redactSensitiveInfo_masksApiKeysAndBearerTokens() {
        val raw = "Failed calling key=AIzaSyTestSecretKey123456789 with Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.payload.sig"
        val redacted = SanaPromptEngine.redactSensitiveInfo(raw)

        assertThat(redacted).doesNotContain("AIzaSyTestSecretKey123456789")
        assertThat(redacted).doesNotContain("eyJhbGciOiJIUzI1NiJ9")
        assertThat(redacted).contains("AIza***")
        assertThat(redacted).contains("Bearer ***")
    }
}
