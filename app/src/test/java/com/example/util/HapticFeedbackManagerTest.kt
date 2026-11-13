package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HapticFeedbackManagerTest {

    @Test
    fun convenienceMethods_dispatchExpectedSemanticPatterns() {
        val dispatched = mutableListOf<HapticPattern>()
        val manager = HapticFeedbackManager(performer = HapticPerformer { dispatched += it })

        manager.tap()
        manager.toggle(true)
        manager.toggle(false)
        manager.heartLike(true)
        manager.heartLike(false)
        manager.success()
        manager.reject()
        manager.dragStart()

        assertThat(dispatched).containsExactly(
            HapticPattern.LIGHT_TAP,
            HapticPattern.TOGGLE_ON,
            HapticPattern.TOGGLE_OFF,
            HapticPattern.HEART_LIKE,
            HapticPattern.LIGHT_TAP,
            HapticPattern.SUCCESS,
            HapticPattern.REJECT,
            HapticPattern.DRAG_START
        ).inOrder()
    }

    @Test
    fun disabledManager_suppressesAllHapticDispatches() {
        var enabled = false
        val dispatched = mutableListOf<HapticPattern>()
        val manager = HapticFeedbackManager(
            performer = HapticPerformer { dispatched += it },
            enabledProvider = { enabled }
        )

        manager.tap()
        manager.toggle(true)
        manager.heartLike(true)
        manager.success()
        assertThat(dispatched).isEmpty()

        enabled = true
        manager.heartLike(true)
        assertThat(dispatched).containsExactly(HapticPattern.HEART_LIKE)
    }

    @Test
    fun performerExceptions_areSafelySwallowed() {
        val manager = HapticFeedbackManager(
            performer = HapticPerformer { throw IllegalStateException("No vibrator service") }
        )

        manager.tap()
        manager.heartLike(true)
        manager.success()
        manager.reject()
    }

    @Test
    fun resolveDispatchSpec_usesModernConstantsOnApi34Plus_andGracefulFallbacksOnApi26() {
        val toggleOnApi34 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.TOGGLE_ON,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )
        val toggleOffApi34 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.TOGGLE_OFF,
            sdkInt = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        )
        assertThat(toggleOnApi34.viewConstant).isEqualTo(HapticFeedbackConstants.TOGGLE_ON)
        assertThat(toggleOffApi34.viewConstant).isEqualTo(HapticFeedbackConstants.TOGGLE_OFF)

        val heartApi30 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.HEART_LIKE,
            sdkInt = Build.VERSION_CODES.R
        )
        val rejectApi30 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.REJECT,
            sdkInt = Build.VERSION_CODES.R
        )
        assertThat(heartApi30.viewConstant).isEqualTo(HapticFeedbackConstants.CONFIRM)
        assertThat(heartApi30.composeFallback).isEqualTo(HapticFeedbackType.LongPress)
        assertThat(rejectApi30.viewConstant).isEqualTo(HapticFeedbackConstants.REJECT)

        val toggleOnApi26 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.TOGGLE_ON,
            sdkInt = Build.VERSION_CODES.O
        )
        val heartApi26 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.HEART_LIKE,
            sdkInt = Build.VERSION_CODES.O
        )
        val rejectApi26 = HapticFeedbackManager.resolveDispatchSpec(
            pattern = HapticPattern.REJECT,
            sdkInt = Build.VERSION_CODES.O
        )
        assertThat(toggleOnApi26.viewConstant).isEqualTo(HapticFeedbackConstants.CONTEXT_CLICK)
        assertThat(heartApi26.viewConstant).isEqualTo(HapticFeedbackConstants.CONTEXT_CLICK)
        assertThat(rejectApi26.viewConstant).isEqualTo(HapticFeedbackConstants.LONG_PRESS)
    }
}
