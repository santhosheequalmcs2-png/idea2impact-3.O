package com.example.ui.components

import android.content.Intent
import com.example.data.local.IdeaPortfolioEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PortfolioShareHelperTest {

    private val linkedIdea = IdeaPortfolioEntity(
        id = "inn-solar-01",
        innovationId = "inn-solar-01",
        title = "Solar Desalination Microgrid",
        summary = "Off-grid brackish water desalination for coastal farm clusters.",
        category = "CleanTech",
        stage = "MVP",
        creatorName = "Ananya Verma",
        fundingGoal = 150000.0,
        valuation = 1200000.0,
        ndaRequired = true,
        priorityTag = "MVP Ready",
        validationNotes = "Piloted with 4 coastal co-ops.",
        savedAt = 1000L,
        updatedAt = 2000L
    )

    private val customIdea = IdeaPortfolioEntity(
        id = "idea-custom-02",
        innovationId = null,
        title = "Cold-Chain Telemetry Node",
        summary = "LoRaWAN spoilage alert sensors for rural dairy hubs.",
        category = "AgriTech",
        stage = "Concept",
        creatorName = "You",
        fundingGoal = 0.0,
        valuation = 0.0,
        ndaRequired = false,
        priorityTag = "Validating",
        validationNotes = "",
        savedAt = 3000L,
        updatedAt = 3000L
    )

    @Test
    fun buildIdeaShareText_includesMetadataNotesAndInnovationLink() {
        val text = buildIdeaShareText(linkedIdea)

        assertThat(text).contains("Solar Desalination Microgrid (CleanTech · MVP)")
        assertThat(text).contains("Priority: MVP Ready")
        assertThat(text).contains("Creator: Ananya Verma")
        assertThat(text).contains("Funding Goal:")
        assertThat(text).contains("Off-grid brackish water desalination for coastal farm clusters.")
        assertThat(text).contains("Validation Notes: Piloted with 4 coastal co-ops.")
        assertThat(text).contains("https://idea2impact-yges.vercel.app/innovations/inn-solar-01")
    }

    @Test
    fun buildIdeaShareText_omitsZeroFinancialsBlankNotesAndSelfCreator() {
        val text = buildIdeaShareText(customIdea)

        assertThat(text).contains("Cold-Chain Telemetry Node (AgriTech · Concept)")
        assertThat(text).contains("Priority: Validating")
        assertThat(text).doesNotContain("Creator: You")
        assertThat(text).doesNotContain("Funding Goal:")
        assertThat(text).doesNotContain("Valuation:")
        assertThat(text).doesNotContain("Validation Notes:")
        assertThat(text).contains("Shared from IDEA2IMPACT Idea Portfolio")
    }

    @Test
    fun buildIdeaPortfolioExportText_formatsEmptySingleAndMultipleIdeas() {
        assertThat(buildIdeaPortfolioExportText(emptyList())).isEmpty()
        assertThat(buildIdeaPortfolioExportText(listOf(linkedIdea)))
            .isEqualTo(buildIdeaShareText(linkedIdea))

        val bundle = buildIdeaPortfolioExportText(listOf(linkedIdea, customIdea))
        assertThat(bundle).contains("IDEA2IMPACT · Idea Portfolio (2 saved ideas)")
        assertThat(bundle).contains("1. Solar Desalination Microgrid (CleanTech · MVP · MVP Ready)")
        assertThat(bundle).contains("2. Cold-Chain Telemetry Node (AgriTech · Concept · Validating)")
        assertThat(bundle).contains("Notes: Piloted with 4 coastal co-ops.")
        assertThat(bundle).contains("Link: https://idea2impact-yges.vercel.app/innovations/inn-solar-01")
    }

    @Test
    fun createShareIntents_configureActionSendAndPlainTextExtras() {
        val singleIntent = createIdeaShareIntent(linkedIdea)
        assertThat(singleIntent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(singleIntent.type).isEqualTo("text/plain")
        assertThat(singleIntent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Solar Desalination Microgrid")
        assertThat(singleIntent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo(buildIdeaShareText(linkedIdea))

        val exportIntent = createIdeaPortfolioExportIntent(listOf(linkedIdea, customIdea))
        assertThat(exportIntent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(exportIntent.type).isEqualTo("text/plain")
        assertThat(exportIntent.getStringExtra(Intent.EXTRA_SUBJECT))
            .isEqualTo("IDEA2IMPACT Idea Portfolio (2 ideas)")
        assertThat(exportIntent.getStringExtra(Intent.EXTRA_TEXT))
            .isEqualTo(buildIdeaPortfolioExportText(listOf(linkedIdea, customIdea)))
    }
}
