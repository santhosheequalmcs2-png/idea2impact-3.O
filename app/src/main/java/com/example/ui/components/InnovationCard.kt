package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Innovation
import com.example.data.model.InnovationCatalog
import com.example.ui.theme.RoseError
import com.example.util.SecurityCryptoUtils

@Composable
fun InnovationCard(
    innovation: Innovation,
    isLiked: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stageLabel = InnovationCatalog.lifecycleStages.getOrElse(innovation.stageIndex.toInt()) { "Concept" }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("innovation_card_${innovation.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Card Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                if (!innovation.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = innovation.imageUrl,
                        contentDescription = innovation.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.img_hero_innovation),
                        error = painterResource(R.drawable.img_hero_innovation),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.img_hero_innovation),
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
                                listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                // Top Row Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xCC080B14),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = innovation.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = SecurityCryptoUtils.formatPrice(innovation.price),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Bottom Left Stage Pill
                Surface(
                    color = Color(0xBB101625),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = stageLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5CE9C1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Card Content
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = innovation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = innovation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(2.dp))

                // Footer Author & Engagement Row
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
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = innovation.authorName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Text(
                            text = innovation.authorName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("like_button_${innovation.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like ${innovation.title}",
                                    tint = if (isLiked) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = innovation.likeCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Comments count",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = innovation.commentCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(2.dp))

                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("save_button_${innovation.id}")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark ${innovation.title}",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
