package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
private fun rememberShimmerBrush(widthPx: Int): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
    val safeWidth = widthPx.coerceAtLeast(600).toFloat()
    val progress by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )
    val startX = progress * safeWidth
    val bandWidth = (safeWidth * 0.48f).coerceAtLeast(220f)
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    val midHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val peakHighlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    return Brush.linearGradient(
        colors = listOf(base, midHighlight, peakHighlight, midHighlight, base),
        start = Offset(startX - bandWidth, 0f),
        end = Offset(startX, bandWidth * 0.65f)
    )
}

@Composable
fun InnovationCardSkeleton(
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val brush = rememberShimmerBrush(widthPx)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
    ) {
        Column {
            // Thumbnail + floating badge skeletons matching BackendInnovationCard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .background(brush)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(84.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    )
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(22.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.74f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )

                // Funding Goal & Valuation skeleton strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brush)
                    )
                }

                // Creator avatar + action icons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .width(105.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(brush)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketplaceSkeletonList(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("marketplace_skeleton_loader")) {
        val isWide = maxWidth >= 700.dp
        if (isWide) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                items(4) {
                    InnovationCardSkeleton()
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) {
                items(3) {
                    InnovationCardSkeleton()
                }
            }
        }
    }
}

@Composable
fun TalentCardSkeleton(
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val brush = rememberShimmerBrush(widthPx)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.54f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.76f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(brush)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .width(156.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(brush)
                )
            }
        }
    }
}

@Composable
fun TalentSkeletonList(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("talent_skeleton_loader"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(4) {
            TalentCardSkeleton()
        }
    }
}
