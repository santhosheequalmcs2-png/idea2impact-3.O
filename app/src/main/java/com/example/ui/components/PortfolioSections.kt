package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.IdeaPortfolioEntity
import com.example.data.local.InvestorPortfolioEntity
import com.example.data.local.SavedProfileEntity
import com.example.data.local.skills
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.RoseError
import com.example.util.LocalHapticFeedbackManager

private val IDEA_PRIORITY_TAGS = listOf("High Priority", "Validating", "MVP Ready", "Archive")
private val INVESTOR_DILIGENCE_STATUSES = listOf("Watching", "In Diligence", "Term Sheet", "Committed", "Passed")
private val INVESTOR_FILTER_STATUSES = listOf("All") + INVESTOR_DILIGENCE_STATUSES

fun buildIdeaShareText(entry: IdeaPortfolioEntity): String = buildString {
    appendLine("${entry.title} (${entry.category} · ${entry.stage})")
    if (entry.priorityTag.isNotBlank()) {
        appendLine("Priority: ${entry.priorityTag}")
    }
    if (entry.creatorName.isNotBlank() && entry.creatorName != "You") {
        appendLine("Creator: ${entry.creatorName}")
    }
    if (entry.fundingGoal > 0.0) {
        appendLine("Funding Goal: ${formatCurrency(entry.fundingGoal)}")
    }
    if (entry.valuation > 0.0) {
        appendLine("Valuation: ${formatCurrency(entry.valuation)}")
    }
    if (entry.summary.isNotBlank()) {
        appendLine()
        appendLine(entry.summary.trim())
    }
    if (entry.validationNotes.isNotBlank()) {
        appendLine()
        appendLine("Validation Notes: ${entry.validationNotes.trim()}")
    }
    val cleanId = entry.innovationId?.trim().orEmpty()
    if (cleanId.isNotBlank()) {
        appendLine()
        append("https://idea2impact-yges.vercel.app/innovations/${Uri.encode(cleanId)}")
    } else {
        appendLine()
        append("Shared from IDEA2IMPACT Idea Portfolio")
    }
}.trim()

fun buildIdeaPortfolioExportText(items: List<IdeaPortfolioEntity>): String {
    if (items.isEmpty()) return ""
    if (items.size == 1) return buildIdeaShareText(items.first())
    return buildString {
        appendLine("IDEA2IMPACT · Idea Portfolio (${items.size} saved ideas)")
        appendLine()
        items.forEachIndexed { index, entry ->
            appendLine("${index + 1}. ${entry.title} (${entry.category} · ${entry.stage} · ${entry.priorityTag})")
            if (entry.fundingGoal > 0.0) {
                appendLine("   Funding Goal: ${formatCurrency(entry.fundingGoal)}")
            }
            if (entry.summary.isNotBlank()) {
                appendLine("   ${entry.summary.trim()}")
            }
            if (entry.validationNotes.isNotBlank()) {
                appendLine("   Notes: ${entry.validationNotes.trim()}")
            }
            val cleanId = entry.innovationId?.trim().orEmpty()
            if (cleanId.isNotBlank()) {
                appendLine("   Link: https://idea2impact-yges.vercel.app/innovations/${Uri.encode(cleanId)}")
            }
            if (index < items.lastIndex) {
                appendLine()
            }
        }
    }.trim()
}

fun createIdeaShareIntent(entry: IdeaPortfolioEntity): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, entry.title)
        putExtra(Intent.EXTRA_TITLE, "${entry.title} · IDEA2IMPACT")
        putExtra(Intent.EXTRA_TEXT, buildIdeaShareText(entry))
    }

fun createIdeaPortfolioExportIntent(items: List<IdeaPortfolioEntity>): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            if (items.size == 1) items.first().title else "IDEA2IMPACT Idea Portfolio (${items.size} ideas)"
        )
        putExtra(Intent.EXTRA_TITLE, "IDEA2IMPACT Idea Portfolio")
        putExtra(Intent.EXTRA_TEXT, buildIdeaPortfolioExportText(items))
    }

fun shareIdeaFromPortfolio(context: Context, entry: IdeaPortfolioEntity) {
    val chooser = Intent.createChooser(createIdeaShareIntent(entry), "Share Project Idea").apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(chooser) }
}

fun exportIdeaPortfolioToApps(context: Context, items: List<IdeaPortfolioEntity>) {
    if (items.isEmpty()) return
    val chooser = Intent.createChooser(
        createIdeaPortfolioExportIntent(items),
        if (items.size == 1) "Share Project Idea" else "Export Idea Portfolio (${items.size})"
    ).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(chooser) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdeaPortfolioSection(
    items: List<IdeaPortfolioEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAddIdea: (title: String, summary: String, category: String, stage: String, priorityTag: String, notes: String) -> Unit,
    onUpdateNotes: (id: String, priorityTag: String, notes: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onOpenOrAnalyze: (IdeaPortfolioEntity) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedbackManager.current
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<IdeaPortfolioEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("idea_portfolio_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Idea Portfolio (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved project ideas & validation notes stored locally on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(6.dp))
            if (items.isNotEmpty()) {
                IconButton(
                    onClick = {
                        haptics.tap()
                        exportIdeaPortfolioToApps(context, items)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("idea_portfolio_export_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Export Idea Portfolio to external apps",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Button(
                onClick = {
                    haptics.tap()
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("idea_portfolio_add_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Idea")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Idea Portfolio") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search saved ideas, categories, or notes...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("idea_portfolio_search_input")
        )

        if (items.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("idea_portfolio_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = GoldAmber,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "Your Idea Portfolio is empty" else "No matching saved ideas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Save project ideas from the Marketplace detail screen or add a new concept directly here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items.forEach { entry ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("idea_portfolio_card_${entry.id}")
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
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        haptics.tap()
                                        shareIdeaFromPortfolio(context, entry)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("idea_portfolio_share_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Share ${entry.title} to external apps",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptics.reject()
                                        onRemove(entry.id)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("idea_portfolio_remove_${entry.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Remove ${entry.title} from Idea Portfolio",
                                        tint = RoseError
                                    )
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PortfolioBadgePill(
                                text = entry.category,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PortfolioBadgePill(
                                text = entry.stage,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            PortfolioBadgePill(
                                text = entry.priorityTag,
                                containerColor = GoldAmber.copy(alpha = 0.16f),
                                contentColor = GoldAmber
                            )
                        }

                        Text(
                            text = entry.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (entry.validationNotes.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Notes: ${entry.validationNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    editingItem = entry
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("idea_portfolio_edit_${entry.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit Notes", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    haptics.tap()
                                    onOpenOrAnalyze(entry)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("idea_portfolio_open_${entry.id}")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (entry.innovationId != null) "Open" else "Ask AI", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddIdeaPortfolioDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, summary, category, stage, priority, notes ->
                onAddIdea(title, summary, category, stage, priority, notes)
                showAddDialog = false
            }
        )
    }

    val toEdit = editingItem
    if (toEdit != null) {
        EditIdeaPortfolioDialog(
            item = toEdit,
            onDismiss = { editingItem = null },
            onSave = { priority, notes ->
                onUpdateNotes(toEdit.id, priority, notes)
                editingItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestorPortfolioSection(
    items: List<InvestorPortfolioEntity>,
    searchQuery: String,
    statusFilter: String,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onAddDeal: (
        title: String,
        summary: String,
        category: String,
        stage: String,
        founderName: String,
        fundingGoal: Double,
        valuation: Double,
        diligenceStatus: String,
        targetTicketSize: String,
        notes: String
    ) -> Unit,
    onUpdateDiligence: (id: String, status: String, ticketSize: String, notes: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onOpenOrAnalyze: (InvestorPortfolioEntity) -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingDeal by remember { mutableStateOf<InvestorPortfolioEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("investor_portfolio_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Investor Portfolio (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saved project ideas tracked for diligence, ticket size & thesis notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    haptics.tap()
                    showAddDialog = true
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("investor_portfolio_add_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Deal")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Investor Portfolio") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search deals, sectors, or thesis notes...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("investor_portfolio_search_input")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(INVESTOR_FILTER_STATUSES) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = {
                        haptics.tap()
                        onStatusFilterChange(status)
                    },
                    label = { Text(status, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("investor_filter_$status")
                )
            }
        }

        if (items.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("investor_portfolio_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No projects in Investor Portfolio",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bookmark startups from project details or add a project deal to track diligence and target check sizes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items.forEach { deal ->
                val statusColor = when (deal.diligenceStatus) {
                    "Committed" -> EmeraldSuccess
                    "Term Sheet" -> MaterialTheme.colorScheme.primary
                    "In Diligence" -> GoldAmber
                    "Passed" -> RoseError
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("investor_portfolio_card_${deal.id}")
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deal.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Founder: ${deal.founderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptics.reject()
                                    onRemove(deal.id)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("investor_portfolio_remove_${deal.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Remove ${deal.title} from Investor Portfolio",
                                    tint = RoseError
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PortfolioBadgePill(
                                text = deal.diligenceStatus,
                                containerColor = statusColor.copy(alpha = 0.16f),
                                contentColor = statusColor
                            )
                            PortfolioBadgePill(
                                text = deal.category,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PortfolioBadgePill(
                                text = deal.stage,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (deal.targetTicketSize.isNotBlank()) {
                                PortfolioBadgePill(
                                    text = "Ticket: ${deal.targetTicketSize}",
                                    containerColor = EmeraldSuccess.copy(alpha = 0.14f),
                                    contentColor = EmeraldSuccess
                                )
                            }
                        }

                        if (deal.fundingGoal > 0.0 || deal.valuation > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (deal.fundingGoal > 0.0) {
                                    Text(
                                        text = "Goal: ${formatCurrency(deal.fundingGoal)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (deal.valuation > 0.0) {
                                    Text(
                                        text = "Valuation: ${formatCurrency(deal.valuation)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Text(
                            text = deal.summary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (deal.investmentThesisNotes.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Thesis: ${deal.investmentThesisNotes}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    editingDeal = deal
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("investor_portfolio_edit_${deal.id}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Diligence", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    haptics.tap()
                                    onOpenOrAnalyze(deal)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("investor_portfolio_open_${deal.id}")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (deal.innovationId != null) "Open" else "Ask AI", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInvestorDealDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, summary, category, stage, founder, goal, valEst, status, ticket, notes ->
                onAddDeal(title, summary, category, stage, founder, goal, valEst, status, ticket, notes)
                showAddDialog = false
            }
        )
    }

    val toEdit = editingDeal
    if (toEdit != null) {
        EditInvestorDiligenceDialog(
            item = toEdit,
            onDismiss = { editingDeal = null },
            onSave = { status, ticket, notes ->
                onUpdateDiligence(toEdit.id, status, ticket, notes)
                editingDeal = null
            }
        )
    }
}

@Composable
private fun PortfolioBadgePill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AddIdeaPortfolioDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, summary: String, category: String, stage: String, priorityTag: String, notes: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("ClimateTech") }
    var stage by rememberSaveable { mutableStateOf("Concept") }
    var priority by rememberSaveable { mutableStateOf("High Priority") }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to Idea Portfolio", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Idea Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_idea_title_input")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Problem & Solution Summary") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Sector") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stage,
                        onValueChange = { stage = it },
                        label = { Text("Stage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(IDEA_PRIORITY_TAGS) { tag ->
                        FilterChip(
                            selected = priority == tag,
                            onClick = { priority = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Validation Notes (Optional)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, summary, category, stage, priority, notes) },
                enabled = title.trim().isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("add_idea_confirm_button")
            ) {
                Text("Save Idea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditIdeaPortfolioDialog(
    item: IdeaPortfolioEntity,
    onDismiss: () -> Unit,
    onSave: (priorityTag: String, notes: String) -> Unit
) {
    var priority by rememberSaveable(item.id) { mutableStateOf(item.priorityTag) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.validationNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Idea Notes", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(IDEA_PRIORITY_TAGS) { tag ->
                        FilterChip(
                            selected = priority == tag,
                            onClick = { priority = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Validation Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(priority, notes) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddInvestorDealDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        summary: String,
        category: String,
        stage: String,
        founderName: String,
        fundingGoal: Double,
        valuation: Double,
        diligenceStatus: String,
        targetTicketSize: String,
        notes: String
    ) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var founder by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("DeepTech") }
    var stage by rememberSaveable { mutableStateOf("Seed") }
    var ticket by rememberSaveable { mutableStateOf("") }
    var goalText by rememberSaveable { mutableStateOf("") }
    var valuationText by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Watching") }
    var notes by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Investor Portfolio", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Startup / Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_investor_deal_title_input")
                )
                OutlinedTextField(
                    value = founder,
                    onValueChange = { founder = it },
                    label = { Text("Founder / Team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Product & Traction Summary") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Sector") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stage,
                        onValueChange = { stage = it },
                        label = { Text("Round Stage") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ticket,
                        onValueChange = { ticket = it },
                        label = { Text("Target Ticket") },
                        placeholder = { Text("$50K") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it },
                        label = { Text("Funding Goal ($)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = valuationText,
                    onValueChange = { valuationText = it },
                    label = { Text("Estimated Valuation ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Diligence Status", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(INVESTOR_DILIGENCE_STATUSES) { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Investment Thesis & Risks") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        summary,
                        category,
                        stage,
                        founder,
                        goalText.trim().toDoubleOrNull() ?: 0.0,
                        valuationText.trim().toDoubleOrNull() ?: 0.0,
                        status,
                        ticket,
                        notes
                    )
                },
                enabled = title.trim().isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("add_investor_deal_confirm_button")
            ) {
                Text("Save Deal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditInvestorDiligenceDialog(
    item: InvestorPortfolioEntity,
    onDismiss: () -> Unit,
    onSave: (status: String, ticketSize: String, notes: String) -> Unit
) {
    var status by rememberSaveable(item.id) { mutableStateOf(item.diligenceStatus) }
    var ticket by rememberSaveable(item.id) { mutableStateOf(item.targetTicketSize) }
    var notes by rememberSaveable(item.id) { mutableStateOf(item.investmentThesisNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Diligence", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(INVESTOR_DILIGENCE_STATUSES) { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = ticket,
                    onValueChange = { ticket = it },
                    label = { Text("Target Ticket Size") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Investment Thesis Notes") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(status, ticket, notes) },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavedProfilesSection(
    items: List<SavedProfileEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRemove: (id: String) -> Unit,
    onBrowseCollaborators: () -> Unit,
    onMessageProfile: (SavedProfileEntity) -> Unit = {}
) {
    val haptics = LocalHapticFeedbackManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_profiles_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Saved Profiles (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bookmarked collaborator profiles stored locally on your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    haptics.tap()
                    onBrowseCollaborators()
                },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("saved_profiles_browse_button")
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Explore", maxLines = 1)
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Saved Profiles") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            placeholder = { Text("Search saved profiles, skills, or cities...") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("saved_profiles_search_input")
        )

        if (items.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_profiles_empty_state")
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) "No Saved Profiles yet" else "No matching saved profiles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bookmark collaborator profiles in Ecosystem & Collaborators to keep your shortlist ready offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items.forEach { profile ->
                val profileSkills = profile.skills
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_profile_card_${profile.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!profile.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = profile.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = profile.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (profile.verified) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "Verified Specialist",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = profile.role,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptics.reject()
                                    onRemove(profile.id)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("saved_profile_remove_${profile.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "Remove ${profile.name} from Saved Profiles",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (profile.headline.isNotBlank() && profile.headline != profile.role) {
                            Text(
                                text = profile.headline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (profileSkills.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                profileSkills.take(6).forEach { skill ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = skill,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${profile.location} · ${profile.roleCategory}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    haptics.tap()
                                    onMessageProfile(profile)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("saved_profile_message_${profile.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Message", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
