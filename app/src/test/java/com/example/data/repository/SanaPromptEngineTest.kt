package com.example.data.repository

import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.CopilotMessage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.TimeoutCancellationException
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SanaPromptEngineTest {

    private fun sampleInnovation() = BackendInnovation(
        id = "inn-1",
        title = "BioCell Membrane",
        description = "Solid-state zinc membrane filter",
        category = "Green Tech",
        stage = "Prototype",
        fundingGoal = 150_000.0,
        valuation = 900_000.0,
        imageUrl = null,
        authorId = "u-1",
        authorName = "Aarav Mehta",
        authorTitle = "Materials Lead",
        authorAvatar = null,
        isVerifiedCreator = true,
        likeCount = 12,
        commentCount = 3,
        isLiked = false,
        isBookmarked = false
    )

    private fun sampleTalent() = BackendTalent(
        id = "tal-1",
        name = "Dr. Priya Nair",
        role = "Electrochemistry Architect",
        roleCategory = "Builders",
        headline = "Ex-IIT battery membrane researcher",
        skills = listOf("Electrochemistry", "Membranes", "Pilot Scale-up"),
        location = "Bengaluru, India",
        avatarUrl = null,
        verified = true
    )

    @Test
    fun systemInstruction_enforcesSanaIdentity7PartAssessmentAndRatingRulesAcrossModes() {
        val deep = SanaPromptEngine.buildSystemInstruction(isFastMode = false)
        val fast = SanaPromptEngine.buildSystemInstruction(isFastMode = true)

        listOf(deep, fast).forEach { prompt ->
            assertThat(prompt).contains("AI Sana")
            assertThat(prompt).doesNotContain("Gemini")
            assertThat(prompt).doesNotContain("AI Copilot")
            assertThat(prompt).contains("What I understand")
            assertThat(prompt).contains("Risks and open questions")
            assertThat(prompt).contains("Ways to improve")
            assertThat(prompt).contains("How to test it")
            assertThat(prompt).contains("1–10 early rating")
            assertThat(prompt).contains("4–6")
            assertThat(prompt).contains("early assessment, not a prediction or guarantee")
            assertThat(prompt).contains("NEVER invent startups")
            assertThat(prompt).contains("suggestions, not confirmed interest")
        }
        assertThat(deep).contains("Deep Mode")
        assertThat(fast).contains("Fast Mode")
    }

    @Test
    fun scenario1_greeting_doesNotInjectMarketplaceOrInnovationContext() {
        val bundle = SanaPromptEngine.analyzeAndBuildContext(
            prompt = "hi",
            activeInnovation = sampleInnovation(),
            catalogContext = listOf(sampleInnovation()),
            talentContext = listOf(sampleTalent())
        )
        assertThat(bundle.isGreeting).isTrue()
        assertThat(bundle.referenceContextBlock).isEmpty()

        val customKeyContext = SanaPromptEngine.buildCustomKeyContextString(
            prompt = "Hi AI Sana, how are you?",
            history = emptyList(),
            activeInnovation = sampleInnovation(),
            catalogContext = listOf(sampleInnovation()),
            talentContext = listOf(sampleTalent()),
            directorySummaryLines = listOf("BIRAC BIG Grant"),
            isFastMode = true
        )
        assertThat(customKeyContext).doesNotContain("BioCell Membrane")
        assertThat(customKeyContext).doesNotContain("BIRAC BIG Grant")
    }

    @Test
    fun scenario2And3_ideaSubmissionAndValidation_includesFocusedInnovationOnlyWhenRelevant() {
        val evalBundle = SanaPromptEngine.analyzeAndBuildContext(
            prompt = "Here is my idea: a low-cost soil moisture sensor using LoRaWAN for small farms. Is this promising?",
            activeInnovation = sampleInnovation(),
            catalogContext = listOf(sampleInnovation()),
            talentContext = listOf(sampleTalent())
        )
        assertThat(evalBundle.isIdeaEvaluation).isTrue()
        assertThat(evalBundle.isGreeting).isFalse()
        assertThat(evalBundle.isUnrelatedGeneral).isFalse()
        // When asking about "my idea", focused marketplace innovation should not hijack the prompt
        assertThat(evalBundle.referenceContextBlock).isEmpty()

        val validateFocusedBundle = SanaPromptEngine.analyzeAndBuildContext(
            prompt = "How can we validate customer demand and improve this prototype?",
            activeInnovation = sampleInnovation(),
            catalogContext = emptyList()
        )
        assertThat(validateFocusedBundle.isIdeaEvaluation).isTrue()
        assertThat(validateFocusedBundle.referenceContextBlock).contains("<reference_data_only>")
        assertThat(validateFocusedBundle.referenceContextBlock).contains("BioCell Membrane")
    }

    @Test
    fun scenario4_startupAndCollaboratorRequest_groundsOnlyRealAppAndDirectoryRecords() {
        val withEmptyDirectory = SanaPromptEngine.analyzeAndBuildContext(
            prompt = "Find related Indian startups, investors, or collaborators for battery membranes.",
            activeInnovation = null,
            catalogContext = listOf(sampleInnovation()),
            talentContext = listOf(sampleTalent()),
            directorySummaryLines = emptyList()
        )
        assertThat(withEmptyDirectory.needsEcosystemContext).isTrue()
        assertThat(withEmptyDirectory.referenceContextBlock).contains("Dr. Priya Nair")
        assertThat(withEmptyDirectory.referenceContextBlock).contains("No verified startup/investor/program directory entries are currently loaded")

        val withVerifiedDirectory = SanaPromptEngine.buildCustomKeyContextString(
            prompt = "Which investors or startups in India match this?",
            history = listOf(
                CopilotMessage("welcome", true, "Welcome", "09:00"),
                CopilotMessage("u1", false, "We build zinc-air membranes.", "09:01"),
                CopilotMessage("err1", true, "### AI Sana Error\n- timeout", "09:01", isError = true)
            ),
            activeInnovation = sampleInnovation(),
            catalogContext = emptyList(),
            talentContext = listOf(sampleTalent()),
            directorySummaryLines = listOf("- ClimateSeed India [Investor · ClimateTech] | Source: climate.example | Last verified: 2026-10-01"),
            isFastMode = false
        )
        assertThat(withVerifiedDirectory).contains("ClimateSeed India")
        assertThat(withVerifiedDirectory).contains("Dr. Priya Nair")
        assertThat(withVerifiedDirectory).contains("We build zinc-air membranes.")
        assertThat(withVerifiedDirectory).doesNotContain("Welcome")
        assertThat(withVerifiedDirectory).doesNotContain("AI Sana Error")
    }

    @Test
    fun scenario5_unrelatedGeneralQuestions_omitMarketplaceAndInnovationContext() {
        val generalPrompts = listOf(
            "What is the boiling point of water at high altitude?",
            "I have no idea what photosynthesis means, can you explain it?",
            "What is the solution to x^2 - 5x + 6 = 0?"
        )
        generalPrompts.forEach { q ->
            val bundle = SanaPromptEngine.analyzeAndBuildContext(
                prompt = q,
                activeInnovation = sampleInnovation(),
                catalogContext = listOf(sampleInnovation()),
                talentContext = listOf(sampleTalent()),
                directorySummaryLines = listOf("Some Org")
            )
            assertThat(bundle.isUnrelatedGeneral).isTrue()
            assertThat(bundle.referenceContextBlock).isEmpty()
        }
    }

    @Test
    fun scenario6_diagnoseAiError_surfacesPhaseAndRouteDiagnosticsForDeepAndFastModes() {
        val deepFirstChunkTimeout = AiCopilotDiagnosticException(
            route = "deep_flash",
            modelName = "gemini-3.8-flash",
            phase = "awaiting_first_chunk",
            elapsedMs = 55_120L,
            chunkCount = 0,
            appCheckStatus = "token_ready",
            cause = SocketTimeoutException("Read timed out")
        )
        val fastAppCheckFailure = AiCopilotDiagnosticException(
            route = "fast_flash_lite",
            modelName = "gemini-3.5-flash-lite",
            phase = "app_check_probe",
            elapsedMs = 310L,
            chunkCount = 0,
            appCheckStatus = "probe_error:FirebaseException",
            cause = IllegalStateException("App Check token failed")
        )

        val deepMsg = SanaPromptEngine.diagnoseAiError(deepFirstChunkTimeout, "deep_flash")
        val fastMsg = SanaPromptEngine.diagnoseAiError(fastAppCheckFailure, "fast_flash_lite")
        val authErr = SanaPromptEngine.diagnoseAiError(BackendApiException(401, "Invalid API key"), "custom_key")
        val rateErr = SanaPromptEngine.diagnoseAiError(BackendApiException(429, "Quota exceeded"), "custom_key")
        val backendWakeErr = SanaPromptEngine.diagnoseAiError(BackendApiException(503, "Service unavailable"), "custom_key")
        val offlineErr = SanaPromptEngine.diagnoseAiError(UnknownHostException("host"), "deep_flash")
        val timeoutErr = SanaPromptEngine.diagnoseAiError(SocketTimeoutException("timeout"), "fast_flash_lite")
        val emptyErr = SanaPromptEngine.diagnoseAiError(IllegalStateException("AI Sana returned an empty response."), "fast_flash_lite")

        assertThat(deepMsg).contains("before the first response token arrived")
        assertThat(deepMsg).contains("deep_flash")
        assertThat(deepMsg).contains("gemini-3.8-flash")
        assertThat(fastMsg).contains("App Check")
        assertThat(fastMsg).contains("fast_flash_lite")
        assertThat(authErr).contains("Key Settings")
        assertThat(rateErr).contains("429")
        assertThat(backendWakeErr).contains("503")
        assertThat(offlineErr).contains("internet connection")
        assertThat(timeoutErr).contains("timed out")
        assertThat(emptyErr).contains("incomplete or empty")
    }
}

