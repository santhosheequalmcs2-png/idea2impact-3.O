package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldSuccess
import com.example.util.LocalHapticFeedbackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface MarkdownBlock {
    data class TextBlock(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data object DividerBlock : MarkdownBlock
}

@Composable
fun MarkdownMessageContent(
    raw: String,
    textColor: Color,
    primaryAccent: Color,
    codeBg: Color,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val blocks = remember(raw) { parseMarkdownBlocks(raw) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.TextBlock -> {
                    if (block.text.isNotBlank()) {
                        Text(
                            text = formatMarkdownText(
                                raw = block.text,
                                primaryAccent = primaryAccent,
                                codeBg = codeBg,
                                compact = compact
                            ),
                            style = textStyle,
                            color = textColor
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeSnippetCard(
                        language = block.language,
                        code = block.code,
                        compact = compact
                    )
                }
                MarkdownBlock.DividerBlock -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetCard(
    language: String,
    code: String,
    compact: Boolean
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedbackManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        haptics.success()
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AI Sana Code", code))
                        copied = true
                        scope.launch {
                            delay(1800L)
                            copied = false
                        }
                    },
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = "Copy code snippet",
                        tint = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (copied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(
                text = code.trimEnd(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (compact) 11.sp else 12.sp,
                    lineHeight = if (compact) 15.sp else 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(10.dp)
            )
        }
    }
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val textBuffer = StringBuilder()
    val codeBuffer = StringBuilder()
    var inCodeBlock = false
    var codeLang = ""

    fun flushText() {
        val content = textBuffer.toString().trimEnd('\n')
        if (content.isNotBlank()) {
            result += MarkdownBlock.TextBlock(content)
        }
        textBuffer.clear()
    }

    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (!inCodeBlock) {
                flushText()
                inCodeBlock = true
                codeLang = trimmed.removePrefix("```").trim()
                codeBuffer.clear()
            } else {
                result += MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString())
                inCodeBlock = false
                codeLang = ""
                codeBuffer.clear()
            }
        } else if (inCodeBlock) {
            codeBuffer.appendLine(line)
        } else if (trimmed == "---" || trimmed == "***") {
            flushText()
            result += MarkdownBlock.DividerBlock
        } else {
            textBuffer.appendLine(line)
        }
    }

    if (inCodeBlock && codeBuffer.isNotBlank()) {
        result += MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString())
    }
    flushText()
    return result
}

fun formatMarkdownText(
    raw: String,
    primaryAccent: Color,
    codeBg: Color,
    compact: Boolean = false
): AnnotatedString {
    val h3Size = if (compact) 13.sp else 15.sp
    val h2Size = if (compact) 14.sp else 16.sp
    val h1Size = if (compact) 15.sp else 18.sp

    return buildAnnotatedString {
        val lines = raw.lines()
        lines.forEachIndexed { index, originalLine ->
            val indentCount = originalLine.takeWhile { it == ' ' }.length
            val trimmed = originalLine.trimStart()
            when {
                trimmed.startsWith("### ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = h3Size, color = primaryAccent)) {
                        append(trimmed.removePrefix("### ").trim())
                    }
                }
                trimmed.startsWith("## ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = h2Size, color = primaryAccent)) {
                        append(trimmed.removePrefix("## ").trim())
                    }
                }
                trimmed.startsWith("# ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = h1Size, color = primaryAccent)) {
                        append(trimmed.removePrefix("# ").trim())
                    }
                }
                trimmed.startsWith("> ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryAccent)) {
                        append("▎ ")
                    }
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        appendInlineFormatting(trimmed.removePrefix("> "), codeBg, compact)
                    }
                }
                (trimmed.startsWith("- ") || trimmed.startsWith("* ")) && indentCount >= 2 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = primaryAccent.copy(alpha = 0.85f))) {
                        append("   ◦ ")
                    }
                    appendInlineFormatting(trimmed.drop(2), codeBg, compact)
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryAccent)) {
                        append("• ")
                    }
                    appendInlineFormatting(trimmed.drop(2), codeBg, compact)
                }
                Regex("^\\d+\\.\\s+.*").matches(trimmed) -> {
                    val dotIdx = trimmed.indexOf('.')
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryAccent)) {
                        append(trimmed.substring(0, dotIdx + 2))
                    }
                    appendInlineFormatting(trimmed.substring(dotIdx + 2), codeBg, compact)
                }
                else -> appendInlineFormatting(originalLine, codeBg, compact)
            }
            if (index < lines.lastIndex) append('\n')
        }
    }
}

private fun AnnotatedString.Builder.appendInlineFormatting(
    text: String,
    codeBg: Color,
    compact: Boolean
) {
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
                        fontSize = if (compact) 11.sp else 13.sp
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
