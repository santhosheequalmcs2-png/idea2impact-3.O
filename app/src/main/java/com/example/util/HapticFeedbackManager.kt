package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

enum class HapticPattern {
    LIGHT_TAP,
    TOGGLE_ON,
    TOGGLE_OFF,
    HEART_LIKE,
    SUCCESS,
    REJECT,
    DRAG_START
}

@Immutable
data class HapticDispatchSpec(
    val viewConstant: Int,
    val composeFallback: HapticFeedbackType
)

fun interface HapticPerformer {
    fun perform(pattern: HapticPattern)
}

/**
 * Centralized tactile feedback controller for IDEA2IMPACT.
 * Uses Android's native [HapticFeedbackConstants] waveforms (no extra permissions
 * required; honors system Touch Feedback preferences) with Compose [HapticFeedback] fallback.
 */
@Stable
class HapticFeedbackManager(
    private val performer: HapticPerformer,
    private val enabledProvider: () -> Boolean = { true }
) {
    fun perform(pattern: HapticPattern) {
        if (!enabledProvider()) return
        runCatching { performer.perform(pattern) }
    }

    fun tap() = perform(HapticPattern.LIGHT_TAP)

    fun toggle(checked: Boolean) {
        perform(if (checked) HapticPattern.TOGGLE_ON else HapticPattern.TOGGLE_OFF)
    }

    fun heartLike(willBeLiked: Boolean) {
        perform(if (willBeLiked) HapticPattern.HEART_LIKE else HapticPattern.LIGHT_TAP)
    }

    fun success() = perform(HapticPattern.SUCCESS)

    fun reject() = perform(HapticPattern.REJECT)

    fun dragStart() = perform(HapticPattern.DRAG_START)

    companion object {
        val NoOp = HapticFeedbackManager(performer = HapticPerformer { })

        fun resolveDispatchSpec(
            pattern: HapticPattern,
            sdkInt: Int = Build.VERSION.SDK_INT
        ): HapticDispatchSpec = when (pattern) {
            HapticPattern.LIGHT_TAP -> HapticDispatchSpec(
                viewConstant = HapticFeedbackConstants.CLOCK_TICK,
                composeFallback = HapticFeedbackType.TextHandleMove
            )
            HapticPattern.TOGGLE_ON -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_ON
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                },
                composeFallback = HapticFeedbackType.TextHandleMove
            )
            HapticPattern.TOGGLE_OFF -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    HapticFeedbackConstants.TOGGLE_OFF
                } else {
                    HapticFeedbackConstants.CLOCK_TICK
                },
                composeFallback = HapticFeedbackType.TextHandleMove
            )
            HapticPattern.HEART_LIKE -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                },
                composeFallback = HapticFeedbackType.LongPress
            )
            HapticPattern.SUCCESS -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.CONTEXT_CLICK
                },
                composeFallback = HapticFeedbackType.LongPress
            )
            HapticPattern.REJECT -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                },
                composeFallback = HapticFeedbackType.LongPress
            )
            HapticPattern.DRAG_START -> HapticDispatchSpec(
                viewConstant = if (sdkInt >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.GESTURE_START
                } else {
                    HapticFeedbackConstants.KEYBOARD_TAP
                },
                composeFallback = HapticFeedbackType.TextHandleMove
            )
        }
    }
}

private class AndroidViewHapticPerformer(
    private val view: View,
    private val composeHaptic: HapticFeedback,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) : HapticPerformer {
    override fun perform(pattern: HapticPattern) {
        val spec = HapticFeedbackManager.resolveDispatchSpec(pattern, sdkInt)
        val performed = view.performHapticFeedback(spec.viewConstant)
        if (!performed) {
            composeHaptic.performHapticFeedback(spec.composeFallback)
        }
    }
}

val LocalHapticFeedbackManager = staticCompositionLocalOf<HapticFeedbackManager> {
    HapticFeedbackManager.NoOp
}

@Composable
fun rememberHapticFeedbackManager(enabled: Boolean = true): HapticFeedbackManager {
    val view = LocalView.current
    val composeHaptic = LocalHapticFeedback.current
    val currentEnabled = rememberUpdatedState(enabled)
    return remember(view, composeHaptic) {
        HapticFeedbackManager(
            performer = AndroidViewHapticPerformer(view, composeHaptic),
            enabledProvider = { currentEnabled.value }
        )
    }
}
