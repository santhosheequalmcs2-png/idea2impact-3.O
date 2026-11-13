package com.example.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val SharedElementBoundsTransform = BoundsTransform { _: Rect, _: Rect ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedInnovationElement(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || animatedScope == null) return this
    return with(sharedScope) {
        this@sharedInnovationElement.sharedElement(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = SharedElementBoundsTransform
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedInnovationBounds(key: String): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animatedScope = LocalNavAnimatedVisibilityScope.current
    if (sharedScope == null || animatedScope == null) return this
    return with(sharedScope) {
        this@sharedInnovationBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedScope,
            boundsTransform = SharedElementBoundsTransform
        )
    }
}
