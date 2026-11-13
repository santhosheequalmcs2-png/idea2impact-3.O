package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.analytics.LocalAnalyticsTracker
import com.example.data.model.BackendInnovation
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoseError
import com.example.util.LocalHapticFeedbackManager
import com.example.util.StartupSoundPlayer
import java.text.NumberFormat
import java.util.Locale

fun shareInnovationSheet(
    context: Context,
    innovation: BackendInnovation
) {
    val cleanId = innovation.id.trim()
    val innovationUrl = if (cleanId.isNotBlank()) {
        "https://idea2impact-yges.vercel.app/innovations/${Uri.encode(cleanId)}"
    } else {
        "https://idea2impact-yges.vercel.app/marketplace"
    }
    val shareText = buildString {
        append(innovation.title)
        append(" (${innovation.category} · ${innovation.stage})\n")
        if (innovation.fundingGoal > 0.0) {
            append("Funding Goal: ${formatCurrency(innovation.fundingGoal)}\n")
        }
        append(innovationUrl)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, innovation.title)
        putExtra(Intent.EXTRA_TITLE, "${innovation.title} · IDEA2IMPACT")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(sendIntent, "Share Innovation").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(chooser) }
}

@Composable
fun BackendInnovationCard(
    innovation: BackendInnovation,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    analyticsSource: String = "marketplace_card",
    onShare: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedbackManager.current
    val analytics = LocalAnalyticsTracker.current
    val likeScale by animateFloatAsState(
        targetValue = if (innovation.isLiked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "like_scale"
    )
    val bookmarkScale by animateFloatAsState(
        targetValue = if (innovation.isBookmarked) 1.16f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bookmark_scale"
    )

    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("innovation_card_${innovation.id}")
    ) {
        Column {
            // High-res product thumbnail with Category & Stage badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .sharedInnovationElement("innovation_image_${innovation.id}")
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            ) {
                if (!innovation.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = innovation.imageUrl,
                        contentDescription = innovation.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.img_hero_innovation),
                        error = painterResource(id = R.drawable.img_hero_innovation),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_innovation),
                        contentDescription = innovation.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.65f))
                            )
                        )
                )

                // Top Row: Category tag badge + Stage indicator pill in emerald green
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC080B14),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                        modifier = Modifier.sharedInnovationElement("innovation_category_${innovation.id}")
                    ) {
                        Text(
                            text = innovation.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEDF2FF),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = EmeraldSuccess,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.sharedInnovationElement("innovation_stage_${innovation.id}")
                    ) {
                        Text(
                            text = innovation.stage,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = innovation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.sharedInnovationBounds("innovation_title_${innovation.id}")
                )

                Text(
                    text = innovation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Funding Goal & Valuation strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "Funding Goal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(innovation.fundingGoal),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "Valuation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(innovation.valuation),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Creator avatar + Like & Bookmark actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!innovation.authorAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model = innovation.authorAvatar,
                                contentDescription = innovation.authorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(30.dp).clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = innovation.authorName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = innovation.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (innovation.isVerifiedCreator) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = innovation.commentCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.heartLike(willBeLiked = !innovation.isLiked)
                                onToggleLike()
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(min = 48.dp)
                                .testTag("like_button_${innovation.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (innovation.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like ${innovation.title}",
                                    tint = if (innovation.isLiked) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp).scale(likeScale)
                                )
                                Text(
                                    text = innovation.likeCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (innovation.isLiked) RoseError else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptics.toggle(!innovation.isBookmarked)
                                onToggleBookmark()
                            },
                            modifier = Modifier.size(48.dp).testTag("bookmark_button_${innovation.id}")
                        ) {
                            Icon(
                                imageVector = if (innovation.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark ${innovation.title}",
                                tint = if (innovation.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp).scale(bookmarkScale)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.tap()
                                StartupSoundPlayer.playTap(context)
                                analytics.logInnovationShared(
                                    innovationId = innovation.id,
                                    title = innovation.title,
                                    category = innovation.category,
                                    source = analyticsSource
                                )
                                if (onShare != null) onShare() else shareInnovationSheet(context, innovation)
                            },
                            modifier = Modifier.size(48.dp).testTag("share_button_${innovation.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share ${innovation.title}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    if (amount <= 0.0) return "Stealth / TBD"
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }
    return formatter.format(amount)
}
