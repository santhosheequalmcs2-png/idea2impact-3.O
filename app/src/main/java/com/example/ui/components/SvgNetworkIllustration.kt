package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.R
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleGlow

const val DEFAULT_HERO_SVG_ASSET = "file:///android_asset/svg/global_innovation_network.svg"

@Composable
fun rememberSvgImageLoader(): ImageLoader {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

/**
 * High-quality 60fps looping Lottie animation representing IDEA2IMPACT's
 * Global Innovation Exchange network: rotating meridian tracks, capital-flow
 * beams, cross-continental IP packet arcs, and pulsing global hub beacons.
 */
@Composable
fun GlobalExchangeLottieStage(
    modifier: Modifier = Modifier,
    remoteLottieUrl: String? = null,
    speed: Float = 1f,
    isPlaying: Boolean = true
) {
    val cdText = stringResource(R.string.cd_hero_lottie_exchange)
    var remoteFailed by remember(remoteLottieUrl) { mutableStateOf(false) }

    val spec = remember(remoteLottieUrl, remoteFailed) {
        if (!remoteLottieUrl.isNullOrBlank() && !remoteFailed) {
            LottieCompositionSpec.Url(remoteLottieUrl)
        } else {
            LottieCompositionSpec.RawRes(R.raw.lottie_global_innovation_exchange)
        }
    }

    val compositionResult = rememberLottieComposition(spec)
    val composition = compositionResult.value

    if (!remoteLottieUrl.isNullOrBlank() && !remoteFailed && compositionResult.isFailure) {
        remoteFailed = true
    }

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        speed = speed,
        restartOnPlay = false
    )

    Box(
        modifier = modifier
            .semantics { contentDescription = cdText }
            .testTag("hero_lottie_exchange")
    ) {
        if (composition == null) {
            SvgShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Subtle full-bleed repeating Lottie ambient background for the Hero Section card,
 * providing continuous cross-continental exchange arcs and drifting telemetry nodes
 * behind the hero headline, CTAs, and HeroIllustration.
 */
@Composable
fun HeroAmbientLottieBackdrop(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true
) {
    val compositionResult = rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lottie_hero_exchange_backdrop)
    )
    val composition = compositionResult.value
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = isPlaying,
        iterations = LottieConstants.IterateForever,
        speed = 0.85f,
        restartOnPlay = false
    )

    if (composition != null) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Crop,
            modifier = modifier.testTag("hero_ambient_lottie_backdrop")
        )
    }
}

/**
 * Fetches (via HTTP when [remoteSvgUrl] is provided, or local W3C SVG asset) and renders
 * brand-aligned SVG illustrations via Coil's [SvgDecoder], with shimmer loading state
 * and automatic [fallbackDrawableRes] vector fallback.
 */
@Composable
fun BrandSvgIllustration(
    contentDescription: String,
    modifier: Modifier = Modifier,
    remoteSvgUrl: String? = null,
    localSvgUri: String = DEFAULT_HERO_SVG_ASSET,
    @DrawableRes fallbackDrawableRes: Int = R.drawable.vec_hero_ecosystem_banner,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val svgLoader = rememberSvgImageLoader()

    var remoteFailed by remember(remoteSvgUrl) { mutableStateOf(false) }
    var localSvgFailed by remember(localSvgUri) { mutableStateOf(false) }

    val activeSource: String? = when {
        !remoteSvgUrl.isNullOrBlank() && !remoteFailed -> remoteSvgUrl
        !localSvgFailed -> localSvgUri
        else -> null
    }

    if (activeSource == null) {
        Image(
            painter = painterResource(id = fallbackDrawableRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
        return
    }

    val request = remember(activeSource, context) {
        ImageRequest.Builder(context)
            .data(activeSource)
            .decoderFactory(SvgDecoder.Factory())
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        imageLoader = svgLoader,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            SvgShimmerPlaceholder(modifier = Modifier.fillMaxSize())
        },
        error = {
            if (!remoteSvgUrl.isNullOrBlank() && activeSource == remoteSvgUrl) {
                remoteFailed = true
            } else {
                localSvgFailed = true
            }
            Image(
                painter = painterResource(id = fallbackDrawableRes),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    )
}

/**
 * Website-exact HeroIllustration visual cluster combining:
 * 1) Seamless 60fps Lottie Global Innovation Exchange animation backdrop
 * 2) Interactive 3D Geodesic Exchange Sphere
 * 3) Staggered frosted floating vector Trust Cards (matching web screenshot alignment)
 * 4) Centered "INVENT · PROTECT · FUND · SCALE" brand pill bar
 */
@Composable
fun HeroIllustration(
    modifier: Modifier = Modifier,
    remoteLottieUrl: String? = null
) {
    var activeHubLabel by remember { mutableStateOf("San Francisco · AI & Chips") }
    var exchangeSpeed by remember { mutableStateOf(1f) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("hero_illustration_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        listOf(
                            PurpleGlow.copy(alpha = 0.09f),
                            ElectricCyan.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Layered Lottie Global Innovation Exchange + Interactive 3D Geodesic Exchange Globe + Floating Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(212.dp),
                contentAlignment = Alignment.Center
            ) {
                GlobalExchangeLottieStage(
                    remoteLottieUrl = remoteLottieUrl,
                    speed = exchangeSpeed,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )

                GlobalExchange3DGlobe(
                    onHubSelected = { hub ->
                        activeHubLabel = hub
                        exchangeSpeed = if (exchangeSpeed > 1.1f) 1f else 1.35f
                    },
                    transparentStage = true,
                    showOverlayPills = false,
                    stageHeight = 204.dp,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Right Staggered AI Opportunity Floating Badge
                HeroFloatingBadgeChip(
                    iconRes = R.drawable.ic_brand_spark_badge,
                    iconBg = PurpleGlow.copy(alpha = 0.16f),
                    title = "AI Opportunity & IP Match",
                    subtitle = "98% Compatibility · CleanTech",
                    trailingPulse = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .widthIn(max = 228.dp)
                )

                // Bottom-Start Hub Live Indicator
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue)
                        )
                        Text(
                            text = activeHubLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Staggered Trust Cards matching web hero right column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    HeroFloatingBadgeChip(
                        iconRes = R.drawable.ic_nda_shield_badge,
                        iconBg = CobaltBlue.copy(alpha = 0.14f),
                        title = "Mutual NDA Protected",
                        subtitle = "Zero-knowledge encrypted deal rooms",
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    HeroFloatingBadgeChip(
                        iconRes = R.drawable.ic_patent_growth_badge,
                        iconBg = ElectricCyan.copy(alpha = 0.14f),
                        title = "Provisional Patent Secured",
                        subtitle = "USPTO Priority Date Locked",
                        trailingVerified = true,
                        modifier = Modifier.fillMaxWidth(0.92f)
                    )
                }
            }

            // Website-exact "INVENT · PROTECT · FUND · SCALE" brand ribbon
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("INVENT", "PROTECT", "FUND", "SCALE").forEachIndexed { idx, word ->
                        Text(
                            text = word,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (idx < 3) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_brand_spark_badge),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFloatingBadgeChip(
    @DrawableRes iconRes: Int,
    iconBg: Color,
    title: String,
    subtitle: String,
    trailingPulse: Boolean = false,
    trailingVerified: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailingPulse) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(CobaltBlue)
                )
            }
            if (trailingVerified) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Verified",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SvgShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "svg_fetch_shimmer")
    val progress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "svg_shimmer_sweep"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 420f - 180f, 0f),
        end = Offset(progress * 420f, 160f)
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
    )
}

/**
 * Overlapping circular portrait cluster replacing generic initial-only circles
 * in the Hero social proof row. Loads live innovator photos via Coil with clean
 * monogram fallback when offline.
 */
@Composable
fun InnovatorAvatarCluster(
    avatarUrls: List<Pair<String, String>>,
    avatarSize: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val fallbackPalette = remember {
        listOf(
            Color(0xFF0EA5E9),
            Color(0xFF7041C7),
            Color(0xFF10B981),
            Color(0xFFF59E0B)
        )
    }

    Box(
        modifier = modifier
            .width(avatarSize + 18.dp * (avatarUrls.size - 1).coerceAtLeast(0))
            .height(avatarSize)
            .testTag("hero_innovator_avatars")
    ) {
        avatarUrls.forEachIndexed { idx, (initials, url) ->
            val fallbackBg = fallbackPalette[idx % fallbackPalette.size]
            Box(
                modifier = Modifier
                    .offset(x = (idx * 18).dp)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(fallbackBg)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = "Innovator $initials",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    error = {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                )
            }
        }
    }
}
