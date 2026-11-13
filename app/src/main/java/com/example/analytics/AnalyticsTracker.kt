package com.example.analytics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Central abstraction for Firebase Analytics feature-usage tracking across
 * Marketplace, AI Copilot, and Onboarding flows.
 */
interface AnalyticsTracker {

    fun logEvent(name: String, params: Map<String, Any> = emptyMap())

    fun logScreenView(screenName: String, screenClass: String? = null) {
        val params = buildMap<String, Any> {
            put(FirebaseAnalytics.Param.SCREEN_NAME, sanitizeParamValue(screenName))
            if (!screenClass.isNullOrBlank()) {
                put(FirebaseAnalytics.Param.SCREEN_CLASS, sanitizeParamValue(screenClass))
            }
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }

    // --- Onboarding Flow ---

    fun logOnboardingStarted(source: String) {
        logEvent(
            AnalyticsEvents.ONBOARDING_STARTED,
            mapOf("source" to sanitizeParamValue(source))
        )
    }

    fun logOnboardingStepViewed(stepNumber: Int, stepName: String) {
        logEvent(
            AnalyticsEvents.ONBOARDING_STEP_VIEWED,
            mapOf(
                "step_number" to stepNumber,
                "step_name" to sanitizeParamValue(stepName)
            )
        )
    }

    fun logOnboardingStepNavigated(action: String, fromStep: Int, toStep: Int) {
        logEvent(
            AnalyticsEvents.ONBOARDING_STEP_NAVIGATED,
            mapOf(
                "action" to sanitizeParamValue(action),
                "from_step" to fromStep,
                "to_step" to toStep
            )
        )
    }

    fun logOnboardingCompleted(finalStep: Int, stepName: String, skipped: Boolean) {
        val eventName = if (skipped) {
            AnalyticsEvents.ONBOARDING_SKIPPED
        } else {
            AnalyticsEvents.ONBOARDING_COMPLETED
        }
        logEvent(
            eventName,
            mapOf(
                "final_step" to finalStep,
                "step_name" to sanitizeParamValue(stepName),
                "skipped" to skipped
            )
        )
    }

    // --- Marketplace ---

    fun logMarketplaceSearch(queryPreview: String, queryLength: Int) {
        logEvent(
            FirebaseAnalytics.Event.SEARCH,
            mapOf(
                FirebaseAnalytics.Param.SEARCH_TERM to sanitizeParamValue(queryPreview),
                "query_length" to queryLength
            )
        )
    }

    fun logMarketplaceCategorySelected(category: String, source: String) {
        logEvent(
            AnalyticsEvents.MARKETPLACE_CATEGORY_SELECTED,
            mapOf(
                "category" to sanitizeParamValue(category),
                "source" to sanitizeParamValue(source)
            )
        )
    }

    fun logMarketplaceStageSelected(stage: String) {
        logEvent(
            AnalyticsEvents.MARKETPLACE_STAGE_SELECTED,
            mapOf("stage" to sanitizeParamValue(stage))
        )
    }

    fun logMarketplaceSortSelected(sortBy: String) {
        logEvent(
            AnalyticsEvents.MARKETPLACE_SORT_SELECTED,
            mapOf("sort_by" to sanitizeParamValue(sortBy))
        )
    }

    fun logMarketplaceFiltersCleared() {
        logEvent(AnalyticsEvents.MARKETPLACE_FILTERS_CLEARED)
    }

    fun logMarketplaceRefreshed(source: String) {
        logEvent(
            AnalyticsEvents.MARKETPLACE_REFRESHED,
            mapOf("source" to sanitizeParamValue(source))
        )
    }

    fun logInnovationSelected(
        innovationId: String,
        title: String,
        category: String,
        stage: String,
        source: String
    ) {
        logEvent(
            FirebaseAnalytics.Event.SELECT_ITEM,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_NAME to sanitizeParamValue(title),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category),
                "stage" to sanitizeParamValue(stage),
                "source" to sanitizeParamValue(source)
            )
        )
    }

    fun logInnovationLiked(innovationId: String, category: String, liked: Boolean) {
        logEvent(
            AnalyticsEvents.INNOVATION_LIKE_TOGGLED,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category),
                "liked" to liked
            )
        )
    }

    fun logInnovationBookmarked(innovationId: String, category: String, bookmarked: Boolean) {
        logEvent(
            AnalyticsEvents.INNOVATION_BOOKMARK_TOGGLED,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category),
                "bookmarked" to bookmarked
            )
        )
    }

    fun logInnovationShared(
        innovationId: String,
        title: String,
        category: String,
        source: String
    ) {
        logEvent(
            FirebaseAnalytics.Event.SHARE,
            mapOf(
                FirebaseAnalytics.Param.CONTENT_TYPE to "innovation",
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_NAME to sanitizeParamValue(title),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category),
                FirebaseAnalytics.Param.METHOD to "android_share_sheet",
                "source" to sanitizeParamValue(source)
            )
        )
    }

    fun logProposalSubmitted(innovationId: String, proposalType: String) {
        logEvent(
            AnalyticsEvents.PROPOSAL_SUBMITTED,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                "proposal_type" to sanitizeParamValue(proposalType)
            )
        )
    }

    fun logDigitalNdaSigned(innovationId: String, category: String) {
        logEvent(
            AnalyticsEvents.DIGITAL_NDA_SIGNED,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category)
            )
        )
    }

    // --- AI Copilot ---

    fun logCopilotPromptSent(
        promptSource: String,
        modelTier: String,
        hasAttachedImage: Boolean,
        hasInnovationContext: Boolean,
        innovationCategory: String?,
        promptLength: Int,
        surface: String
    ) {
        val params = buildMap<String, Any> {
            put("prompt_source", sanitizeParamValue(promptSource))
            put("model_tier", sanitizeParamValue(modelTier))
            put("has_image", hasAttachedImage)
            put("has_context", hasInnovationContext)
            put("prompt_length", promptLength)
            put("surface", sanitizeParamValue(surface))
            if (!innovationCategory.isNullOrBlank()) {
                put("innovation_category", sanitizeParamValue(innovationCategory))
            }
        }
        logEvent(AnalyticsEvents.COPILOT_PROMPT_SENT, params)
    }

    fun logCopilotResponseCompleted(modelTier: String, success: Boolean, responseLength: Int) {
        logEvent(
            AnalyticsEvents.COPILOT_RESPONSE_COMPLETED,
            mapOf(
                "model_tier" to sanitizeParamValue(modelTier),
                "success" to success,
                "response_length" to responseLength
            )
        )
    }

    fun logCopilotQuickActionTapped(actionLabel: String, surface: String) {
        logEvent(
            AnalyticsEvents.COPILOT_QUICK_ACTION_TAPPED,
            mapOf(
                "action_label" to sanitizeParamValue(actionLabel),
                "surface" to sanitizeParamValue(surface)
            )
        )
    }

    fun logCopilotModelToggled(useFastModel: Boolean, surface: String) {
        logEvent(
            AnalyticsEvents.COPILOT_MODEL_TOGGLED,
            mapOf(
                "model_tier" to if (useFastModel) "fast_flash_lite" else "deep_flash",
                "surface" to sanitizeParamValue(surface)
            )
        )
    }

    fun logCopilotContextSelected(innovationId: String?, innovationCategory: String?) {
        val params = buildMap<String, Any> {
            put("has_context", !innovationId.isNullOrBlank())
            if (!innovationId.isNullOrBlank()) {
                put(FirebaseAnalytics.Param.ITEM_ID, sanitizeParamValue(innovationId))
            }
            if (!innovationCategory.isNullOrBlank()) {
                put(FirebaseAnalytics.Param.ITEM_CATEGORY, sanitizeParamValue(innovationCategory))
            }
        }
        logEvent(AnalyticsEvents.COPILOT_CONTEXT_SELECTED, params)
    }

    fun logCopilotImageAttached() {
        logEvent(AnalyticsEvents.COPILOT_IMAGE_ATTACHED)
    }

    fun logCopilotReplyCopied() {
        logEvent(AnalyticsEvents.COPILOT_REPLY_COPIED)
    }

    fun logCopilotChatReset() {
        logEvent(AnalyticsEvents.COPILOT_CHAT_RESET)
    }

    fun logCopilotPopupToggled(expanded: Boolean) {
        logEvent(
            AnalyticsEvents.COPILOT_POPUP_TOGGLED,
            mapOf("expanded" to expanded)
        )
    }

    fun logCopilotExpandedFromPopup() {
        logEvent(AnalyticsEvents.COPILOT_EXPANDED_FROM_POPUP)
    }

    fun logCopilotOpenedFromDetail(innovationId: String, category: String) {
        logEvent(
            AnalyticsEvents.COPILOT_OPENED_FROM_DETAIL,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category)
            )
        )
    }

    fun logCopilotPitchDeckGenerated(innovationId: String, category: String, success: Boolean) {
        logEvent(
            AnalyticsEvents.COPILOT_PITCH_DECK_GENERATED,
            mapOf(
                FirebaseAnalytics.Param.ITEM_ID to sanitizeParamValue(innovationId),
                FirebaseAnalytics.Param.ITEM_CATEGORY to sanitizeParamValue(category),
                "success" to success
            )
        )
    }

    fun logCopilotApiKeyUpdated(hasCustomKey: Boolean) {
        logEvent(
            AnalyticsEvents.COPILOT_API_KEY_UPDATED,
            mapOf("has_custom_key" to hasCustomKey)
        )
    }

    companion object {
        const val MAX_EVENT_NAME_LENGTH = 40
        const val MAX_PARAM_NAME_LENGTH = 40
        const val MAX_PARAM_VALUE_LENGTH = 100

        fun sanitizeIdentifier(raw: String, maxLen: Int = MAX_EVENT_NAME_LENGTH): String {
            val cleaned = raw.trim().replace(Regex("[^A-Za-z0-9_]"), "_")
            val prefixed = if (cleaned.isNotEmpty() && !cleaned[0].isLetter()) "e_$cleaned" else cleaned
            return prefixed.take(maxLen).ifBlank { "unknown_event" }
        }

        fun sanitizeParamValue(raw: String): String = raw.trim().take(MAX_PARAM_VALUE_LENGTH)

        fun mapToBundle(params: Map<String, Any>): Bundle = Bundle().apply {
            params.forEach { (rawKey, value) ->
                val key = sanitizeIdentifier(rawKey, MAX_PARAM_NAME_LENGTH)
                when (value) {
                    is String -> putString(key, sanitizeParamValue(value))
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    is Boolean -> putString(key, value.toString())
                    else -> putString(key, sanitizeParamValue(value.toString()))
                }
            }
        }
    }
}

object AnalyticsEvents {
    const val ONBOARDING_STARTED = "onboarding_started"
    const val ONBOARDING_STEP_VIEWED = "onboarding_step_viewed"
    const val ONBOARDING_STEP_NAVIGATED = "onboarding_step_navigated"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"

    const val MARKETPLACE_CATEGORY_SELECTED = "marketplace_category_selected"
    const val MARKETPLACE_STAGE_SELECTED = "marketplace_stage_selected"
    const val MARKETPLACE_SORT_SELECTED = "marketplace_sort_selected"
    const val MARKETPLACE_FILTERS_CLEARED = "marketplace_filters_cleared"
    const val MARKETPLACE_REFRESHED = "marketplace_refreshed"
    const val INNOVATION_LIKE_TOGGLED = "innovation_like_toggled"
    const val INNOVATION_BOOKMARK_TOGGLED = "innovation_bookmark_toggled"
    const val PROPOSAL_SUBMITTED = "proposal_submitted"
    const val DIGITAL_NDA_SIGNED = "digital_nda_signed"

    const val COPILOT_PROMPT_SENT = "copilot_prompt_sent"
    const val COPILOT_RESPONSE_COMPLETED = "copilot_response_completed"
    const val COPILOT_QUICK_ACTION_TAPPED = "copilot_quick_action_tapped"
    const val COPILOT_MODEL_TOGGLED = "copilot_model_toggled"
    const val COPILOT_CONTEXT_SELECTED = "copilot_context_selected"
    const val COPILOT_IMAGE_ATTACHED = "copilot_image_attached"
    const val COPILOT_REPLY_COPIED = "copilot_reply_copied"
    const val COPILOT_CHAT_RESET = "copilot_chat_reset"
    const val COPILOT_POPUP_TOGGLED = "copilot_popup_toggled"
    const val COPILOT_EXPANDED_FROM_POPUP = "copilot_expanded_from_popup"
    const val COPILOT_OPENED_FROM_DETAIL = "copilot_opened_from_detail"
    const val COPILOT_PITCH_DECK_GENERATED = "copilot_pitch_deck_generated"
    const val COPILOT_API_KEY_UPDATED = "copilot_api_key_updated"
}

class FirebaseAnalyticsTracker(
    context: Context
) : AnalyticsTracker {

    private val appContext = context.applicationContext
    private val isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        runCatching { FirebaseAnalytics.getInstance(appContext) }
            .onFailure { Log.w(TAG, "FirebaseAnalytics unavailable in current environment: ${it.message}") }
            .getOrNull()
    }

    override fun logEvent(name: String, params: Map<String, Any>) {
        val safeName = AnalyticsTracker.sanitizeIdentifier(name)
        val bundle = AnalyticsTracker.mapToBundle(params)
        if (isDebuggable) {
            Log.d(TAG, "event=$safeName params=$params")
        }
        runCatching {
            firebaseAnalytics?.logEvent(safeName, bundle)
        }.onFailure {
            Log.w(TAG, "Failed logging analytics event '$safeName': ${it.message}")
        }
    }

    private companion object {
        const val TAG = "Idea2ImpactAnalytics"
    }
}

object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(name: String, params: Map<String, Any>) = Unit
}

val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> { NoOpAnalyticsTracker }
