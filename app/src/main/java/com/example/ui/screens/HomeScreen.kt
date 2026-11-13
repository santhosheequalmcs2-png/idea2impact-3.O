package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BackendInnovation
import com.example.data.model.CategoryItem
import com.example.data.seed.SeedCatalog
import com.example.ui.components.HeroAmbientLottieBackdrop
import com.example.ui.components.HeroIllustration
import com.example.ui.components.InnovatorAvatarCluster
import com.example.ui.components.formatCurrency
import com.example.ui.components.shareInnovationSheet
import com.example.ui.components.sharedInnovationBounds
import com.example.ui.components.sharedInnovationElement
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PurpleGlow
import com.example.util.LocalHapticFeedbackManager
import com.example.util.StartupSoundPlayer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    categories: List<CategoryItem>,
    featuredInnovations: List<BackendInnovation> = emptyList(),
    entranceReady: Boolean = true,
    onOpenDetail: (BackendInnovation) -> Unit = {},
    onExploreInnovations: (category: String) -> Unit,
    onMeetBuilders: () -> Unit,
    onOpenPricing: () -> Unit,
    onReplayOnboarding: () -> Unit = {}
) {
    var heroVisible by remember { mutableStateOf(false) }
    var metricsVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedbackManager.current

    LaunchedEffect(entranceReady) {
        if (entranceReady) {
            heroVisible = true
            kotlinx.coroutines.delay(180L)
            metricsVisible = true
        } else {
            heroVisible = false
            metricsVisible = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Unified Responsive Hero Grid (2-column on wide/tablet, stacked grid on compact)
        item(key = "home_hero_grid") {
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 6 }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    tonalElevation = 1.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                    ) {
                        HeroAmbientLottieBackdrop(
                            modifier = Modifier.matchParentSize()
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            val isWideHero = maxWidth >= 680.dp
                            val useSideBySideCtas = maxWidth >= 340.dp

                            if (isWideHero) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeroCopyColumn(
                                        useSideBySideCtas = true,
                                        onExploreInnovations = onExploreInnovations,
                                        onMeetBuilders = onMeetBuilders,
                                        modifier = Modifier.weight(1.06f)
                                    )
                                    HeroIllustration(
                                        modifier = Modifier.weight(1.0f)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    HeroCopyColumn(
                                        useSideBySideCtas = useSideBySideCtas,
                                        onExploreInnovations = onExploreInnovations,
                                        onMeetBuilders = onMeetBuilders,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HeroIllustration(
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Trust Metric Strip (4 Columns)
        item {
            AnimatedVisibility(
                visible = metricsVisible,
                enter = fadeIn(tween(460)) + slideInVertically(tween(460)) { it / 5 }
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricColumn("40+", "Verified Innovations", Modifier.weight(1f))
                        MetricColumn("\$12.8M", "Capital Pipeline", Modifier.weight(1f))
                        MetricColumn("1,200+", "Active Builders & Engineers", Modifier.weight(1f))
                        MetricColumn("98.4%", "Mutual NDA Compliance", Modifier.weight(1f))
                    }
                }
            }
        }

        // 3B. Featured Breakthroughs Quick Carousel (instant from Room cache or network)
        if (featuredInnovations.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Featured Breakthroughs", style = MaterialTheme.typography.titleLarge)
                        TextButton(
                            onClick = {
                                haptics.tap()
                                onExploreInnovations("All")
                            }
                        ) {
                            Text("Explore All")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(featuredInnovations.take(6), key = { it.id }) { item ->
                            Surface(
                                onClick = {
                                    haptics.tap()
                                    onOpenDetail(item)
                                },
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                                shadowElevation = 2.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.width(248.dp).testTag("home_featured_${item.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.sharedInnovationElement("innovation_category_${item.id}")
                                        ) {
                                            Text(
                                                text = item.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            color = EmeraldSuccess.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.sharedInnovationElement("innovation_stage_${item.id}")
                                        ) {
                                            Text(
                                                text = item.stage,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldSuccess,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.sharedInnovationBounds("innovation_title_${item.id}")
                                    )

                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Goal: ${formatCurrency(item.fundingGoal)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                haptics.tap()
                                                StartupSoundPlayer.playTap(context)
                                                shareInnovationSheet(context, item)
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("home_featured_share_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Share,
                                                contentDescription = "Share ${item.title}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "Inspect ${item.title}",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. How It Works (4 Steps with Vector Badges)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("How It Works", style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = {
                            haptics.tap()
                            onReplayOnboarding()
                        },
                        modifier = Modifier.testTag("home_replay_onboarding_button")
                    ) {
                        Text("Process Tour")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val steps = listOf(
                    Triple(
                        "01. Submit & Protect",
                        "Lock in priority date with USPTO-aligned disclosures & mutual digital NDAs.",
                        R.drawable.ic_nda_shield_badge to CobaltBlue.copy(alpha = 0.14f)
                    ),
                    Triple(
                        "02. AI Opportunity Match",
                        "AI Sana scans prior-art, calculates TAM/SAM/SOM, and maps investor fit.",
                        R.drawable.ic_brand_spark_badge to PurpleGlow.copy(alpha = 0.14f)
                    ),
                    Triple(
                        "03. Assemble Elite Builders",
                        "Connect with vetted firmware architects, full-stack engineers, and patent attorneys.",
                        R.drawable.ic_builder_network_badge to PurpleGlow.copy(alpha = 0.12f)
                    ),
                    Triple(
                        "04. Capital & Licensing",
                        "Secure pre-seed micro-backing, angel syndicates, or commercial IP licensing.",
                        R.drawable.ic_patent_growth_badge to ElectricCyan.copy(alpha = 0.14f)
                    )
                )
                steps.forEach { (title, desc, iconPair) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(iconPair.second),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = iconPair.first),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. 8 Exact Categories Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore Categories", style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = {
                            haptics.tap()
                            onExploreInnovations("All")
                        }
                    ) {
                        Text("View All")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    maxItemsInEachRow = 2
                ) {
                    categories.forEach { cat ->
                        Surface(
                            onClick = {
                                haptics.tap()
                                onExploreInnovations(cat.name)
                            },
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 112.dp)
                                .testTag("home_category_${cat.name.lowercase().replace(" ", "_")}")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat.name),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${cat.count}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Membership Plans Card
        item {
            Surface(
                onClick = {
                    haptics.tap()
                    onOpenPricing()
                },
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().testTag("home_pricing_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SIMPLE PLANS. BIG POSSIBILITIES.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Find your way to what’s next.", style = MaterialTheme.typography.titleSmall)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View plans", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun HeroCopyColumn(
    useSideBySideCtas: Boolean,
    onExploreInnovations: (category: String) -> Unit,
    onMeetBuilders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedbackManager.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Website-faithful Eyebrow Row with pulse dot + gradient accent line
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CobaltBlue)
            )
            Text(
                text = "GLOBAL INNOVATION EXCHANGE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PurpleGlow, CobaltBlue.copy(alpha = 0.1f))
                        )
                    )
            )
        }

        Text(
            text = buildAnnotatedString {
                append("Where breakthrough ideas meet ")
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            listOf(PurpleGlow, CobaltBlue, ElectricCyan)
                        )
                    )
                ) {
                    append("global capital & elite builders.")
                }
            },
            style = MaterialTheme.typography.displayMedium
        )

        Text(
            text = "The international innovation marketplace connecting visionary creators, robotics architects, and angel funds. Turn raw concepts into protected, funded, high-impact reality.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (useSideBySideCtas) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroPrimaryExploreButton(
                    onClick = {
                        haptics.tap()
                        onExploreInnovations("All")
                    },
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onMeetBuilders()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("home_meet_builders_button")
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Meet Builders", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroPrimaryExploreButton(
                    onClick = {
                        haptics.tap()
                        onExploreInnovations("All")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onMeetBuilders()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("home_meet_builders_button")
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Meet Elite Builders")
                }
            }
        }

        // Social Proof Row: Overlapping fetched innovator portraits + text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            val heroAvatars = remember {
                SeedCatalog.talents.take(4).map { talent ->
                    val initials = talent.name.split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                    initials to talent.avatarUrl
                }
            }
            InnovatorAvatarCluster(avatarUrls = heroAvatars)
            Text(
                text = "Backed by 2,400+ innovators across 18 countries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeroPrimaryExploreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(PurpleGlow, CobaltBlue)
                )
            )
            .clickable(onClick = onClick)
            .testTag("home_explore_innovations_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp)
        ) {
            Text(
                text = "Explore Ideas",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Filled.NorthEast,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

private fun categoryIcon(name: String): ImageVector = when (name) {
    "Ideas" -> Icons.Filled.Lightbulb
    "Inventions" -> Icons.Filled.PrecisionManufacturing
    "Physical Products" -> Icons.Filled.Inventory2
    "Websites" -> Icons.Filled.Public
    "Mobile Apps" -> Icons.Filled.PhoneAndroid
    "AI & Robotics" -> Icons.Filled.SmartToy
    "Software" -> Icons.Filled.Terminal
    "Green Tech" -> Icons.Filled.Eco
    else -> Icons.Filled.Devices
}
