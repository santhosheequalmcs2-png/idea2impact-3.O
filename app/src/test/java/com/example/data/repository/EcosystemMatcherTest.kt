package com.example.data.repository

import com.example.data.model.BackendTalent
import com.example.data.model.MatchTargetType
import com.example.data.model.ShareIdeaScope
import com.example.data.model.VerifiedDirectoryJsonParser
import com.example.data.model.VerifiedOrganization
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class EcosystemMatcherTest {

    @Test
    fun jsonParser_validatesRequiredFieldsAndRejectsMissingSourceUrlOrDate() {
        val validJson = """
            [
              {
                "id": "synthetic_incubator_1",
                "name": "Synthetic Agri Incubator",
                "organizationType": "Incubator",
                "sector": "AgriTech",
                "description": "Synthetic test record for rural cold-chain hardware.",
                "location": "Pune, Maharashtra, India",
                "stageOrFocus": "Prototype",
                "publicOpportunities": ["Test Hardware Grant"],
                "eligibility": "Registered early-stage hardware teams",
                "geography": "India",
                "deadline": "2026-12-15",
                "sourceName": "Synthetic Program Registry",
                "sourceUrl": "https://example.org/registry",
                "verificationStatus": "VERIFIED",
                "lastVerifiedAt": "2026-11-10"
              }
            ]
        """.trimIndent()

        val parsed = VerifiedDirectoryJsonParser.parse(validJson)
        assertThat(parsed.isSuccess).isTrue()
        val org = parsed.getOrThrow().single()
        assertThat(org.name).isEqualTo("Synthetic Agri Incubator")
        assertThat(org.isFundingRelevant()).isTrue()
        assertThat(org.isVerificationMissingOrStale(LocalDate.of(2026, 11, 16))).isFalse()

        val missingSourceJson = """
            [
              {
                "name": "Missing Source Org",
                "organizationType": "Startup",
                "sector": "AI & Robotics",
                "sourceName": "Portal",
                "lastVerifiedAt": "2026-11-10"
              }
            ]
        """.trimIndent()
        assertThat(VerifiedDirectoryJsonParser.parse(missingSourceJson).isFailure).isTrue()

        val badDateJson = """
            [
              {
                "name": "Bad Date Org",
                "organizationType": "Investor",
                "sector": "Green Tech",
                "sourceName": "Portal",
                "sourceUrl": "https://example.org",
                "lastVerifiedAt": "not-a-date"
              }
            ]
        """.trimIndent()
        assertThat(VerifiedDirectoryJsonParser.parse(badDateJson).isFailure).isTrue()
    }

    @Test
    fun isVerificationMissingOrStale_detectsStaleOrUnverifiedDates() {
        val staleOrg = VerifiedOrganization(
            id = "stale_1",
            name = "Old Verified Fund",
            organizationType = "Investor",
            sector = "Green Tech",
            sourceName = "Official Site",
            sourceUrl = "https://example.org/fund",
            verificationStatus = "VERIFIED",
            lastVerifiedAt = "2026-01-01"
        )
        assertThat(staleOrg.isVerificationMissingOrStale(LocalDate.of(2026, 11, 16))).isTrue()

        val freshNeedsReview = staleOrg.copy(
            verificationStatus = "NEEDS_REVIEW",
            lastVerifiedAt = "2026-11-15"
        )
        assertThat(freshNeedsReview.isVerificationMissingOrStale(LocalDate.of(2026, 11, 16))).isTrue()
    }

    @Test
    fun matchIdeaToEcosystem_returnsExplainablePossibleMatchesAndExcludesUnrelated() {
        val agriOrg = VerifiedOrganization(
            id = "org_agri",
            name = "Synthetic Agri Hardware Hub",
            organizationType = "Incubator",
            sector = "Green Tech & AgriTech",
            description = "Supports solar cold-storage and sensor prototypes in rural India.",
            location = "Bengaluru, Karnataka, India",
            stageOrFocus = "Prototype",
            publicOpportunities = listOf("Prototype Sandbox Cohort"),
            sourceName = "Synthetic Registry",
            sourceUrl = "https://example.org/agri",
            verificationStatus = "VERIFIED",
            lastVerifiedAt = "2026-11-12"
        )
        val unrelatedOrg = VerifiedOrganization(
            id = "org_gaming",
            name = "Synthetic Indie Esports Studio",
            organizationType = "Company",
            sector = "Gaming & Media",
            description = "Develops multiplayer console titles.",
            location = "Mumbai, India",
            stageOrFocus = "Series B",
            sourceName = "Synthetic Registry",
            sourceUrl = "https://example.org/gaming",
            verificationStatus = "VERIFIED",
            lastVerifiedAt = "2026-11-12"
        )
        val matchingTalent = BackendTalent(
            id = "tal_solar",
            name = "Arjun Rao",
            role = "Embedded Solar Engineer",
            roleCategory = "Builders",
            headline = "Cold-storage telemetry & firmware specialist",
            skills = listOf("Solar", "IoT", "Cold-Storage"),
            location = "Bengaluru, India",
            avatarUrl = null,
            verified = true
        )

        val results = EcosystemMatcher.matchIdeaToEcosystem(
            ideaTitle = "Solar Cold-Storage Sensor Crate",
            ideaDescription = "Low-cost IoT cold-storage monitoring for rural farmers.",
            ideaCategory = "Green Tech",
            ideaStage = "Prototype",
            preferredLocation = "Bengaluru",
            organizations = listOf(agriOrg, unrelatedOrg),
            talents = listOf(matchingTalent)
        )

        assertThat(results.mapNotNull { it.organization?.id }).containsExactly("org_agri")
        val orgResult = results.first { it.targetType == MatchTargetType.ORGANIZATION }
        assertThat(orgResult.badgeLabel).isEqualTo("Possible Match")
        assertThat(orgResult.matchReasons.joinToString(" ")).contains("Sector match")
        assertThat(orgResult.sourceAttribution).contains("https://example.org/agri")
        assertThat(orgResult.lastVerifiedAt).isEqualTo("2026-11-12")

        val talentResult = results.first { it.targetType == MatchTargetType.TALENT }
        assertThat(talentResult.talent?.id).isEqualTo("tal_solar")
        assertThat(talentResult.matchReasons.joinToString(" ")).contains("Matching skills")
    }

    @Test
    fun buildControlledShareSummary_neverLeaksPrivatePitchInRestrictedScopes() {
        val secretPitch = "Proprietary 4-stage lithium-ceramic coating formula under stealth R&D."

        assertThat(
            EcosystemMatcher.buildControlledShareSummary(
                ideaTitle = "VoltCoat",
                ideaCategory = "Inventions",
                ideaStage = "Prototype",
                ideaDescription = secretPitch,
                scope = ShareIdeaScope.NONE
            )
        ).isNull()

        val titleOnly = EcosystemMatcher.buildControlledShareSummary(
            ideaTitle = "VoltCoat",
            ideaCategory = "Inventions",
            ideaStage = "Prototype",
            ideaDescription = secretPitch,
            scope = ShareIdeaScope.TITLE_SECTOR_ONLY
        )
        assertThat(titleOnly).contains("VoltCoat")
        assertThat(titleOnly).doesNotContain("lithium-ceramic")

        val customProblem = EcosystemMatcher.buildControlledShareSummary(
            ideaTitle = "VoltCoat",
            ideaCategory = "Inventions",
            ideaStage = "Prototype",
            ideaDescription = secretPitch,
            scope = ShareIdeaScope.PROBLEM_SUMMARY,
            customProblemSummary = "Reducing thermal degradation in battery packs."
        )
        assertThat(customProblem).contains("Reducing thermal degradation")
        assertThat(customProblem).doesNotContain("lithium-ceramic")

        val fullPitch = EcosystemMatcher.buildControlledShareSummary(
            ideaTitle = "VoltCoat",
            ideaCategory = "Inventions",
            ideaStage = "Prototype",
            ideaDescription = secretPitch,
            scope = ShareIdeaScope.FULL_PITCH
        )
        assertThat(fullPitch).contains("lithium-ceramic")
    }
}
