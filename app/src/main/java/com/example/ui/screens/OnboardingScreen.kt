package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.HeroAmbientLottieBackdrop
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleGlow
import com.example.util.StartupSoundPlayer
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class OnboardingStepSpec(
    val numberLabel: String,
    @StringRes val stepShortLabelRes: Int,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val illustrationRes: Int,
    @StringRes val illustrationCdRes: Int,
    @DrawableRes val badgeIconRes: Int,
    val accentColor: Color,
    val secondaryColor: Color,
    val highlights: List<String>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val steps = remember {
        listOf(
            OnboardingStepSpec(
                numberLabel = "01",
                stepShortLabelRes = R.string.onboard_step_1_label,
                titleRes = R.string.onboard_step_1_title,
                descriptionRes = R.string.onboard_step_1_desc,
                illustrationRes = R.drawable.img_onboard_submit,
                illustrationCdRes = R.string.cd_onboard_submit,
                badgeIconRes = R.drawable.ic_nda_shield_badge,
                accentColor = CobaltBlue,
                secondaryColor = PurpleGlow,
                highlights = listOf("SHA-256 Proof", "USPTO Priority Date", "Mutual Digital NDA")
            ),
            OnboardingStepSpec(
                numberLabel = "02",
                stepShortLabelRes = R.string.onboard_step_2_label,
                titleRes = R.string.onboard_step_2_title,
                descriptionRes = R.string.onboard_step_2_desc,
                illustrationRes = R.drawable.img_onboard_match,
                illustrationCdRes = R.string.cd_onboard_match,
                badgeIconRes = R.drawable.ic_brand_spark_badge,
                accentColor = PurpleGlow,
                secondaryColor = ElectricCyan,
                highlights = listOf("Gemini Prior-Art Scan", "TAM / SAM / SOM", "98% Fit Scoring")
            ),
            OnboardingStepSpec(
                numberLabel = "03",
                stepShortLabelRes = R.string.onboard_step_3_label,
                titleRes = R.string.onboard_step_3_title,
                descriptionRes = R.string.onboard_step_3_desc,
                illustrationRes = R.drawable.img_onboard_assemble,
                illustrationCdRes = R.string.cd_onboard_assemble,
                badgeIconRes = R.drawable.ic_builder_network_badge,
                accentColor = CobaltBlue,
                secondaryColor = ElectricCyan,
                highlights = listOf("Robotics & Firmware", "Industrial Design", "Patent Counsel")
            ),
            OnboardingStepSpec(
                numberLabel = "04",
                stepShortLabelRes = R.string.onboard_step_4_label,
                titleRes = R.string.onboard_step_4_title,
                descriptionRes = R.string.onboard_step_4_desc,
                illustrationRes = R.drawable.img_onboard_capital,
                illustrationCdRes = R.string.cd_onboard_capital,
                badgeIconRes = R.drawable.ic_patent_growth_badge,
                accentColor = EmeraldSuccess,
                secondaryColor = CobaltBlue,
                highlights = listOf("Encrypted Deal Rooms", "Pre-Seed Syndicates", "IP Licensing")
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { steps.size })

    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch {
            StartupSoundPlayer.playTap(context)
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            HeroAmbientLottieBackdrop(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.52f }
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header: Brand + Step Pill + Skip Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = stringResource(R.string.cd_brand_mark),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "IDEA2IMPACT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "PROCESS TOUR · ${pagerState.currentPage + 1} OF ${steps.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            StartupSoundPlayer.playTap(context)
                            onFinishOnboarding()
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.onboard_skip),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4-Step Interactive Selector Rail (Submit -> Match -> Assemble -> Capital)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        val isSelected = pagerState.currentPage == idx
                        val isPassed = idx < pagerState.currentPage
                        val chipContainer by animateColorAsState(
                            targetValue = when {
                                isSelected -> step.accentColor.copy(alpha = 0.16f)
                                isPassed -> EmeraldSuccess.copy(alpha = 0.10f)
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                            },
                            label = "step_chip_bg"
                        )
                        val chipBorder by animateColorAsState(
                            targetValue = when {
                                isSelected -> step.accentColor.copy(alpha = 0.75f)
                                isPassed -> EmeraldSuccess.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                            },
                            label = "step_chip_border"
                        )

                        Surface(
                            color = chipContainer,
                            border = BorderStroke(1.dp, chipBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    StartupSoundPlayer.playTap(context)
                                    scope.launch { pagerState.animateScrollToPage(idx) }
                                }
                                .testTag("onboarding_step_chip_${idx + 1}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = step.numberLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) step.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = stringResource(step.stepShortLabelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Swipeable 4-Step HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("onboarding_horizontal_pager")
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                    val clampedOffset = abs(pageOffset).coerceIn(0f, 1f)

                    OnboardingPageContent(
                        step = step,
                        pageOffsetFraction = clampedOffset,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Bottom Pager Dot Indicator + Primary Navigation Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OnboardingPagerDots(
                        pagerState = pagerState,
                        steps = steps,
                        onSelectPage = { target ->
                            StartupSoundPlayer.playTap(context)
                            scope.launch { pagerState.animateScrollToPage(target) }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage > 0) {
                            OutlinedButton(
                                onClick = {
                                    StartupSoundPlayer.playTap(context)
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .weight(0.42f)
                                    .heightIn(min = 52.dp)
                                    .testTag("onboarding_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.onboard_previous),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        val isLastPage = pagerState.currentPage == steps.lastIndex
                        Box(
                            modifier = Modifier
                                .weight(if (pagerState.currentPage > 0) 0.58f else 1f)
                                .heightIn(min = 52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        if (isLastPage) {
                                            listOf(PurpleGlow, CobaltBlue, EmeraldSuccess)
                                        } else {
                                            listOf(PurpleGlow, CobaltBlue)
                                        }
                                    )
                                )
                                .clickable {
                                    if (isLastPage) {
                                        StartupSoundPlayer.playSuccess(context)
                                        onFinishOnboarding()
                                    } else {
                                        StartupSoundPlayer.playTap(context)
                                        scope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                }
                                .testTag(
                                    if (isLastPage) "onboarding_get_started_button" else "onboarding_next_button"
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = if (isLastPage) {
                                        stringResource(R.string.onboard_get_started)
                                    } else {
                                        stringResource(R.string.onboard_next)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = if (isLastPage) {
                                        Icons.Filled.RocketLaunch
                                    } else {
                                        Icons.AutoMirrored.Filled.ArrowForward
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingPageContent(
    step: OnboardingStepSpec,
    pageOffsetFraction: Float,
    modifier: Modifier = Modifier
) {
    val scale = 1f - (pageOffsetFraction * 0.08f)
    val contentAlpha = 1f - (pageOffsetFraction * 0.45f)

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        val isWidePage = maxWidth >= 660.dp

        if (isWidePage) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 980.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingVisualCard(
                    step = step,
                    modifier = Modifier
                        .weight(1.05f)
                        .aspectRatio(4f / 3f)
                )
                OnboardingStepDetails(
                    step = step,
                    modifier = Modifier
                        .weight(0.95f)
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OnboardingVisualCard(
                    step = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .aspectRatio(4f / 3f)
                )
                Spacer(Modifier.height(18.dp))
                OnboardingStepDetails(
                    step = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingVisualCard(
    step: OnboardingStepSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.35f)),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = step.illustrationRes),
                contentDescription = stringResource(step.illustrationCdRes),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Subtle corner step badge matching app card polish
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.45f)),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        painter = painterResource(id = step.badgeIconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "STEP ${step.numberLabel} · ${stringResource(step.stepShortLabelRes).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = step.accentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingStepDetails(
    step: OnboardingStepSpec,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(step.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(step.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            step.highlights.forEach { highlight ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = step.accentColor.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, step.accentColor.copy(alpha = 0.32f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = step.accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = highlight,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPagerDots(
    pagerState: PagerState,
    steps: List<OnboardingStepSpec>,
    onSelectPage: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val selected = pagerState.currentPage == index
            val dotWidth by animateDpAsState(
                targetValue = if (selected) 28.dp else 9.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "pager_dot_width"
            )
            val dotColor by animateColorAsState(
                targetValue = if (selected) step.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                label = "pager_dot_color"
            )

            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .widthIn(min = 24.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onSelectPage(index) }
                    .padding(horizontal = 4.dp)
                    .testTag("onboarding_dot_${index + 1}"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(9.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}
