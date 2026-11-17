package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.EcosystemMatchResult
import com.example.data.model.MatchTargetType
import com.example.data.model.ShareIdeaScope
import com.example.data.model.VerifiedOrganization
import com.example.ui.components.IndiaDirectoryDisclaimerCard
import com.example.ui.components.PrivacyProposalDialog
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAmber
import com.example.util.LocalHapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MatchesScreen(
    activeInnovation: BackendInnovation?,
    availableInnovations: List<BackendInnovation>,
    savedOpportunityIds: Set<String>,
    isBusy: Boolean,
    onComputeMatches: (
        title: String,
        description: String,
        category: String,
        stage: String,
        location: String
    ) -> List<EcosystemMatchResult>,
    onToggleSaveOrganization: (VerifiedOrganization) -> Unit,
    onSendCollaborationProposal: (
        recipient: BackendTalent,
        innovation: BackendInnovation?,
        proposalType: String,
        amount: String,
        message: String,
        contactInfo: String,
        shareScope: ShareIdeaScope,
        customProblemSummary: String,
        onDone: () -> Unit
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val haptics = LocalHapticFeedbackManager.current
    var ideaTitle by remember(activeInnovation?.id) {
        mutableStateOf(activeInnovation?.title.orEmpty())
    }
    var ideaCategory by remember(activeInnovation?.id) {
        mutableStateOf(activeInnovation?.category ?: "Green Tech")
    }
    var ideaStage by remember(activeInnovation?.id) {
        mutableStateOf(activeInnovation?.stage ?: "Prototype")
    }
    var preferredCity by remember { mutableStateOf("") }
    var ideaDescription by remember(activeInnovation?.id) {
        mutableStateOf(activeInnovation?.description.orEmpty())
    }

    var matches by remember(activeInnovation?.id) {
        mutableStateOf(
            if (activeInnovation != null) {
                onComputeMatches(
                    activeInnovation.title,
                    activeInnovation.description,
                    activeInnovation.category,
                    activeInnovation.stage,
                    ""
                )
            } else emptyList()
        )
    }

    var selectedTalentTarget by remember { mutableStateOf<BackendTalent?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ecosystem & Collaborator Matches") },
            navigationIcon = {
                IconButton(
                    onClick = {
                        haptics.tap()
                        onBack()
                    },
                    modifier = Modifier.testTag("matches_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().testTag("matches_results_list")
        ) {
            item {
                IndiaDirectoryDisclaimerCard(
                    text = "Possible Matches are rule-based suggestions using only verified directory entries and public collaborator profiles loaded in this app. Nothing is shared until you choose a privacy level and confirm outreach."
                )
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Match Against Your Idea",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = ideaTitle,
                            onValueChange = { ideaTitle = it },
                            label = { Text("Idea Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("matches_title_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = ideaCategory,
                                onValueChange = { ideaCategory = it },
                                label = { Text("Sector / Category") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = preferredCity,
                                onValueChange = { preferredCity = it },
                                label = { Text("Location (Optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = ideaDescription,
                            onValueChange = { ideaDescription = it },
                            label = { Text("Problem & Solution Keywords") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth().testTag("matches_desc_input")
                        )
                        Button(
                            onClick = {
                                haptics.success()
                                matches = onComputeMatches(
                                    ideaTitle,
                                    ideaDescription,
                                    ideaCategory,
                                    ideaStage,
                                    preferredCity
                                )
                            },
                            enabled = ideaTitle.isNotBlank() || ideaDescription.isNotBlank() || ideaCategory.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("find_matches_button")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Find Possible Matches")
                        }
                    }
                }
            }

            if (matches.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("No strong matches found in current records.", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Load additional verified Indian organizations under Talents → India Directory or refine your sector/keywords.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(matches, key = { res ->
                    res.organization?.id ?: res.talent?.id ?: res.supportingInfo.hashCode().toString()
                }) { match ->
                    MatchResultCard(
                        match = match,
                        isSavedOrg = match.organization?.let { savedOpportunityIds.contains(it.id) } ?: false,
                        onToggleSaveOrg = { org -> onToggleSaveOrganization(org) },
                        onConnectTalent = { talent -> selectedTalentTarget = talent }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    val connectTarget = selectedTalentTarget
    if (connectTarget != null) {
        PrivacyProposalDialog(
            targetTalent = connectTarget,
            availableInnovations = availableInnovations,
            preselectedInnovation = activeInnovation,
            isBusy = isBusy,
            onSend = { innovation, type, amount, msg, contact, scope, summary ->
                onSendCollaborationProposal(
                    connectTarget,
                    innovation,
                    type,
                    amount,
                    msg,
                    contact,
                    scope,
                    summary
                ) {
                    selectedTalentTarget = null
                }
            },
            onDismiss = { selectedTalentTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchResultCard(
    match: EcosystemMatchResult,
    isSavedOrg: Boolean,
    onToggleSaveOrg: (VerifiedOrganization) -> Unit,
    onConnectTalent: (BackendTalent) -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    val org = match.organization
    val talent = match.talent
    val title = org?.name ?: talent?.name ?: "Suggested Match"
    val isStale = org?.isVerificationMissingOrStale() ?: false

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (org != null) {
                        Icon(
                            imageVector = if (isStale) Icons.Filled.Warning else Icons.Filled.Verified,
                            contentDescription = if (isStale) "Verification review needed" else "Verified source record",
                            tint = if (isStale) GoldAmber else EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = match.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = match.supportingInfo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                match.matchReasons.forEach { reason ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (!match.sourceAttribution.isNullOrBlank()) {
                Text(
                    text = "Source: ${match.sourceAttribution}${match.lastVerifiedAt?.let { " · Verified $it" }.orEmpty()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (match.targetType == MatchTargetType.ORGANIZATION && org != null) {
                OutlinedButton(
                    onClick = {
                        haptics.toggle(!isSavedOrg)
                        onToggleSaveOrg(org)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = if (isSavedOrg) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isSavedOrg) "Saved to Opportunities" else "Save Opportunity")
                }
            } else if (match.targetType == MatchTargetType.TALENT && talent != null) {
                Button(
                    onClick = {
                        haptics.tap()
                        onConnectTalent(talent)
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Filled.Handshake, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect with Privacy Controls")
                }
            }
        }
    }
}
