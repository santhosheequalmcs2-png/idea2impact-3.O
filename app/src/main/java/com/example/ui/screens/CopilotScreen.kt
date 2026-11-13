package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.analytics.LocalAnalyticsTracker
import com.example.data.model.BackendInnovation
import com.example.data.model.CopilotMessage
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.components.NeuralCore3D
import com.example.ui.theme.EmeraldSuccess
import com.example.util.LocalHapticFeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class QuickActionPrompt(
    val chipLabel: String,
    val prompt: String
)

private val QUICK_ACTIONS = listOf(
    QuickActionPrompt(
        "🛡️ Patentability & Prior-Art Scan",
        "Run a comprehensive patentability & prior-art scan for this innovation. Identify potential USPTO/PCT claim pillars and freedom-to-operate risks."
    ),
    QuickActionPrompt(
        "📊 Market Sizing (TAM/SAM/SOM)",
        "Calculate a bottom-up TAM, SAM, and SOM market sizing breakdown with unit economics and GTM beachhead wedges."
    ),
    QuickActionPrompt(
        "📑 1-Min Investor Pitch Deck",
        "Draft a crisp 1-minute, 5-slide investor pitch deck covering Problem, Breakthrough Solution, Market Size, IP Moat, and Capital Ask."
    ),
    QuickActionPrompt(
        "🛠️ Prototyping & Tech Blueprint",
        "Outline a rapid prototyping & engineering architecture blueprint with hardware BOM / firmware stack and MVP milestones."
    ),
    QuickActionPrompt(
        "⚖️ IP Protection & NDA Strategy",
        "Recommend a layered IP protection & Mutual NDA strategy prior to disclosing CAD schematics to suppliers or angel investors."
    ),
    QuickActionPrompt(
        "🤝 Co-Founder & Talent Equity",
        "Design a fair co-founder & founding engineer equity vesting split (4-year cliff, milestone tranches) for a deep-tech startup."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
    messages: List<CopilotMessage>,
    isLoading: Boolean,
    customApiKey: String,
    availableInnovations: List<BackendInnovation> = emptyList(),
    focusedInnovation: BackendInnovation? = null,
    useFastModel: Boolean = false,
    canRegenerate: Boolean = false,
    onSelectFocusedInnovation: (BackendInnovation?) -> Unit = {},
    onToggleFastModel: (Boolean) -> Unit = {},
    onSaveApiKey: (String) -> Unit,
    onSendPrompt: (prompt: String, bitmap: Bitmap?, promptSource: String) -> Unit,
    onStopGeneration: () -> Unit = {},
    onRegenerateLast: () -> Unit = {},
    onResetChat: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var copiedMessageId by remember { mutableStateOf<String?>(null) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedbackManager.current
    val analytics = LocalAnalyticsTracker.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val decoded = decodeDownscaledBitmap(context, uri, maxDimension = 1024)
                if (decoded != null) {
                    attachedBitmap = decoded
                    analytics.logCopilotImageAttached()
                }
            }
        }
    }

    val lastContentLen = messages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(messages.size, lastContentLen, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex + 1)
        }
    }

    val modelLabel = if (customApiKey.isNotBlank()) {
        "● ✦ CUSTOM KEY · AI SANA"
    } else if (useFastModel) {
        "● ⚡ AI SANA FLASH-LITE"
    } else {
        "● ✦ AI SANA · STREAMING"
    }

    val chipModelName = if (useFastModel) "AI Sana Fast" else "AI Sana"

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Compact Header: "Idea Assistant" + Live Badge + Controls + Single Combined Context/Quick-Action Strip
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    ) {
                        Text(
                            text = "AI Sana",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                            Text(
                                text = modelLabel.removePrefix("● "),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccess,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                val nextFast = !useFastModel
                                haptics.toggle(nextFast)
                                onToggleFastModel(nextFast)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (useFastModel) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (useFastModel) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                }
                            ),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("copilot_model_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (useFastModel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (useFastModel) "Fast" else "Deep",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptics.tap()
                                showKeyDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("copilot_key_settings")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Key,
                                    contentDescription = "Google AI Studio Key Settings",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (customApiKey.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                haptics.tap()
                                onResetChat()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("copilot_new_chat")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AddComment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Combined slim row: Context dropdown pill + 6 Quick Action pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (availableInnovations.isNotEmpty() || focusedInnovation != null) {
                        item(key = "copilot_context_selector") {
                            Box {
                                Surface(
                                    onClick = {
                                        haptics.tap()
                                        showContextMenu = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (focusedInnovation != null) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (focusedInnovation != null) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                        }
                                    ),
                                    modifier = Modifier.heightIn(min = 30.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 9.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = focusedInnovation?.title ?: "General Context",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Select innovation context",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showContextMenu,
                                    onDismissRequest = { showContextMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("General Context") },
                                        leadingIcon = {
                                            if (focusedInnovation == null) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            haptics.tap()
                                            onSelectFocusedInnovation(null)
                                            showContextMenu = false
                                        },
                                        modifier = Modifier.testTag("copilot_context_general")
                                    )
                                    val quickItems = buildList {
                                        if (focusedInnovation != null) add(focusedInnovation)
                                        availableInnovations.filter { it.id != focusedInnovation?.id }.take(8).forEach { add(it) }
                                    }
                                    quickItems.forEach { item ->
                                        val isSelected = focusedInnovation?.id == item.id
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = item.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            leadingIcon = {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                haptics.tap()
                                                onSelectFocusedInnovation(if (isSelected) null else item)
                                                showContextMenu = false
                                            },
                                            modifier = Modifier.testTag("copilot_context_${item.id}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(QUICK_ACTIONS) { action ->
                        Surface(
                            onClick = {
                                if (!isLoading) {
                                    haptics.tap()
                                    analytics.logCopilotQuickActionTapped(
                                        actionLabel = action.chipLabel,
                                        surface = "full_screen"
                                    )
                                    onSendPrompt(action.prompt, attachedBitmap, "quick_action")
                                    attachedBitmap = null
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                            modifier = Modifier.heightIn(min = 30.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = action.chipLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Chat Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "copilot_3d_neural_core") {
                NeuralCore3D(isThinking = isLoading)
            }
            items(messages, key = { it.id }) { msg ->
                val isCopied = copiedMessageId == msg.id
                val isLastMessage = msg.id == messages.lastOrNull()?.id
                val isCurrentlyStreaming = isLoading && msg.isAssistant && isLastMessage

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isAssistant) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (msg.isAssistant) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            when {
                                msg.isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                                msg.isAssistant -> MaterialTheme.colorScheme.outline
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(max = 540.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (msg.isAssistant) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isCurrentlyStreaming) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(13.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isCurrentlyStreaming) "$chipModelName · Thinking..." else chipModelName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (msg.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!isCurrentlyStreaming && msg.content.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (isLastMessage && canRegenerate && msg.id != "welcome") {
                                                TextButton(
                                                    onClick = {
                                                        haptics.tap()
                                                        onRegenerateLast()
                                                    },
                                                    modifier = Modifier.testTag("regenerate_reply")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Refresh,
                                                        contentDescription = "Regenerate response",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                    Text(
                                                        text = "Retry",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    haptics.success()
                                                    analytics.logCopilotReplyCopied()
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("AI Sana Reply", msg.content))
                                                    copiedMessageId = msg.id
                                                    scope.launch {
                                                        delay(2000L)
                                                        if (copiedMessageId == msg.id) copiedMessageId = null
                                                    }
                                                },
                                                modifier = Modifier.testTag("copy_reply_${msg.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                                    contentDescription = "Copy response",
                                                    tint = if (isCopied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCopied) "Copied" else "Copy",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isCopied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (msg.content.isBlank() && isCurrentlyStreaming) {
                                Text(
                                    text = "Synthesizing architecture & diligence...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                MarkdownMessageContent(
                                    raw = msg.content,
                                    textColor = if (msg.isAssistant) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                    primaryAccent = MaterialTheme.colorScheme.primary,
                                    codeBg = MaterialTheme.colorScheme.surfaceVariant,
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            val lastMsg = messages.lastOrNull()
            if (!isLoading && lastMsg != null && lastMsg.isAssistant && lastMsg.id != "welcome" && !lastMsg.isError) {
                item(key = "copilot_follow_up_chips") {
                    val followUps = remember(focusedInnovation?.id, lastMsg.id) {
                        buildFollowUpSuggestions(focusedInnovation)
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 4.dp)
                    ) {
                        items(followUps) { (label, followUpPrompt) ->
                            Surface(
                                onClick = {
                                    haptics.tap()
                                    analytics.logCopilotQuickActionTapped(
                                        actionLabel = label,
                                        surface = "follow_up_chip"
                                    )
                                    onSendPrompt(followUpPrompt, null, "follow_up_chip")
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Compact Composer Row (with optional Prototype Image attachment preview)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val preview = attachedBitmap
                if (preview != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Attached prototype image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Text(
                            text = "Prototype photo attached for vision analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                haptics.tap()
                                attachedBitmap = null
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove attached image",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = {
                            if (!isLoading) {
                                haptics.tap()
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("copilot_attach_image")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Attach prototype or schematic image",
                                modifier = Modifier.size(18.dp),
                                tint = if (attachedBitmap != null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val promptInteractionSource = remember { MutableInteractionSource() }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        singleLine = false,
                        maxLines = 3,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        interactionSource = promptInteractionSource,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp, max = 92.dp)
                            .testTag("copilot_prompt_input")
                    ) { innerTextField ->
                        OutlinedTextFieldDefaults.DecorationBox(
                            value = input,
                            innerTextField = innerTextField,
                            enabled = true,
                            singleLine = false,
                            visualTransformation = VisualTransformation.None,
                            interactionSource = promptInteractionSource,
                            placeholder = {
                                Text(
                                    text = if (focusedInnovation != null) {
                                        "Ask about ${focusedInnovation.title}..."
                                    } else {
                                        "Ask patent, TAM, BOM, or attach sketch..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            container = {
                                OutlinedTextFieldDefaults.Container(
                                    enabled = true,
                                    isError = false,
                                    interactionSource = promptInteractionSource,
                                    colors = OutlinedTextFieldDefaults.colors(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        )
                    }

                    if (isLoading) {
                        Button(
                            onClick = {
                                haptics.tap()
                                onStopGeneration()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("copilot_stop_button")
                        ) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Stop AI generation",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Stop", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                val prompt = input.trim()
                                if (prompt.isNotBlank()) {
                                    haptics.success()
                                    val img = attachedBitmap
                                    input = ""
                                    attachedBitmap = null
                                    onSendPrompt(prompt, img, "custom_input")
                                }
                            },
                            enabled = input.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("copilot_send_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Send", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (showKeyDialog) {
        var keyInput by remember { mutableStateOf(customApiKey) }
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("AI Model Key Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "AI Sana works automatically with zero API key. Optionally paste a personal API key (AIzaSy...) to route text chats via the custom backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text("Optional Key (AIzaSy...)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("copilot_api_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.success()
                        onSaveApiKey(keyInput)
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptics.tap()
                        onSaveApiKey("")
                        showKeyDialog = false
                    }
                ) {
                    Text("Use Default AI")
                }
            }
        )
    }
}

private fun buildFollowUpSuggestions(focusedInnovation: BackendInnovation?): List<Pair<String, String>> {
    return if (focusedInnovation != null) {
        listOf(
            "Stress-test risks" to "Stress-test the top 3 technical, regulatory, and market adoption risks for ${focusedInnovation.title} and how to mitigate each.",
            "Architecture & BOM" to "Break down a concrete MVP system architecture, core components, and rough BOM/cloud cost range for ${focusedInnovation.title}.",
            "Investor objections" to "What tough due-diligence questions will institutional VCs ask about ${focusedInnovation.title}, and how should we answer them?",
            "30-60-90 plan" to "Give a crisp 30-60-90 day execution plan with measurable validation gates for ${focusedInnovation.title}."
        )
    } else {
        listOf(
            "Stress-test risks" to "Stress-test the top technical and commercialization edge cases in what we just discussed.",
            "Compare top listings" to "Compare the strongest innovations currently in the IDEA2IMPACT marketplace by technical moat and commercial readiness.",
            "Turn into code/spec" to "Provide a concrete technical specification or starter code snippet for the proposed approach.",
            "30-60-90 plan" to "Turn this into a crisp 30-60-90 day execution roadmap with concrete milestones."
        )
    }
}

private suspend fun decodeDownscaledBitmap(
    context: Context,
    uri: Uri,
    maxDimension: Int
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDimension || boundsOpts.outHeight / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }.getOrNull()
}
