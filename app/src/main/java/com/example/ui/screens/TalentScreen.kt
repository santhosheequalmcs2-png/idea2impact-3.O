package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.ExactCategories
import com.example.data.model.ShareIdeaScope
import com.example.data.model.TalentProfile
import com.example.data.model.UiState
import com.example.data.model.VerifiedOrganization
import com.example.ui.components.BuilderConstellation3D
import com.example.ui.components.EditCollaboratorProfileDialog
import com.example.ui.components.FundingOpportunitiesPanel
import com.example.ui.components.ImportVerifiedJsonDialog
import com.example.ui.components.MyCollaboratorProfileCard
import com.example.ui.components.PrivacyProposalDialog
import com.example.ui.components.TalentSkeletonList
import com.example.ui.components.VerifiedDirectoryPanel
import com.example.util.LocalHapticFeedbackManager

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TalentScreen(
    talentsState: UiState<List<BackendTalent>>,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    onRetry: () -> Unit,
    onSendConnectMessage: (talent: BackendTalent, message: String) -> Unit = { _, _ -> },
    myTalentProfile: TalentProfile? = null,
    availableInnovations: List<BackendInnovation> = emptyList(),
    verifiedOrganizations: List<VerifiedOrganization> = emptyList(),
    savedOpportunityIds: Set<String> = emptySet(),
    isBusy: Boolean = false,
    onToggleTalentVisibility: (Boolean) -> Unit = {},
    onSaveMyTalentProfile: (
        name: String,
        role: String,
        headline: String,
        bio: String,
        skills: List<String>,
        location: String,
        isPublic: Boolean,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
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
    ) -> Unit = { talent, _, _, _, msg, _, _, _, onDone ->
        onSendConnectMessage(talent, msg)
        onDone()
    },
    onImportVerifiedJson: (String) -> Result<Int> = { Result.success(0) },
    onClearImportedDirectory: () -> Unit = {},
    onToggleSaveOrganization: (VerifiedOrganization) -> Unit = {},
    onOpenMatches: () -> Unit = {}
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var connectTarget by remember { mutableStateOf<BackendTalent?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedbackManager.current
    val subTabs = listOf("People", "India Directory", "Funding")

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "Ecosystem & Collaborators",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Builders, verified Indian organizations, and funding programs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onOpenMatches()
                    },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("talent_open_matches_button")
                ) {
                    Text("Match")
                }
            }

            SecondaryTabRow(selectedTabIndex = selectedSubTab) {
                subTabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedSubTab == idx,
                        onClick = {
                            haptics.tap()
                            selectedSubTab = idx
                        },
                        text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.testTag("ecosystem_subtab_$idx")
                    )
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search name, skill, role, or city...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        haptics.tap()
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("talent_search_input")
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ExactCategories.talentRoles) { role ->
                            FilterChip(
                                selected = selectedRole == role,
                                onClick = {
                                    haptics.tap()
                                    onRoleChange(role)
                                },
                                label = { Text(role) },
                                modifier = Modifier.heightIn(min = 40.dp)
                            )
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = {
                        haptics.success()
                        onRetry()
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    when (talentsState) {
                        UiState.Loading -> TalentSkeletonList()
                        is UiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        talentsState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Button(
                                        onClick = {
                                            haptics.tap()
                                            onRetry()
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val filtered = remember(talentsState.data, selectedRole, searchQuery) {
                                val byRole = if (selectedRole == "All") talentsState.data
                                else talentsState.data.filter { it.roleCategory.equals(selectedRole, ignoreCase = true) }
                                val q = searchQuery.trim()
                                if (q.isBlank()) byRole
                                else byRole.filter { t ->
                                    t.name.contains(q, ignoreCase = true) ||
                                        t.role.contains(q, ignoreCase = true) ||
                                        t.headline.contains(q, ignoreCase = true) ||
                                        t.location.contains(q, ignoreCase = true) ||
                                        t.skills.any { it.contains(q, ignoreCase = true) }
                                }
                            }

                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "my_collaborator_profile") {
                                    MyCollaboratorProfileCard(
                                        profile = myTalentProfile,
                                        isBusy = isBusy,
                                        onToggleVisibility = onToggleTalentVisibility,
                                        onEditProfile = { showEditProfileDialog = true }
                                    )
                                }
                                item(key = "talents_3d_constellation") {
                                    BuilderConstellation3D(
                                        selectedRole = selectedRole,
                                        onCycleRole = {
                                            haptics.tap()
                                            val roles = ExactCategories.talentRoles
                                            val idx = roles.indexOf(selectedRole)
                                            onRoleChange(roles[(idx + 1) % roles.size])
                                        }
                                    )
                                }
                                if (filtered.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "No specialists matched your current filter.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (searchQuery.isNotBlank() || selectedRole != "All") {
                                                    OutlinedButton(
                                                        onClick = {
                                                            haptics.tap()
                                                            searchQuery = ""
                                                            onRoleChange("All")
                                                        },
                                                        modifier = Modifier.heightIn(min = 48.dp)
                                                    ) {
                                                        Text("Reset Filters")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(filtered, key = { it.id }) { talent ->
                                        TalentCardItem(
                                            talent = talent,
                                            onConnect = {
                                                haptics.tap()
                                                connectTarget = talent
                                            }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(12.dp)) }
                            }
                        }
                    }
                }
            }
            1 -> {
                VerifiedDirectoryPanel(
                    organizations = verifiedOrganizations,
                    savedIds = savedOpportunityIds,
                    onToggleSave = onToggleSaveOrganization,
                    onOpenImportDialog = { showImportJsonDialog = true },
                    onClearImported = onClearImportedDirectory,
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                FundingOpportunitiesPanel(
                    organizations = verifiedOrganizations,
                    savedIds = savedOpportunityIds,
                    onToggleSave = onToggleSaveOrganization,
                    onOpenImportDialog = { showImportJsonDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    val target = connectTarget
    if (target != null) {
        PrivacyProposalDialog(
            targetTalent = target,
            availableInnovations = availableInnovations,
            isBusy = isBusy,
            onSend = { innovation, type, amount, message, contact, scope, summary ->
                onSendCollaborationProposal(
                    target,
                    innovation,
                    type,
                    amount,
                    message,
                    contact,
                    scope,
                    summary
                ) {
                    connectTarget = null
                }
            },
            onDismiss = { connectTarget = null }
        )
    }

    if (showEditProfileDialog) {
        EditCollaboratorProfileDialog(
            initialProfile = myTalentProfile,
            isBusy = isBusy,
            onSave = { name, role, headline, bio, skills, location, isPublic ->
                onSaveMyTalentProfile(name, role, headline, bio, skills, location, isPublic) {
                    showEditProfileDialog = false
                }
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showImportJsonDialog) {
        ImportVerifiedJsonDialog(
            onImport = onImportVerifiedJson,
            onDismiss = { showImportJsonDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TalentCardItem(
    talent: BackendTalent,
    onConnect: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("talent_card_${talent.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!talent.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = talent.avatarUrl,
                        contentDescription = talent.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                talent.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            talent.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (talent.verified) {
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = "Verified Specialist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        talent.role,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (talent.headline.isNotBlank() && talent.headline != talent.role) {
                Text(
                    talent.headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (talent.skills.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    talent.skills.take(6).forEach { skill ->
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        talent.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onConnect,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Connect / Proposal")
                }
            }
        }
    }
}

