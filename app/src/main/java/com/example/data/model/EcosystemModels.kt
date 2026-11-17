package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object EcosystemTaxonomy {
    val organizationTypes = listOf(
        "All",
        "Startup",
        "Company",
        "Investor",
        "Accelerator",
        "Incubator",
        "Government Program"
    )

    val fundingOrganizationTypes = listOf(
        "All Funding",
        "Investor",
        "Accelerator",
        "Incubator",
        "Government Program"
    )

    val verificationStatuses = listOf("VERIFIED", "NEEDS_REVIEW", "UNVERIFIED")
}

enum class ShareIdeaScope(val label: String, val helperText: String) {
    NONE("No idea details", "Send an introduction without disclosing private idea details."),
    TITLE_SECTOR_ONLY("Title & sector only", "Share only the title, category, and current stage."),
    PROBLEM_SUMMARY("Problem summary", "Share a brief custom/problem summary without full IP specifics."),
    FULL_PITCH("Full description", "Share full innovation description. Recommended only after trust or NDA.")
}

@IgnoreExtraProperties
data class VerifiedOrganization(
    @DocumentId val id: String = "",
    val name: String = "",
    val organizationType: String = "Startup",
    val sector: String = "",
    val description: String = "",
    val location: String = "",
    val stageOrFocus: String = "",
    val website: String? = null,
    val publicContactChannel: String? = null,
    val publicOpportunities: List<String> = emptyList(),
    val eligibility: String? = null,
    val geography: String? = null,
    val deadline: String? = null,
    val sourceName: String = "",
    val sourceUrl: String = "",
    val verificationStatus: String = "VERIFIED",
    val lastVerifiedAt: String = ""
) {
    fun isFundingRelevant(): Boolean {
        return organizationType.equals("Investor", ignoreCase = true) ||
            organizationType.equals("Accelerator", ignoreCase = true) ||
            organizationType.equals("Incubator", ignoreCase = true) ||
            organizationType.equals("Government Program", ignoreCase = true) ||
            publicOpportunities.isNotEmpty() ||
            !eligibility.isNullOrBlank()
    }

    fun isVerificationMissingOrStale(today: LocalDate = LocalDate.now()): Boolean {
        if (verificationStatus != "VERIFIED") return true
        if (sourceName.isBlank() || sourceUrl.isBlank()) return true
        if (lastVerifiedAt.isBlank()) return true
        val parsed = runCatching { LocalDate.parse(lastVerifiedAt.take(10)) }.getOrNull() ?: return true
        return ChronoUnit.DAYS.between(parsed, today) > 90L
    }

    fun toDirectorySummaryLine(): String {
        val oppSummary = if (publicOpportunities.isNotEmpty()) {
            " | Opportunities: ${publicOpportunities.take(2).joinToString("; ")}"
        } else ""
        return "- $name [$organizationType · $sector] ($location, Focus: $stageOrFocus)$oppSummary | Source: $sourceName ($sourceUrl) | Last verified: $lastVerifiedAt [$verificationStatus]"
    }

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "name" to name.trim().take(160),
            "organizationType" to organizationType.trim().take(60),
            "sector" to sector.trim().take(80),
            "description" to description.trim().take(2000),
            "location" to location.trim().take(120),
            "stageOrFocus" to stageOrFocus.trim().take(160),
            "website" to website?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "publicContactChannel" to publicContactChannel?.trim()?.takeIf { it.isNotBlank() }?.take(300),
            "publicOpportunities" to publicOpportunities.map { it.trim().take(160) }.filter { it.isNotBlank() }.take(10),
            "eligibility" to eligibility?.trim()?.takeIf { it.isNotBlank() }?.take(500),
            "geography" to geography?.trim()?.takeIf { it.isNotBlank() }?.take(120),
            "deadline" to deadline?.trim()?.takeIf { it.isNotBlank() }?.take(80),
            "sourceName" to sourceName.trim().take(160),
            "sourceUrl" to sourceUrl.trim().take(500),
            "verificationStatus" to verificationStatus,
            "lastVerifiedAt" to lastVerifiedAt.trim().take(32),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class SavedOpportunity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val organizationType: String = "",
    val sector: String = "",
    val sourceUrl: String = "",
    val lastVerifiedAt: String = ""
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "organizationId" to organizationId,
        "organizationName" to organizationName.take(160),
        "organizationType" to organizationType.take(60),
        "sector" to sector.take(80),
        "sourceUrl" to sourceUrl.take(500),
        "lastVerifiedAt" to lastVerifiedAt.take(32),
        "createdAt" to FieldValue.serverTimestamp()
    )
}

enum class MatchTargetType {
    ORGANIZATION,
    TALENT
}

data class EcosystemMatchResult(
    val targetType: MatchTargetType,
    val organization: VerifiedOrganization? = null,
    val talent: BackendTalent? = null,
    val score: Int,
    val matchReasons: List<String>,
    val supportingInfo: String,
    val sourceAttribution: String? = null,
    val lastVerifiedAt: String? = null,
    val badgeLabel: String = "Possible Match"
)

object VerifiedDirectoryJsonParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val allowedTypes = setOf(
        "Startup", "Company", "Investor", "Accelerator", "Incubator", "Government Program"
    )

    val SCHEMA_TEMPLATE_JSON: String = """
        [
          {
            "id": "<unique_slug_id>",
            "name": "<Verified Organization or Program Name>",
            "organizationType": "Startup | Company | Investor | Accelerator | Incubator | Government Program",
            "sector": "<e.g. Green Tech, AI & Robotics, AgriTech, Biotech>",
            "description": "<Concise factual summary from official source>",
            "location": "<City, State, India>",
            "stageOrFocus": "<Stage or focus area from source>",
            "website": "https://<official-domain>",
            "publicContactChannel": "<Official public portal or contact URL>",
            "publicOpportunities": ["<Verified grant, cohort, or partnership track>"],
            "eligibility": "<Published eligibility criteria if verified>",
            "geography": "<India / State scope>",
            "deadline": "<YYYY-MM-DD or Rolling if verified>",
            "sourceName": "<Official Registry / Program Page Name>",
            "sourceUrl": "https://<verifiable-source-url>",
            "verificationStatus": "VERIFIED",
            "lastVerifiedAt": "2026-11-16"
          }
        ]
    """.trimIndent()

    fun parse(rawJson: String): Result<List<VerifiedOrganization>> = runCatching {
        val trimmed = rawJson.trim()
        require(trimmed.isNotEmpty()) { "Paste a JSON array of verified organization records." }
        val element = json.parseToJsonElement(trimmed)
        require(element is JsonArray) { "JSON root must be an array `[ ... ]` of organization objects." }
        require(element.isNotEmpty()) { "The JSON array is empty." }

        val parsed = element.mapIndexed { idx, item ->
            val obj = item as? JsonObject
                ?: throw IllegalArgumentException("Entry #${idx + 1} must be a JSON object.")

            val name = obj.reqStr("name", idx)
            val orgType = obj.reqStr("organizationType", idx)
            require(allowedTypes.any { it.equals(orgType, ignoreCase = true) }) {
                "Entry #${idx + 1} ($name) has invalid organizationType '$orgType'. Allowed: ${allowedTypes.joinToString(", ")}."
            }
            val canonicalType = allowedTypes.first { it.equals(orgType, ignoreCase = true) }
            val sector = obj.reqStr("sector", idx)
            val sourceName = obj.reqStr("sourceName", idx)
            val sourceUrl = obj.reqStr("sourceUrl", idx)
            require(sourceUrl.startsWith("https://") || sourceUrl.startsWith("http://")) {
                "Entry #${idx + 1} ($name) must provide a valid http(s) sourceUrl."
            }
            val lastVerifiedAt = obj.reqStr("lastVerifiedAt", idx)
            require(runCatching { LocalDate.parse(lastVerifiedAt.take(10)) }.isSuccess) {
                "Entry #${idx + 1} ($name) must provide lastVerifiedAt in YYYY-MM-DD format."
            }
            val statusRaw = obj.optStr("verificationStatus")?.uppercase() ?: "VERIFIED"
            val verificationStatus = if (statusRaw in EcosystemTaxonomy.verificationStatuses) statusRaw else "NEEDS_REVIEW"

            val rawId = obj.optStr("id")?.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                ?: ("org_" + (name + "_" + sourceUrl).lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(48))

            val opportunities = obj["publicOpportunities"]?.let { el ->
                if (el is JsonArray) {
                    el.mapNotNull { (it as? JsonPrimitive)?.content?.trim()?.takeIf { s -> s.isNotEmpty() } }
                } else emptyList()
            }.orEmpty()

            VerifiedOrganization(
                id = rawId,
                name = name,
                organizationType = canonicalType,
                sector = sector,
                description = obj.optStr("description") ?: "",
                location = obj.optStr("location") ?: "India",
                stageOrFocus = obj.optStr("stageOrFocus") ?: "",
                website = obj.optStr("website"),
                publicContactChannel = obj.optStr("publicContactChannel"),
                publicOpportunities = opportunities,
                eligibility = obj.optStr("eligibility"),
                geography = obj.optStr("geography"),
                deadline = obj.optStr("deadline"),
                sourceName = sourceName,
                sourceUrl = sourceUrl,
                verificationStatus = verificationStatus,
                lastVerifiedAt = lastVerifiedAt.take(10)
            )
        }
        parsed
    }

    private fun JsonObject.optStr(key: String): String? =
        (this[key] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private fun JsonObject.reqStr(key: String, idx: Int): String =
        optStr(key) ?: throw IllegalArgumentException("Entry #${idx + 1} is missing required field '$key'.")
}
