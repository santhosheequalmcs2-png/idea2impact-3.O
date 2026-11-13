package com.example.analytics

import com.google.common.truth.Truth.assertThat
import com.google.firebase.analytics.FirebaseAnalytics
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalyticsTrackerTest {

    private data class RecordedEvent(
        val name: String,
        val params: Map<String, Any>
    )

    private class RecordingAnalyticsTracker : AnalyticsTracker {
        val events = mutableListOf<RecordedEvent>()

        override fun logEvent(name: String, params: Map<String, Any>) {
            events += RecordedEvent(name = name, params = params)
        }
    }

    @Test
    fun onboardingFlow_logsExpectedStepAndCompletionEvents() {
        val tracker = RecordingAnalyticsTracker()

        tracker.logOnboardingStarted("first_launch")
        tracker.logOnboardingStepViewed(stepNumber = 1, stepName = "submit")
        tracker.logOnboardingStepNavigated(action = "next", fromStep = 1, toStep = 2)
        tracker.logOnboardingStepViewed(stepNumber = 2, stepName = "match")
        tracker.logOnboardingCompleted(finalStep = 4, stepName = "capital", skipped = false)
        tracker.logOnboardingCompleted(finalStep = 2, stepName = "match", skipped = true)

        assertThat(tracker.events.map { it.name }).containsExactly(
            AnalyticsEvents.ONBOARDING_STARTED,
            AnalyticsEvents.ONBOARDING_STEP_VIEWED,
            AnalyticsEvents.ONBOARDING_STEP_NAVIGATED,
            AnalyticsEvents.ONBOARDING_STEP_VIEWED,
            AnalyticsEvents.ONBOARDING_COMPLETED,
            AnalyticsEvents.ONBOARDING_SKIPPED
        ).inOrder()

        assertThat(tracker.events[0].params["source"]).isEqualTo("first_launch")
        assertThat(tracker.events[2].params).containsExactly(
            "action", "next",
            "from_step", 1,
            "to_step", 2
        )
        assertThat(tracker.events[4].params["skipped"]).isEqualTo(false)
        assertThat(tracker.events[5].params["skipped"]).isEqualTo(true)
    }

    @Test
    fun marketplaceEvents_useStandardAndCustomSchemasCleanly() {
        val tracker = RecordingAnalyticsTracker()

        tracker.logMarketplaceCategorySelected(category = "AI & Robotics", source = "filter_chip")
        tracker.logMarketplaceStageSelected(stage = "Seed")
        tracker.logMarketplaceSortSelected(sortBy = "Top Voted")
        tracker.logMarketplaceRefreshed(source = "pull_to_refresh")
        tracker.logMarketplaceSearch(queryPreview = "quantum battery", queryLength = 15)
        tracker.logInnovationSelected(
            innovationId = "inno-42",
            title = "Solid-State Quantum Cell",
            category = "CleanTech",
            stage = "Series A",
            source = "marketplace"
        )
        tracker.logInnovationShared(
            innovationId = "inno-42",
            title = "Solid-State Quantum Cell",
            category = "CleanTech",
            source = "marketplace_card"
        )

        assertThat(tracker.events[0].name).isEqualTo(AnalyticsEvents.MARKETPLACE_CATEGORY_SELECTED)
        assertThat(tracker.events[0].params["source"]).isEqualTo("filter_chip")
        assertThat(tracker.events[3].params["source"]).isEqualTo("pull_to_refresh")

        assertThat(tracker.events[4].name).isEqualTo(FirebaseAnalytics.Event.SEARCH)
        assertThat(tracker.events[4].params[FirebaseAnalytics.Param.SEARCH_TERM]).isEqualTo("quantum battery")

        assertThat(tracker.events[5].name).isEqualTo(FirebaseAnalytics.Event.SELECT_ITEM)
        assertThat(tracker.events[5].params[FirebaseAnalytics.Param.ITEM_ID]).isEqualTo("inno-42")

        assertThat(tracker.events[6].name).isEqualTo(FirebaseAnalytics.Event.SHARE)
        assertThat(tracker.events[6].params[FirebaseAnalytics.Param.METHOD]).isEqualTo("android_share_sheet")
    }

    @Test
    fun copilotEvents_captureSurfaceModelContextAndActions() {
        val tracker = RecordingAnalyticsTracker()

        tracker.logCopilotPopupToggled(expanded = true)
        tracker.logCopilotModelToggled(useFastModel = true, surface = "floating_popup")
        tracker.logCopilotContextSelected(innovationId = "inno-7", innovationCategory = "BioTech")
        tracker.logCopilotQuickActionTapped(actionLabel = "Summarize", surface = "floating_popup")
        tracker.logCopilotPromptSent(
            promptSource = "popup_quick_action",
            modelTier = "fast_flash_lite",
            hasAttachedImage = false,
            hasInnovationContext = true,
            innovationCategory = "BioTech",
            promptLength = 72,
            surface = "floating_popup"
        )
        tracker.logCopilotResponseCompleted(
            modelTier = "fast_flash_lite",
            success = true,
            responseLength = 640
        )
        tracker.logCopilotImageAttached()
        tracker.logCopilotReplyCopied()
        tracker.logCopilotPitchDeckGenerated(innovationId = "inno-7", category = "BioTech", success = true)

        assertThat(tracker.events.map { it.name }).containsExactly(
            AnalyticsEvents.COPILOT_POPUP_TOGGLED,
            AnalyticsEvents.COPILOT_MODEL_TOGGLED,
            AnalyticsEvents.COPILOT_CONTEXT_SELECTED,
            AnalyticsEvents.COPILOT_QUICK_ACTION_TAPPED,
            AnalyticsEvents.COPILOT_PROMPT_SENT,
            AnalyticsEvents.COPILOT_RESPONSE_COMPLETED,
            AnalyticsEvents.COPILOT_IMAGE_ATTACHED,
            AnalyticsEvents.COPILOT_REPLY_COPIED,
            AnalyticsEvents.COPILOT_PITCH_DECK_GENERATED
        ).inOrder()

        assertThat(tracker.events[1].params["model_tier"]).isEqualTo("fast_flash_lite")
        assertThat(tracker.events[4].params["surface"]).isEqualTo("floating_popup")
        assertThat(tracker.events[4].params["has_context"]).isEqualTo(true)
        assertThat(tracker.events[5].params["response_length"]).isEqualTo(640)
    }

    @Test
    fun bundleConversion_sanitizesLongStringsAndPreservesTypedPrimitives() {
        val longValue = "x".repeat(150)
        val bundle = AnalyticsTracker.mapToBundle(
            mapOf(
                "item_name" to "  $longValue  ",
                "count" to 7,
                "score" to 98.5,
                "enabled" to true
            )
        )

        assertThat(bundle.getString("item_name")).hasLength(AnalyticsTracker.MAX_PARAM_VALUE_LENGTH)
        assertThat(bundle.getLong("count")).isEqualTo(7L)
        assertThat(bundle.getDouble("score")).isEqualTo(98.5)
        assertThat(bundle.getString("enabled")).isEqualTo("true")

        val sanitizedName = AnalyticsTracker.sanitizeIdentifier("123-event.with spaces!_and_extra_length_beyond_forty")
        assertThat(sanitizedName.length).isAtMost(AnalyticsTracker.MAX_EVENT_NAME_LENGTH)
        assertThat(sanitizedName.first().isLetter()).isTrue()
        assertThat(sanitizedName).doesNotContain(" ")
        assertThat(sanitizedName).doesNotContain("-")
    }
}
