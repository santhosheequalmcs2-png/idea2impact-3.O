package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.BackendInnovation
import com.example.data.model.CopilotMessage
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldSuccess
import kotlin.math.roundToInt

private val POPUP_QUICK_PROMPTS = listOf(
    "Summarize" to "Give a crisp 3-bullet executive summary of this innovation and its core edge.",
    "Valuation" to "Estimate realistic pre-seed/seed valuation benchmarks and key value drivers.",
    "IP Risks" to "List top 3 patentability / prior-art risks and how to protect the IP."
)

@Composable
fun MovableAiPopup(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    messages: List<CopilotMessage>,
    isLoading: Boolean,
    useFastModel: Boolean,
    activeContext: BackendInnovation?,
    onToggleFastModel: (Boolean) -> Unit,
    onSendPrompt: (String) -> Unit,
    onOpenFullCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .zIndex(15f)
    ) {
        val marginPx = with(density) { 12.dp.toPx() }
        val topMinPx = with(density) { 64.dp.toPx() }
        val bottomReservedPx = with(density) { 84.dp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        val bubbleSizeDp = 58.dp
        val bubbleSizePx = with(density) { bubbleSizeDp.toPx() }

        val popupWidthDp = (maxWidth - 24.dp).coerceAtMost(356.dp)
        val popupHeightDp = (maxHeight * 0.58f).coerceIn(320.dp, 450.dp)
        val popupWidthPx = with(density) { popupWidthDp.toPx() }
        val popupHeightPx = with(density) { popupHeightDp.toPx() }

        // Separate draggable coordinates for bubble vs popup, initialized cleanly on first measure
        var bubbleInitialized by rememberSaveable { mutableStateOf(false) }
        var bubbleX by rememberSaveable { mutableFloatStateOf(0f) }
        var bubbleY by rememberSaveable { mutableFloatStateOf(0f) }

        var popupInitialized by rememberSaveable { mutableStateOf(false) }
        var popupX by rememberSaveable { mutableFloatStateOf(0f) }
        var popupY by rememberSaveable { mutableFloatStateOf(0f) }

        val maxBubbleX = (maxWidthPx - bubbleSizePx - marginPx).coerceAtLeast(marginPx)
        val maxBubbleY = (maxHeightPx - bubbleSizePx - bottomReservedPx).coerceAtLeast(topMinPx)

        val maxPopupX = (maxWidthPx - popupWidthPx - marginPx).coerceAtLeast(marginPx)
        val maxPopupY = (maxHeightPx - popupHeightPx - marginPx).coerceAtLeast(topMinPx)

        LaunchedEffect(maxWidthPx, maxHeightPx) {
            if (!bubbleInitialized && maxWidthPx > 0f && maxHeightPx > 0f) {
                bubbleX = maxBubbleX
                bubbleY = (maxBubbleY - with(density) { 18.dp.toPx() }).coerceAtLeast(topMinPx)
                bubbleInitialized = true
            } else {
                bubbleX = bubbleX.coerceIn(marginPx, maxBubbleX)
                bubbleY = bubbleY.coerceIn(topMinPx, maxBubbleY)
            }

            if (!popupInitialized && maxWidthPx > 0f && maxHeightPx > 0f) {
                popupX = ((maxWidthPx - popupWidthPx) / 2f).coerceIn(marginPx, maxPopupX)
                popupY = (maxHeightPx - popupHeightPx - bottomReservedPx).coerceIn(topMinPx, maxPopupY)
                popupInitialized = true
            } else {
                popupX = popupX.coerceIn(marginPx, maxPopupX)
                popupY = popupY.coerceIn(topMinPx, maxPopupY)
            }
        }

        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.75f, animationSpec = tween(200)),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.75f, animationSpec = tween(140))
        ) {
            var isDraggingBubble by remember { mutableStateOf(false) }
            val bubbleScale by animateFloatAsState(
                targetValue = if (isDraggingBubble) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ai_bubble_drag_scale"
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(
                    1.5.dp,
                    Brush.linearGradient(listOf(ElectricCyan, CobaltBlue, MaterialTheme.colorScheme.primary))
                ),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            bubbleX.coerceIn(marginPx, maxBubbleX).roundToInt(),
                            bubbleY.coerceIn(topMinPx, maxBubbleY).roundToInt()
                        )
                    }
                    .size(bubbleSizeDp)
                    .scale(bubbleScale)
                    .pointerInput(maxBubbleX, maxBubbleY) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingBubble = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDragEnd = { isDraggingBubble = false },
                            onDragCancel = { isDraggingBubble = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                bubbleX = (bubbleX + dragAmount.x).coerceIn(marginPx, maxBubbleX)
                                bubbleY = (bubbleY + dragAmount.y).coerceIn(topMinPx, maxBubbleY)
                            }
                        )
                    }
                    .clip(CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Anchor popup near current bubble position clamped to screen
                        popupX = (bubbleX - popupWidthPx + bubbleSizePx).coerceIn(marginPx, maxPopupX)
                        popupY = (bubbleY - popupHeightPx + bubbleSizePx).coerceIn(topMinPx, maxPopupY)
                        onExpandedChange(true)
                    }
                    .testTag("movable_ai_bubble")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Open movable AI Assistant",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(7.dp)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.86f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.88f, animationSpec = tween(160))
        ) {
            var inputText by rememberSaveable { mutableStateOf("") }
            var isDraggingPopup by remember { mutableStateOf(false) }
            val listState = rememberLazyListState()

            LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.lastIndex)
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(
                    width = if (isDraggingPopup) 2.dp else 1.2.dp,
                    brush = Brush.linearGradient(listOf(ElectricCyan.copy(alpha = 0.8f), MaterialTheme.colorScheme.primary))
                ),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            popupX.coerceIn(marginPx, maxPopupX).roundToInt(),
                            popupY.coerceIn(topMinPx, maxPopupY).roundToInt()
                        )
                    }
                    .size(width = popupWidthDp, height = popupHeightDp)
                    .testTag("movable_ai_popup")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Draggable Pop-Up Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .pointerInput(maxPopupX, maxPopupY) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDraggingPopup = true
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    },
                                    onDragEnd = { isDraggingPopup = false },
                                    onDragCancel = { isDraggingPopup = false },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        popupX = (popupX + dragAmount.x).coerceIn(marginPx, maxPopupX)
                                        popupY = (popupY + dragAmount.y).coerceIn(topMinPx, maxPopupY)
                                    }
                                )
                            }
                            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                            .testTag("movable_ai_drag_handle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = "Drag AI pop-up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini AI Pop-up",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = activeContext?.title ?: "Drag bar to move anywhere",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilterChip(
                            selected = useFastModel,
                            onClick = { onToggleFastModel(!useFastModel) },
                            leadingIcon = { Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = {
                                Text(
                                    text = if (useFastModel) "Fast" else "Deep",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )

                        IconButton(
                            onClick = onOpenFullCopilot,
                            modifier = Modifier.size(40.dp).testTag("movable_ai_expand_tab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OpenInFull,
                                contentDescription = "Open full AI Copilot",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandedChange(false)
                            },
                            modifier = Modifier.size(40.dp).testTag("movable_ai_close")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Minimize AI pop-up",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Quick Prompt Chips
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(POPUP_QUICK_PROMPTS) { (label, prompt) ->
                            AssistChip(
                                onClick = {
                                    if (!isLoading) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendPrompt(prompt)
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Streaming Chat Messages
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (msg.isAssistant) Arrangement.Start else Arrangement.End
                            ) {
                                Surface(
                                    color = if (msg.isAssistant) {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier.widthIn(max = popupWidthDp - 44.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        if (msg.isAssistant && msg.content.isBlank() && isLoading) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = "Thinking...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = msg.content.replace("### ", "").replace("**", ""),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (msg.isAssistant) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Compact Input Composer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Ask Gemini...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).testTag("movable_ai_input")
                        )

                        Surface(
                            shape = CircleShape,
                            color = if (inputText.isNotBlank() && !isLoading) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                                    val outgoing = inputText.trim()
                                    if (outgoing.isNotBlank()) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSendPrompt(outgoing)
                                        inputText = ""
                                    }
                                }
                                .testTag("movable_ai_send")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send prompt",
                                    tint = if (inputText.isNotBlank() && !isLoading) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
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
