package com.example.data.repository

import com.example.data.model.BackendTalent
import com.example.data.model.EcosystemMatchResult
import com.example.data.model.MatchTargetType
import com.example.data.model.ShareIdeaScope
import com.example.data.model.VerifiedOrganization

object EcosystemMatcher {

    private val STOP_WORDS = setOf(
        "about", "after", "and", "also", "been", "build", "building", "built", "could", "early",
        "enable", "for", "from", "have", "idea", "ideas", "impact", "into", "more", "most",
        "near", "only", "platform", "product", "solution", "startup", "system", "systems",
        "that", "their", "them", "there", "these", "this", "using", "with", "would", "your"
    )

    fun extractKeywords(vararg texts: String): Set<String> {
        return texts
            .flatMap { text ->
                text.lowercase()
                    .split(Regex("[^a-z0-9+]+"))
                    .map { it.trim() }
                    .filter { it.length >= 3 && it !in STOP_WORDS }
            }
            .toSet()
    }

    fun matchIdeaToEcosystem(
        ideaTitle: String,
        ideaDescription: String,
        ideaCategory: String,
        ideaStage: String = "",
        preferredLocation: String = "",
        organizations: List<VerifiedOrganization>,
        talents: List<BackendTalent>,
        maxOrgMatches: Int = 8,
        maxTalentMatches: Int = 8
    ): List<EcosystemMatchResult> {
        val ideaKeywords = extractKeywords(ideaTitle, ideaDescription, ideaCategory)
        val catLower = ideaCategory.trim().lowercase()
        val stageLower = ideaStage.trim().lowercase()
        val locLower = preferredLocation.trim().lowercase()

        val orgMatches = organizations.mapNotNull { org ->
            val reasons = mutableListOf<String>()
            var score = 0

            val sectorLower = org.sector.lowercase()
            if (catLower.isNotBlank() && (
                    sectorLower.contains(catLower) ||
                        catLower.contains(sectorLower) ||
                        extractKeywords(ideaCategory).any { sectorLower.contains(it) }
                    )
            ) {
                score += 4
                reasons += "Sector match: ${org.sector}"
            }

            val orgKeywords = extractKeywords(
                org.name,
                org.sector,
                org.description,
                org.stageOrFocus,
                org.publicOpportunities.joinToString(" "),
                org.eligibility.orEmpty()
            )
            val overlap = ideaKeywords.intersect(orgKeywords).sorted().take(4)
            if (overlap.isNotEmpty()) {
                score += overlap.size * 2
                reasons += "Topic keywords: ${overlap.joinToString(", ")}"
            }

            if (stageLower.isNotBlank() && org.stageOrFocus.lowercase().contains(stageLower)) {
                score += 2
                reasons += "Stage/focus fit: ${org.stageOrFocus}"
            }

            if (locLower.isNotBlank() &&
                (org.location.lowercase().contains(locLower) ||
                    org.geography.orEmpty().lowercase().contains(locLower))
            ) {
                score += 2
                reasons += "Geography match: ${org.location}"
            }

            if (org.publicOpportunities.isNotEmpty()) {
                score += 1
                reasons += "Listed opportunity: ${org.publicOpportunities.first()}"
            }

            if (score <= 0 || reasons.isEmpty()) {
                null
            } else {
                val supporting = buildString {
                    append("${org.organizationType} · ${org.location}")
                    if (org.stageOrFocus.isNotBlank()) append(" · Focus: ${org.stageOrFocus}")
                    if (!org.publicContactChannel.isNullOrBlank()) append("\nChannel: ${org.publicContactChannel}")
                }
                EcosystemMatchResult(
                    targetType = MatchTargetType.ORGANIZATION,
                    organization = org,
                    score = score,
                    matchReasons = reasons,
                    supportingInfo = supporting,
                    sourceAttribution = "${org.sourceName} (${org.sourceUrl})",
                    lastVerifiedAt = org.lastVerifiedAt.ifBlank { "Date unverified" },
                    badgeLabel = "Possible Match"
                )
            }
        }.sortedByDescending { it.score }.take(maxOrgMatches)

        val talentMatches = talents.mapNotNull { talent ->
            val reasons = mutableListOf<String>()
            var score = 0

            val skillOverlap = talent.skills.filter { skill ->
                val skLower = skill.lowercase()
                ideaKeywords.any { kw -> skLower.contains(kw) || kw.contains(skLower) }
            }
            if (skillOverlap.isNotEmpty()) {
                score += skillOverlap.size * 3
                reasons += "Matching skills: ${skillOverlap.take(3).joinToString(", ")}"
            }

            val profileKeywords = extractKeywords(talent.role, talent.headline, talent.skills.joinToString(" "))
            val topicOverlap = ideaKeywords.intersect(profileKeywords).sorted().take(3)
            if (topicOverlap.isNotEmpty()) {
                score += topicOverlap.size * 2
                reasons += "Domain experience: ${topicOverlap.joinToString(", ")}"
            }

            if (locLower.isNotBlank() && talent.location.lowercase().contains(locLower)) {
                score += 2
                reasons += "Location match: ${talent.location}"
            }

            if (talent.roleCategory.equals("Builders", ignoreCase = true) ||
                talent.roleCategory.equals("Investors", ignoreCase = true) ||
                talent.roleCategory.equals("Patent Professionals", ignoreCase = true)
            ) {
                if (score > 0) {
                    reasons += "Complementary role: ${talent.roleCategory}"
                    score += 1
                }
            }

            if (score <= 0 || reasons.isEmpty()) {
                null
            } else {
                EcosystemMatchResult(
                    targetType = MatchTargetType.TALENT,
                    talent = talent,
                    score = score,
                    matchReasons = reasons,
                    supportingInfo = "${talent.role} (${talent.roleCategory}) · ${talent.location}\n${talent.headline}",
                    sourceAttribution = if (talent.verified) "Verified app talent profile" else "App talent profile",
                    lastVerifiedAt = null,
                    badgeLabel = "Possible Match"
                )
            }
        }.sortedByDescending { it.score }.take(maxTalentMatches)

        return (orgMatches + talentMatches).sortedByDescending { it.score }
    }

    fun buildControlledShareSummary(
        ideaTitle: String,
        ideaCategory: String,
        ideaStage: String,
        ideaDescription: String,
        scope: ShareIdeaScope,
        customProblemSummary: String = ""
    ): String? {
        return when (scope) {
            ShareIdeaScope.NONE -> null
            ShareIdeaScope.TITLE_SECTOR_ONLY ->
                "Shared concept: \"$ideaTitle\" ($ideaCategory · $ideaStage)"
            ShareIdeaScope.PROBLEM_SUMMARY -> {
                val summary = customProblemSummary.trim().ifBlank {
                    ideaDescription.trim().take(160).let {
                        if (ideaDescription.length > 160) "$it…" else it
                    }
                }
                "Shared problem summary for \"$ideaTitle\" ($ideaCategory): $summary"
            }
            ShareIdeaScope.FULL_PITCH ->
                "Shared full concept \"$ideaTitle\" ($ideaCategory · $ideaStage): ${ideaDescription.trim()}"
        }
    }
}
