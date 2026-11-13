package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.BackendInnovation
import com.example.data.model.CopilotMessage
import com.example.data.model.Innovation
import com.example.data.model.InvestorOnePager
import com.example.data.model.PitchDeckResult
import com.example.data.model.PitchSlide
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiCopilotRepository"
private const val AI_TIMEOUT_MS = 60_000L

class AiCopilotRepository {

    private val deepSystemPromptText = """
        You are AI Sana, the senior software, hardware, IP, and venture architect built into IDEA2IMPACT.
        You advise inventors, deep-tech builders, patent attorneys, and angel/VC investors.

        Response Standards:
        1. Lead with precision: start immediately with the core answer, verdict, or architecture decision—no empty pleasantries ("Great question!", "Sure!") or generic disclaimers.
        2. Think critically: explicitly call out key assumptions, technical bottlenecks, prior-art or regulatory edge cases, and trade-offs instead of giving blind praise.
        3. Be concrete and quantitative: when sizing markets (TAM/SAM/SOM), estimating BOM/MVP timelines, structuring equity, or outlining patent claims, provide bottom-up reasoning, realistic numeric ranges, and named protocols/components.
        4. Format for mobile scannability: use crisp markdown headings (## or ###), concise bullets (-), indented sub-bullets (  -) for trade-offs, numbered steps (1.) for plans, and fenced code blocks (```) for code, schemas, BOM tables, or formulas.
        5. Close actionable requests with a brief "### Recommended Next Steps" section (2–3 concrete actions the user can execute next).
        6. Never refer to yourself as an AI language model, Gemini, or AI Copilot—you are AI Sana.
    """.trimIndent()

    private val fastSystemPromptText = """
        You are AI Sana (Fast Mode), a sharp senior engineering & venture advisor on IDEA2IMPACT.
        Give direct, high-signal, scannable answers with ### headings and concise (-) bullets. Skip filler, state key risks/assumptions clearly, and finish with 2 concrete next steps when helpful. Never mention Gemini or underlying model names.
    """.trimIndent()

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.45f
                topP = 0.9f
                maxOutputTokens = 2048
            },
            systemInstruction = content { text(deepSystemPromptText) }
        )
    }

    private val fastChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                temperature = 0.55f
                topP = 0.9f
                maxOutputTokens = 1200
            },
            systemInstruction = content { text(fastSystemPromptText) }
        )
    }

    private val onePagerSchema = Schema.obj(
        properties = mapOf(
            "executiveSummary" to Schema.string(description = "2-3 sentence investment thesis summary"),
            "problemStatement" to Schema.string(description = "Specific market pain point and urgency"),
            "solutionAdvantage" to Schema.string(description = "Technical differentiation and defensible IP moat"),
            "marketOpportunity" to Schema.string(description = "Bottom-up TAM/SAM/SOM estimate and target customer"),
            "suggestedTranche" to Schema.string(description = "Recommended pre-seed/seed raise and use of funds"),
            "milestones" to Schema.array(
                items = Schema.string(),
                description = "4 concrete 90-day validation or commercialization milestones"
            )
        )
    )

    private val onePagerModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = onePagerSchema
            }
        )
    }

    private val pitchDeckSchema = Schema.obj(
        properties = mapOf(
            "slides" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "slideNumber" to Schema.integer(description = "1 to 5"),
                        "title" to Schema.string(description = "Crisp slide heading, e.g., 01. Vision & Opportunity"),
                        "subtitle" to Schema.string(description = "One-line takeaway for investors"),
                        "bullets" to Schema.array(
                            items = Schema.string(),
                            description = "3 concise institutional-grade bullet points"
                        )
                    )
                ),
                description = "Exactly 5 executive investor pitch deck slides"
            )
        )
    )

    private val pitchDeckModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = pitchDeckSchema
            }
        )
    }

    fun streamBackendCopilotReply(
        prompt: String,
        history: List<CopilotMessage>,
        activeInnovation: BackendInnovation?,
        attachedImage: Bitmap? = null,
        useFastModel: Boolean = false,
        catalogContext: List<BackendInnovation> = emptyList()
    ): Flow<String> = flow {
        val model = if (useFastModel) fastChatModel else chatModel

        val recentHistory = history
            .filter {
                it.id != "welcome" &&
                    !it.isError &&
                    it.content.isNotBlank() &&
                    !it.content.startsWith("### AI Request Error") &&
                    !it.content.startsWith("### Connection Issue")
            }
            .takeLast(12)
            .map { msg ->
                content(role = if (msg.isAssistant) "model" else "user") {
                    text(msg.content.removeSuffix("\n\n*(Generation stopped)*"))
                }
            }

        val currentPromptText = buildString {
            if (activeInnovation != null) {
                appendLine("Focused Innovation Context:")
                appendLine("- Title: ${activeInnovation.title}")
                appendLine("- Category: ${activeInnovation.category} | Stage: ${activeInnovation.stage}")
                appendLine("- Creator: ${activeInnovation.authorName} (${activeInnovation.authorTitle})")
                appendLine("- Funding Goal: $${activeInnovation.fundingGoal.toInt()} | Valuation: $${activeInnovation.valuation.toInt()}")
                appendLine("- NDA Required: ${activeInnovation.ndaRequired} | Upvotes: ${activeInnovation.likeCount}")
                appendLine("- Summary: ${activeInnovation.description}")
                appendLine()
            } else if (catalogContext.isNotEmpty()) {
                appendLine("Live IDEA2IMPACT Marketplace Snapshot (use if the user asks for comparisons or recommendations):")
                catalogContext.take(6).forEach { item ->
                    appendLine("- ${item.title} [${item.category} · ${item.stage}] — Goal $${item.fundingGoal.toInt()}, Val $${item.valuation.toInt()}")
                }
                appendLine()
            }
            if (attachedImage != null) {
                appendLine("Inspect the attached prototype, schematic, or diagram carefully and integrate visual observations directly into your analysis.")
            }
            append("User Request: ")
            append(prompt)
        }

        val currentContent = content(role = "user") {
            if (attachedImage != null) {
                image(attachedImage)
            }
            text(currentPromptText)
        }

        val requestContents = recentHistory + currentContent
        val accumulated = StringBuilder()

        try {
            withTimeout(AI_TIMEOUT_MS) {
                model.generateContentStream(requestContents).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Sana returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Sana streaming timed out", e)
            throw IllegalStateException("AI Sana timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Sana streaming failed", e)
            throw e
        }
    }

    suspend fun generateBackendPitchDeck(innovation: BackendInnovation): PitchDeckResult {
        val prompt = """
            Generate a crisp 5-slide institutional investor pitch deck for this innovation:
            Title: ${innovation.title}
            Category: ${innovation.category}
            Stage: ${innovation.stage}
            Creator: ${innovation.authorName} (${innovation.authorTitle})
            Funding Target: $${innovation.fundingGoal.toInt()}
            Target Valuation: $${innovation.valuation.toInt()}
            NDA Required: ${innovation.ndaRequired}
            Description: ${innovation.description}
            
            Slide 1: Vision & Problem
            Slide 2: Technical Breakthrough & Architecture
            Slide 3: Market Opportunity & Unit Economics
            Slide 4: IP Moat & Defensibility
            Slide 5: Capital Allocation & 90-Day Roadmap
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                pitchDeckModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty structured response from AI Sana.")
            val root = JSONObject(jsonText)
            val slidesArray = root.getJSONArray("slides")
            val slides = (0 until slidesArray.length()).map { idx ->
                val obj = slidesArray.getJSONObject(idx)
                val bulletsArr = obj.optJSONArray("bullets")
                val bullets = if (bulletsArr != null) {
                    (0 until bulletsArr.length()).map { bulletsArr.getString(it) }
                } else emptyList()
                PitchSlide(
                    slideNumber = obj.optInt("slideNumber", idx + 1),
                    title = obj.getString("title"),
                    subtitle = obj.optString("subtitle", ""),
                    bullets = bullets
                )
            }.take(5)

            if (slides.isEmpty()) {
                throw IllegalStateException("AI Sana returned 0 pitch deck slides.")
            }
            PitchDeckResult(innovationTitle = innovation.title, slides = slides)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Pitch deck generation timed out", e)
            throw IllegalStateException("Pitch deck generation timed out after 60 seconds. Please retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate structured pitch deck", e)
            throw e
        }
    }

    fun streamCopilotReply(prompt: String, activeInnovation: Innovation?): Flow<String> = flow {
        val fullPrompt = buildString {
            if (activeInnovation != null) {
                appendLine("Active Innovation Context:")
                appendLine("Title: ${activeInnovation.title}")
                appendLine("Category: ${activeInnovation.category}")
                appendLine("Valuation / Ask: $${activeInnovation.price.toInt()}")
                appendLine("Creator: ${activeInnovation.authorName}")
                appendLine("Description: ${activeInnovation.description}")
                appendLine()
            }
            append("User Request: ")
            append(prompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                chatModel.generateContentStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI Sana returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI Sana streaming timed out", e)
            throw IllegalStateException("AI Sana timed out after 60 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Sana streaming failed", e)
            throw e
        }
    }

    suspend fun generateInvestorOnePager(innovation: Innovation): InvestorOnePager {
        val prompt = """
            Generate a crisp institutional-grade Investor One-Pager memo for this innovation:
            Title: ${innovation.title}
            Category: ${innovation.category}
            Creator: ${innovation.authorName} (${innovation.authorHeadline ?: "Innovator"})
            Licensing Open: ${innovation.licensing}
            Funding Open: ${innovation.funding}
            Asking Price / Seed Target: $${innovation.price.toInt()}
            Description: ${innovation.description}
        """.trimIndent()

        return try {
            val response = withTimeout(AI_TIMEOUT_MS) {
                onePagerModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("Empty structured response from AI Sana.")
            val json = JSONObject(jsonText)
            val milestonesArr = json.optJSONArray("milestones")
            val milestones = if (milestonesArr != null) {
                (0 until milestonesArr.length()).map { milestonesArr.getString(it) }
            } else emptyList()

            InvestorOnePager(
                executiveSummary = json.getString("executiveSummary"),
                problemStatement = json.getString("problemStatement"),
                solutionAdvantage = json.getString("solutionAdvantage"),
                marketOpportunity = json.getString("marketOpportunity"),
                suggestedTranche = json.getString("suggestedTranche"),
                milestones = milestones
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Investor One-Pager timed out", e)
            throw IllegalStateException("One-Pager generation timed out after 60 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate Investor One-Pager", e)
            throw e
        }
    }
}
