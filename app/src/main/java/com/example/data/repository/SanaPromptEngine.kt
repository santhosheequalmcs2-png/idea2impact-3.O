package com.example.data.repository

import com.example.data.model.AiFailureCategory
import com.example.data.model.AiRequestPhase
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.CopilotMessage

data class SanaContextBundle(
    val isGreeting: Boolean,
    val isIdeaEvaluation: Boolean,
    val needsEcosystemContext: Boolean,
    val isUnrelatedGeneral: Boolean,
    val referenceContextBlock: String
)

object SanaPromptEngine {

    private val GREETING_REGEX = Regex(
        "^(hi+|hello+|hey+|yo|hola|namaste|good morning|good afternoon|good evening|thanks|thank you|ok|okay|cool|how are you|what's up|whats up|sup)(\\s+(there|everyone|team|(ai\\s+)?sana))?([,\\s]+(how are you|what's up|how's it going))?[!.?\\s]*$",
        RegexOption.IGNORE_CASE
    )

    private val ECOSYSTEM_WORD_REGEX = Regex(
        "\\b(startup|startups|company|companies|investor|investors|vc|angel|builder|builders|collaborator|collaborators|cofounder|co-founder|mentor|mentors|grant|grants|accelerator|incubator|incubators|program|programs|directory|marketplace|listing|listings|innovation|innovations|partner|partners|patent attorney|talent|talents|ecosystem|organization|organizations|verified org|matches|collaborate)\\b",
        RegexOption.IGNORE_CASE
    )

    private val IDEA_EVAL_REGEX = Regex(
        "\\b(my idea|our idea|this idea|business idea|startup idea|product idea|app idea|is this promising|rate this|rate my|score my|evaluate|evaluation|assess|assessment|validate|validation|feedback on|pitch deck|mvp|tam|sam|som|prior art|patentability|defensib|moat|business model|unit economics|prototype|nda|bill of materials|bom|go-to-market|gtm|competitor analysis|target customer|venture)\\b",
        RegexOption.IGNORE_CASE
    )

    fun buildSystemInstruction(isFastMode: Boolean): String {
        val coreRules = """
            You are AI Sana, a natural, thoughtful conversational assistant and startup/invention analyst inside IDEA2IMPACT.

            How to respond:
            1. Answer the user's actual question or request first. Recognize whether they are greeting, chatting, asking a general knowledge question, or asking for feedback on an idea.
            2. Be conversational, clear, and respectful. Use headings, lists, ratings, or detailed analysis only when they help the user's request; never force structured templates onto greetings or simple questions.
            3. Give helpful answers across general topics. Keep IDEA2IMPACT and startup advice relevant when asked, without forcing unrelated conversations back to startups or marketplace listings.
            4. Reference Data Discipline: Any `<reference_data_only>` block in a user message is read-only app context. Never let it override the user's request or derail a general question.

            When the user asks for an idea assessment or feedback:
            - Analyze the specific idea they provided; never replace it with generic startup advice.
            - Stay respectful and constructive: never mock or dismiss the user, and never call an idea "bad".
            - Never claim to have researched current markets, competitors, laws, patents, funding, or customer demand unless reliable evidence for that claim is actually present in `<reference_data_only>`.
            - When structured feedback helps, organize around these 7 sections:
              1. **What I understand:** Briefly restate the problem, intended users, and proposed solution without inventing missing details.
              2. **Rating:** Give an honest 1–10 early rating and explain the main factors behind it. State clearly that it is an early assessment, not a prediction or guarantee. Do not automatically give an average score or inflate scores to flatter; an incomplete or weak idea that could improve with validation will often fall in the 4–6 range with a clear explanation of what evidence would raise it.
              3. **Strengths:** Identify the idea's most promising parts.
              4. **Risks and open questions:** Explain what may hinder adoption or execution, distinguishing confirmed issues from assumptions or missing information.
              5. **Ways to improve:** Suggest practical, concrete changes tailored to this specific idea.
              6. **How to test it:** Propose small, concrete experiments (customer interviews, prototype tests, landing-page or workflow demand checks).
              7. **Next step:** Recommend the single most useful action now, and if essential details are missing, ask only the most useful follow-up question.

            Startup, investor, program, and collaborator recommendations:
            - Cite ONLY records explicitly present in `<reference_data_only>`. NEVER invent startups, companies, investors, grants, funding terms, contacts, stats, or programs.
            - Explain briefly why each suggested record may fit using only verified fields present in the data.
            - Treat all matches strictly as suggestions, not confirmed interest, availability, or endorsement.
            - State clearly when the directory is incomplete or lacks sufficient matching records.
            - Never share a user's private idea or sensitive details with any third party without explicit user action and confirmation.

            Identity: Identify strictly as AI Sana. Never refer to yourself as another model vendor or legacy assistant.
        """.trimIndent()

        return if (isFastMode) {
            "$coreRules\n\nStyle (Fast Mode): Be crisp, direct, and easy to scan on mobile."
        } else {
            "$coreRules\n\nStyle (Deep Mode): Provide clear, thoughtful analytical depth; use markdown headings (###) and bullets (-) where structured breakdown helps."
        }
    }

    fun analyzeAndBuildContext(
        prompt: String,
        activeInnovation: BackendInnovation?,
        catalogContext: List<BackendInnovation> = emptyList(),
        talentContext: List<BackendTalent> = emptyList(),
        directorySummaryLines: List<String> = emptyList(),
        hasAttachedImage: Boolean = false
    ): SanaContextBundle {
        val clean = prompt.trim()
        val lower = clean.lowercase()

        val isGreeting = !hasAttachedImage && GREETING_REGEX.matches(clean)
        val needsEcosystem = !isGreeting && ECOSYSTEM_WORD_REGEX.containsMatchIn(lower)
        val isIdeaEval = !isGreeting && (
            IDEA_EVAL_REGEX.containsMatchIn(lower) ||
                ((lower.contains("idea") || lower.contains("startup") || lower.contains("product")) &&
                    (lower.contains("feedback") || lower.contains("review") || lower.contains("improve") ||
                        lower.contains("test") || lower.contains("risk") || lower.contains("promising") ||
                        lower.contains("rating") || lower.contains("strength")))
            )
        val mentionsApp = lower.contains("idea2impact") || lower.contains("ai sana") || lower.contains("sana")
        val referencesFocusedItem = activeInnovation != null && !isGreeting && (
            Regex("\\b(this innovation|this project|this listing|this prototype|this deck|focused item|above project)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower) ||
                activeInnovation.title.lowercase().split(Regex("\\s+")).any { token ->
                    token.length >= 4 && lower.contains(token)
                }
            )

        val shouldIncludeFocusedInnovation = activeInnovation != null &&
            !isGreeting &&
            (referencesFocusedItem || hasAttachedImage || (needsEcosystem && lower.contains("this")) || (isIdeaEval && !lower.contains("my idea")))

        val isUnrelatedGeneral = !isGreeting &&
            !shouldIncludeFocusedInnovation &&
            !needsEcosystem &&
            !isIdeaEval &&
            !hasAttachedImage &&
            !mentionsApp

        val referenceBlock = buildString {
            if (isGreeting || isUnrelatedGeneral) {
                return@buildString
            }

            val sections = mutableListOf<String>()

            if (shouldIncludeFocusedInnovation && activeInnovation != null) {
                sections += buildString {
                    appendLine("Focused App Innovation (for reference if relevant):")
                    appendLine("- Title: ${activeInnovation.title}")
                    appendLine("- Category: ${activeInnovation.category} | Stage: ${activeInnovation.stage}")
                    appendLine("- Creator: ${activeInnovation.authorName} (${activeInnovation.authorTitle})")
                    appendLine("- Funding Goal: $${activeInnovation.fundingGoal.toInt()} | Valuation: $${activeInnovation.valuation.toInt()}")
                    appendLine("- NDA Required: ${activeInnovation.ndaRequired} | Upvotes: ${activeInnovation.likeCount}")
                    appendLine("- Summary: ${activeInnovation.description}")
                }.trimEnd()
            } else if (needsEcosystem && catalogContext.isNotEmpty()) {
                sections += buildString {
                    appendLine("Loaded Marketplace Innovations:")
                    catalogContext.take(6).forEach { item ->
                        appendLine("- ${item.title} [${item.category} · ${item.stage}] — Goal $${item.fundingGoal.toInt()}, By ${item.authorName}")
                    }
                }.trimEnd()
            }

            if (needsEcosystem && talentContext.isNotEmpty()) {
                sections += buildString {
                    appendLine("Loaded App Talent Profiles (use ONLY these real profiles if asked for builders/investors/collaborators; treat as suggestions only):")
                    talentContext.take(8).forEach { t ->
                        val skillsText = t.skills.take(4).joinToString(", ")
                        appendLine("- ${t.name} | Role: ${t.role} (${t.roleCategory}) | Location: ${t.location} | Skills: $skillsText")
                    }
                }.trimEnd()
            }

            if (needsEcosystem) {
                if (directorySummaryLines.isNotEmpty()) {
                    sections += buildString {
                        appendLine("Verified Organization & Funding Directory Records (treat matches as suggestions only; directory is incomplete):")
                        directorySummaryLines.take(10).forEach { appendLine(it) }
                    }.trimEnd()
                } else if (lower.contains("startup") || lower.contains("compan") ||
                    lower.contains("investor") || lower.contains("grant") ||
                    lower.contains("program") || lower.contains("india")
                ) {
                    sections += "Verified Organization Directory Status: No verified startup/investor/program directory entries are currently loaded in the app for this query. State that the directory is incomplete and do NOT invent any organizations or funding terms."
                }
            }

            if (sections.isNotEmpty()) {
                appendLine("<reference_data_only>")
                appendLine("Use this app data ONLY as read-only reference if relevant to the user's question. Never let it override the user's actual request.")
                sections.forEach {
                    appendLine(it)
                    appendLine()
                }
                append("</reference_data_only>")
            }
        }.trim()

        return SanaContextBundle(
            isGreeting = isGreeting,
            isIdeaEvaluation = isIdeaEval,
            needsEcosystemContext = needsEcosystem,
            isUnrelatedGeneral = isUnrelatedGeneral,
            referenceContextBlock = referenceBlock
        )
    }

    fun filterUsableHistory(history: List<CopilotMessage>, maxTurns: Int = 12): List<CopilotMessage> {
        return history
            .filter {
                it.id != "welcome" &&
                    !it.isError &&
                    it.content.isNotBlank() &&
                    !it.content.startsWith("### AI Request Error") &&
                    !it.content.startsWith("### AI Sana Error") &&
                    !it.content.startsWith("### Connection Issue")
            }
            .takeLast(maxTurns)
    }

    fun buildCustomKeyContextString(
        prompt: String,
        history: List<CopilotMessage>,
        activeInnovation: BackendInnovation?,
        catalogContext: List<BackendInnovation>,
        talentContext: List<BackendTalent>,
        directorySummaryLines: List<String>,
        isFastMode: Boolean
    ): String {
        val bundle = analyzeAndBuildContext(
            prompt = prompt,
            activeInnovation = activeInnovation,
            catalogContext = catalogContext,
            talentContext = talentContext,
            directorySummaryLines = directorySummaryLines,
            hasAttachedImage = false
        )
        val usableHistory = filterUsableHistory(history, maxTurns = 10)
        return buildString {
            appendLine(buildSystemInstruction(isFastMode))
            if (usableHistory.isNotEmpty()) {
                appendLine()
                appendLine("Recent Conversation History:")
                usableHistory.forEach { msg ->
                    val speaker = if (msg.isAssistant) "AI Sana" else "User"
                    appendLine("$speaker: ${msg.content.removeSuffix("\n\n*(Generation stopped)*")}")
                }
            }
            if (bundle.referenceContextBlock.isNotBlank()) {
                appendLine()
                appendLine(bundle.referenceContextBlock)
            }
        }.trim()
    }

    private val API_KEY_SECRET_REGEX = Regex("AIza[0-9A-Za-z_-]{10,}")
    private val BEARER_TOKEN_REGEX = Regex("Bearer\\s+[A-Za-z0-9._~+/=-]+", RegexOption.IGNORE_CASE)

    fun redactSensitiveInfo(raw: String): String {
        if (raw.isBlank()) return ""
        return raw
            .replace(API_KEY_SECRET_REGEX, "AIza***")
            .replace(BEARER_TOKEN_REGEX, "Bearer ***")
    }

    fun classifyAiError(error: Throwable): AiFailureCategory {
        val diag = error as? AiCopilotDiagnosticException
        if (diag != null) {
            if (diag.phase == "empty_completion") {
                return AiFailureCategory.EMPTY_RESPONSE
            }
            if (diag.category != AiFailureCategory.UNKNOWN) {
                return diag.category
            }
        }

        val visited = hashSetOf<Throwable>()
        var current: Throwable? = error
        while (current != null && visited.add(current)) {
            val className = current.javaClass.simpleName.lowercase()
            val msgLower = current.message.orEmpty().lowercase()

            when (current) {
                is kotlinx.coroutines.TimeoutCancellationException,
                is java.net.SocketTimeoutException,
                is java.io.InterruptedIOException -> return AiFailureCategory.TIMEOUT
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.NoRouteToHostException,
                is javax.net.ssl.SSLHandshakeException -> return AiFailureCategory.NETWORK_OFFLINE
                is BackendApiException -> when (current.code) {
                    401, 403 -> return AiFailureCategory.APP_CHECK_OR_AUTH
                    404 -> return if (msgLower.contains("model")) AiFailureCategory.MODEL_UNAVAILABLE else AiFailureCategory.CUSTOM_KEY_OR_BACKEND
                    408, 504 -> return AiFailureCategory.TIMEOUT
                    429 -> return AiFailureCategory.QUOTA_OR_RATE_LIMIT
                    500, 502, 503 -> return AiFailureCategory.PROVIDER_OVERLOAD
                    in 400..499 -> return AiFailureCategory.CUSTOM_KEY_OR_BACKEND
                    else -> return AiFailureCategory.CUSTOM_KEY_OR_BACKEND
                }
            }

            if (className.contains("serialization")) {
                return AiFailureCategory.CUSTOM_KEY_OR_BACKEND
            }

            if (msgLower.contains("timed out") ||
                msgLower.contains("timeout") ||
                msgLower.contains("deadline exceeded") ||
                msgLower.contains("deadline_exceeded")
            ) {
                return AiFailureCategory.TIMEOUT
            }
            if (className.contains("quota") ||
                msgLower.contains("quota") ||
                msgLower.contains("resource_exhausted") ||
                msgLower.contains("rate limit") ||
                msgLower.contains("429")
            ) {
                return AiFailureCategory.QUOTA_OR_RATE_LIMIT
            }
            if (className.contains("invalidapikey") ||
                msgLower.contains("app check") ||
                msgLower.contains("attestation") ||
                msgLower.contains("permission_denied") ||
                msgLower.contains("unauthenticated") ||
                msgLower.contains("api key not valid") ||
                msgLower.contains("403")
            ) {
                return AiFailureCategory.APP_CHECK_OR_AUTH
            }
            if (msgLower.contains("model") &&
                (msgLower.contains("not found") ||
                    msgLower.contains("not_found") ||
                    msgLower.contains("unsupported") ||
                    msgLower.contains("deprecated") ||
                    msgLower.contains("invalid") ||
                    msgLower.contains("unavailable"))
            ) {
                return AiFailureCategory.MODEL_UNAVAILABLE
            }
            if (msgLower.contains("404") || msgLower.contains("not_found")) {
                return AiFailureCategory.MODEL_UNAVAILABLE
            }
            if (className.contains("serviceunavailable") ||
                className.contains("serverexception") ||
                msgLower.contains("overloaded") ||
                msgLower.contains("service unavailable") ||
                msgLower.contains("temporarily unavailable") ||
                msgLower.contains("503") ||
                msgLower.contains("502") ||
                msgLower.contains("capacity") ||
                msgLower.contains("high demand")
            ) {
                return AiFailureCategory.PROVIDER_OVERLOAD
            }
            if (msgLower.contains("unable to resolve host") ||
                msgLower.contains("network is unreachable") ||
                msgLower.contains("failed to connect") ||
                msgLower.contains("no address associated") ||
                msgLower.contains("connection reset") ||
                msgLower.contains("software caused connection abort") ||
                msgLower.contains("network")
            ) {
                return if (diag != null && diag.chunkCount > 0) {
                    AiFailureCategory.STREAMING_INTERRUPTION
                } else {
                    AiFailureCategory.NETWORK_OFFLINE
                }
            }
            if (msgLower.contains("empty") &&
                (msgLower.contains("response") || msgLower.contains("reply") || msgLower.contains("stream"))
            ) {
                return AiFailureCategory.EMPTY_RESPONSE
            }
            current = current.cause
        }

        if (diag != null) {
            if (diag.appCheckStatus.startsWith("probe_error")) {
                return AiFailureCategory.APP_CHECK_OR_AUTH
            }
            if (diag.chunkCount > 0 || diag.phase == "streaming_chunks") {
                return AiFailureCategory.STREAMING_INTERRUPTION
            }
        }
        return AiFailureCategory.UNKNOWN
    }

    fun diagnoseAiError(error: Throwable, routeName: String): String {
        val category = classifyAiError(error)
        if (error is AiCopilotDiagnosticException) {
            val cause = error.cause ?: error
            val sanitizedCause = redactSensitiveInfo(cause.message.orEmpty()).ifBlank { cause.javaClass.simpleName }
            val primaryNoteSuffix = error.primaryFailureNote
                ?.takeIf { it.isNotBlank() }
                ?.let { " Initial attempt note: ${redactSensitiveInfo(it)}." }
                .orEmpty()

            return when (category) {
                AiFailureCategory.APP_CHECK_OR_AUTH ->
                    "Firebase AI / App Check rejected the request on ${error.route} (${error.modelName}, phase=${error.phase}, appCheck=${error.appCheckStatus}). Check Firebase App Check debug token or API access, or toggle Fast mode and tap Retry.$primaryNoteSuffix"
                AiFailureCategory.TIMEOUT -> {
                    val phaseHint = when (error.phase) {
                        "app_check_probe" -> "while acquiring Firebase App Check token"
                        "awaiting_first_chunk" -> "before the first response token arrived (${error.elapsedMs / 1000}s)"
                        "streaming_chunks" -> "mid-stream after receiving ${error.chunkCount} chunks"
                        else -> "during ${error.phase}"
                    }
                    "AI Sana timed out $phaseHint (${error.route} · ${error.modelName}, appCheck=${error.appCheckStatus}). Check your network connection or toggle Fast mode and tap Retry.$primaryNoteSuffix"
                }
                AiFailureCategory.NETWORK_OFFLINE ->
                    "Unable to reach Firebase AI servers (${error.route} · ${error.modelName}) during ${error.phase}. Check internet connectivity and tap Retry.$primaryNoteSuffix"
                AiFailureCategory.MODEL_UNAVAILABLE ->
                    "The selected AI model (${error.modelName}) is currently unavailable or unsupported on ${error.route} (phase=${error.phase}): $sanitizedCause. Switch modes or tap Retry.$primaryNoteSuffix"
                AiFailureCategory.QUOTA_OR_RATE_LIMIT ->
                    "AI request quota or rate limit was temporarily reached (${error.route} · ${error.modelName}). Switch to Fast mode or wait a moment, then tap Retry.$primaryNoteSuffix"
                AiFailureCategory.PROVIDER_OVERLOAD ->
                    "The AI model provider is temporarily overloaded (${error.route} · ${error.modelName}, phase=${error.phase}). Tap Retry or switch to Fast mode.$primaryNoteSuffix"
                AiFailureCategory.STREAMING_INTERRUPTION ->
                    "The response stream was interrupted mid-answer after ${error.chunkCount} chunks (${error.route} · ${error.modelName}): $sanitizedCause. Partial output was preserved above; tap Retry to regenerate cleanly.$primaryNoteSuffix"
                AiFailureCategory.EMPTY_RESPONSE ->
                    "AI Sana received an incomplete or empty response (${error.route} · ${error.modelName}). Tap Retry to try again.$primaryNoteSuffix"
                else ->
                    "AI Sana request failed on ${error.route} (${error.modelName}) during ${error.phase} (appCheck=${error.appCheckStatus}): $sanitizedCause. Tap Retry or try Fast mode.$primaryNoteSuffix"
            }
        }

        val sanitizedMsg = redactSensitiveInfo(error.message.orEmpty())
        if (error is BackendApiException) {
            return when (error.code) {
                401, 403 -> "Your custom AI Studio key or authorization was rejected (HTTP ${error.code}). Verify your key in Key Settings or switch back to Default AI, then tap Retry."
                404 -> "The custom AI backend endpoint was not found (HTTP 404). Clear your custom key to use Default AI, or check backend availability."
                429 -> "Rate limit or quota reached on the custom key route (HTTP 429). Wait a moment or switch to Default AI, then tap Retry."
                502, 503, 504 -> "The AI backend server is waking up or temporarily unavailable (HTTP ${error.code}). Wait a few seconds and tap Retry, or clear your custom key to use Default AI."
                in 400..499 -> "The AI backend could not process this request (HTTP ${error.code}: $sanitizedMsg). Simplify your prompt or check your custom key."
                else -> "The AI backend returned an error (HTTP ${error.code}: $sanitizedMsg). Please tap Retry or switch to Default AI."
            }
        }

        return when (category) {
            AiFailureCategory.TIMEOUT ->
                "AI Sana timed out while waiting for a response ($routeName). Check your connection or toggle Fast mode and tap Retry."
            AiFailureCategory.NETWORK_OFFLINE ->
                "Unable to reach AI Sana right now. Check your internet connection and tap Retry."
            AiFailureCategory.APP_CHECK_OR_AUTH ->
                "Firebase AI / App Check authorization failed. Verify device network/time or Google Play Services, then tap Retry."
            AiFailureCategory.MODEL_UNAVAILABLE ->
                "The requested AI model is currently unavailable ($routeName): $sanitizedMsg. Switch modes or tap Retry."
            AiFailureCategory.QUOTA_OR_RATE_LIMIT ->
                "AI request quota or rate limit was temporarily exceeded. Switch to Fast mode or wait a moment, then tap Retry."
            AiFailureCategory.PROVIDER_OVERLOAD ->
                "The AI provider is temporarily overloaded ($routeName). Wait a moment and tap Retry, or switch modes."
            AiFailureCategory.CUSTOM_KEY_OR_BACKEND ->
                "The custom AI backend returned an invalid or unreadable response ($routeName): $sanitizedMsg. Check your custom key or switch to Default AI."
            AiFailureCategory.EMPTY_RESPONSE ->
                "AI Sana received an incomplete or empty response ($routeName). Tap Retry to try again."
            else ->
                sanitizedMsg.takeIf { it.isNotBlank() }
                    ?: "Unable to complete AI Sana request ($routeName). Check connectivity and tap Retry."
        }
    }

    fun isTimeoutException(error: Throwable): Boolean {
        return classifyAiError(error) == AiFailureCategory.TIMEOUT
    }

    fun resolveStatusLabel(
        phase: AiRequestPhase,
        elapsedMs: Long,
        hasAttachedImage: Boolean = false,
        isCustomKey: Boolean = false
    ): String {
        val elapsedSec = (elapsedMs / 1000L).coerceAtLeast(1L)
        return when (phase) {
            AiRequestPhase.IDLE -> ""
            AiRequestPhase.CHECKING_CONNECTION -> {
                if (elapsedMs >= 4_000L) "Still checking connection..." else "Checking connection..."
            }
            AiRequestPhase.THINKING -> when {
                elapsedMs >= 15_000L -> "Still waiting for response (${elapsedSec}s)..."
                elapsedMs >= 6_000L -> if (isCustomKey) {
                    "Waiting for AI backend (${elapsedSec}s)..."
                } else {
                    "Still thinking (${elapsedSec}s)..."
                }
                hasAttachedImage -> "Analyzing prototype image..."
                else -> "Thinking..."
            }
            AiRequestPhase.RETRYING -> "Provider busy · retrying (${elapsedSec}s)..."
            AiRequestPhase.SWITCHING_MODEL -> "Switching to AI Sana Fast..."
            AiRequestPhase.STREAMING -> "Streaming response..."
        }
    }

    fun resolveStatusHint(
        phase: AiRequestPhase,
        elapsedMs: Long,
        isFastMode: Boolean
    ): String {
        return when (phase) {
            AiRequestPhase.IDLE -> ""
            AiRequestPhase.CHECKING_CONNECTION ->
                "Verifying network and AI access..."
            AiRequestPhase.THINKING -> when {
                elapsedMs >= 15_000L -> if (isFastMode) {
                    "Response is taking longer than usual. You can Stop anytime."
                } else {
                    "Deep analysis is taking longer than usual. Toggle Fast mode or Stop anytime."
                }
                elapsedMs >= 6_000L -> if (isFastMode) {
                    "Waiting for first response token..."
                } else {
                    "Deep mode is reasoning before streaming the first token..."
                }
                else -> "Preparing AI Sana response..."
            }
            AiRequestPhase.RETRYING ->
                "Temporary provider load before first response token; backing off briefly..."
            AiRequestPhase.SWITCHING_MODEL ->
                "Deep mode is busy; switching to Fast mode (gemini-3.5-flash-lite) with your history intact..."
            AiRequestPhase.STREAMING ->
                "Receiving live tokens..."
        }
    }
}
