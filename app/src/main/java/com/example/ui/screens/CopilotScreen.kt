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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackendInnovation
import com.example.data.model.CopilotMessage
import com.example.ui.components.NeuralCore3D
import com.example.ui.theme.EmeraldSuccess
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

@Composable
fun CopilotScreen(
    messages: List<CopilotMessage>,
    isLoading: Boolean,
    customApiKey: String,
    availableInnovations: List<BackendInnovation> = emptyList(),
    focusedInnovation: BackendInnovation? = null,
    useFastModel: Boolean = false,
    onSelectFocusedInnovation: (BackendInnovation?) -> Unit = {},
    onToggleFastModel: (Boolean) -> Unit = {},
    onSaveApiKey: (String) -> Unit,
    onSendPrompt: (String, Bitmap?) -> Unit,
    onResetChat: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var copiedMessageId by remember { mutableStateOf<String?>(null) }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val decoded = decodeDownscaledBitmap(context, uri, maxDimension = 1024)
                if (decoded != null) {
                    attachedBitmap = decoded
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
        "● ✦ CUSTOM KEY · GEMINI"
    } else if (useFastModel) {
        "● ⚡ GEMINI 3.5 FLASH-LITE"
    } else {
        "● ✦ GEMINI 3.8 FLASH · STREAMING"
    }

    val chipModelName = if (useFastModel) "Gemini 3.5 Flash-Lite" else "Gemini 3.8 Flash"

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Header: "Idea Assistant" + Live Gemini Badge + Speed Toggle + Key Settings + New Chat
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Idea Assistant", style = MaterialTheme.typography.titleLarge)
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess)
                                )
                                Text(
                                    text = modelLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldSuccess,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = useFastModel,
                            onClick = { onToggleFastModel(!useFastModel) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(if (useFastModel) "Fast" else "Deep") },
                            modifier = Modifier.heightIn(min = 40.dp).testTag("copilot_model_toggle")
                        )
                        IconButton(
                            onClick = { showKeyDialog = true },
                            modifier = Modifier.size(48.dp).testTag("copilot_key_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = "Google AI Studio Key Settings",
                                tint = if (customApiKey.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onResetChat,
                            modifier = Modifier.heightIn(min = 42.dp).testTag("copilot_new_chat")
                        ) {
                            Icon(Icons.Filled.AddComment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New")
                        }
                    }
                }

                // Innovation Context Selector Row (Ground AI on any innovation)
                if (availableInnovations.isNotEmpty() || focusedInnovation != null) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        item {
                            FilterChip(
                                selected = focusedInnovation == null,
                                onClick = { onSelectFocusedInnovation(null) },
                                label = { Text("General Context") },
                                modifier = Modifier.heightIn(min = 36.dp).testTag("copilot_context_general")
                            )
                        }
                        val quickItems = buildList {
                            if (focusedInnovation != null) add(focusedInnovation)
                            availableInnovations.filter { it.id != focusedInnovation?.id }.take(8).forEach { add(it) }
                        }
                        items(quickItems, key = { it.id }) { item ->
                            FilterChip(
                                selected = focusedInnovation?.id == item.id,
                                onClick = {
                                    onSelectFocusedInnovation(if (focusedInnovation?.id == item.id) null else item)
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 170.dp)
                                    )
                                },
                                modifier = Modifier.heightIn(min = 36.dp).testTag("copilot_context_${item.id}")
                            )
                        }
                    }
                }

                // 6 Quick Action Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QUICK_ACTIONS) { action ->
                        AssistChip(
                            onClick = {
                                if (!isLoading) {
                                    onSendPrompt(action.prompt, attachedBitmap)
                                    attachedBitmap = null
                                }
                            },
                            label = { Text(action.chipLabel) }
                        )
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "copilot_3d_neural_core") {
                NeuralCore3D(isThinking = isLoading)
            }
            items(messages, key = { it.id }) { msg ->
                val isCopied = copiedMessageId == msg.id
                val isCurrentlyStreaming = isLoading && msg.isAssistant && msg.id == messages.lastOrNull()?.id

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isAssistant) Arrangement.Start else Arrangement.End
                ) {
                    Surface(
                        color = if (msg.isAssistant) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(
                            1.dp,
                            if (msg.isAssistant) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
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
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isCurrentlyStreaming) "$chipModelName · Streaming..." else chipModelName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (!isCurrentlyStreaming && msg.content.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("Idea Assistant Reply", msg.content))
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

                            if (msg.content.isBlank() && isCurrentlyStreaming) {
                                Text(
                                    text = "Synthesizing analysis...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = formatMarkdownText(
                                        raw = msg.content,
                                        primaryAccent = MaterialTheme.colorScheme.primary,
                                        codeBg = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (msg.isAssistant) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Composer Row (with optional Prototype Image attachment preview)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val preview = attachedBitmap
                if (preview != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Attached prototype image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Prototype photo attached for Gemini vision analysis",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { attachedBitmap = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove attached image")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isLoading,
                        modifier = Modifier.size(48.dp).testTag("copilot_attach_image")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "Attach prototype or schematic image",
                            tint = if (attachedBitmap != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Text(
                                if (focusedInnovation != null) {
                                    "Ask about ${focusedInnovation.title}..."
                                } else {
                                    "Ask about patent novelty, TAM, BOM, or attach a sketch..."
                                }
                            )
                        },
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("copilot_prompt_input")
                    )
                    Button(
                        onClick = {
                            val prompt = input.trim()
                            if (prompt.isNotBlank()) {
                                val img = attachedBitmap
                                input = ""
                                attachedBitmap = null
                                onSendPrompt(prompt, img)
                            }
                        },
                        enabled = !isLoading && input.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.heightIn(min = 52.dp).testTag("copilot_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
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
                        "Firebase AI Logic (Gemini 3.8 Flash) works automatically with zero API key. Optionally paste a personal Google AI Studio key (AIzaSy...) to route text chats via the custom backend.",
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

private fun formatMarkdownText(
    raw: String,
    primaryAccent: Color,
    codeBg: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = raw.lines()
        lines.forEachIndexed { index, originalLine ->
            val trimmed = originalLine.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryAccent)) {
                        append(trimmed.removePrefix("### ").trim())
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryAccent)) {
                        append(trimmed.removePrefix("## ").trim())
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryAccent)) {
                        append(trimmed.removePrefix("# ").trim())
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryAccent)) {
                        append("• ")
                    }
                    appendInlineFormatting(trimmed.drop(2), codeBg)
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val dotIdx = trimmed.indexOf('.')
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryAccent)) {
                        append(trimmed.substring(0, dotIdx + 2))
                    }
                    appendInlineFormatting(trimmed.substring(dotIdx + 2), codeBg)
                }
                else -> appendInlineFormatting(originalLine, codeBg)
            }
            if (index < lines.lastIndex) append('\n')
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatting(text: String, codeBg: Color) {
    var cursor = 0
    while (cursor < text.length) {
        val nextBold = text.indexOf("**", cursor)
        val nextCode = text.indexOf('`', cursor)
        val isBoldNext = nextBold != -1 && (nextCode == -1 || nextBold < nextCode)
        val isCodeNext = nextCode != -1 && (nextBold == -1 || nextCode < nextBold)

        when {
            isBoldNext -> {
                if (nextBold > cursor) append(text.substring(cursor, nextBold))
                val endBold = text.indexOf("**", nextBold + 2)
                if (endBold == -1) {
                    append(text.substring(nextBold))
                    break
                }
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(nextBold + 2, endBold))
                }
                cursor = endBold + 2
            }
            isCodeNext -> {
                if (nextCode > cursor) append(text.substring(cursor, nextCode))
                val endCode = text.indexOf('`', nextCode + 1)
                if (endCode == -1) {
                    append(text.substring(nextCode))
                    break
                }
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBg.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                ) {
                    append(text.substring(nextCode + 1, endCode))
                }
                cursor = endCode + 1
            }
            else -> {
                append(text.substring(cursor))
                break
            }
        }
    }
}
