package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.SavedProfileEntity
import com.example.data.local.toBackendTalent
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.ExactCategories
import com.example.data.model.ShareIdeaScope
import com.example.data.model.TalentProfile
import com.example.data.model.UiState
import com.example.data.model.VerifiedOrganization
import com.example.ui.components.BuilderConstellation3D
import com.example.ui.components.EcosystemSearchField
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
    onOpenMatches: () -> Unit = {},
    savedProfiles: List<SavedProfileEntity> = emptyList(),
    savedProfileIds: Set<String> = emptySet(),
    onToggleSaveTalent: (BackendTalent) -> Unit = {},
    onMessageTalent: (BackendTalent) -> Unit = {}
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showSavedProfilesOnly by remember { mutableStateOf(false) }
    var connectTarget by remember { mutableStateOf<BackendTalent?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedbackManager.current
    val subTabs = listOf("People", "India Directory", "Funding")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactMobile = maxWidth < 600.dp

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactMobile) 14.dp else 16.dp,
                        vertical = if (isCompactMobile) 4.dp else 10.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 4.dp else 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Ecosystem & Collaborators",
                            style = if (isCompactMobile) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    letterSpacing = (-0.2).sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Builders, verified Indian organizations, and funding programs.",
                            style = if (isCompactMobile) {
                                MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 14.5.sp
                                )
                            } else {
                                MaterialTheme.typography.bodySmall
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            onOpenMatches()
                        },
                        contentPadding = if (isCompactMobile) {
                            PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        } else {
                            PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                        },
                        modifier = Modifier
                            .heightIn(min = if (isCompactMobile) 42.dp else 48.dp)
                            .testTag("talent_open_matches_button")
                    ) {
                        Text("Match", maxLines = 1, softWrap = false)
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
                            modifier = Modifier
                                .heightIn(min = if (isCompactMobile) 38.dp else 48.dp)
                                .testTag("ecosystem_subtab_$idx")
                        ) {
                            Text(
                                text = label,
                                style = if (isCompactMobile) {
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 11.5.sp,
                                        letterSpacing = (-0.25).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    MaterialTheme.typography.labelLarge
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactMobile) 2.dp else 12.dp,
                                    vertical = if (isCompactMobile) 6.dp else 12.dp
                                )
                            )
                        }
                    }
                }
            }

            when (selectedSubTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (isCompactMobile) 14.dp else 16.dp,
                                end = if (isCompactMobile) 14.dp else 16.dp,
                                top = if (isCompactMobile) 4.dp else 0.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 6.dp else 8.dp)
                    ) {
                        EcosystemSearchField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholderText = "Search name, skill, role, or city...",
                            clearContentDescription = "Clear search",
                            compact = isCompactMobile,
                            modifier = Modifier.testTag("talent_search_input")
                        )

                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides if (isCompactMobile) 36.dp else 48.dp
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactMobile) 6.dp else 8.dp)
                            ) {
                                item(key = "saved_profiles_chip") {
                                    FilterChip(
                                        selected = showSavedProfilesOnly,
                                        onClick = {
                                            haptics.tap()
                                            showSavedProfilesOnly = !showSavedProfilesOnly
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (showSavedProfilesOnly) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = "Saved Profiles (${savedProfiles.size})",
                                                maxLines = 1,
                                                style = if (isCompactMobile) {
                                                    MaterialTheme.typography.labelMedium
                                                } else {
                                                    MaterialTheme.typography.labelLarge
                                                }
                                            )
                                        },
                                        modifier = Modifier
                                            .heightIn(min = if (isCompactMobile) 34.dp else 40.dp)
                                            .testTag("talent_saved_profiles_chip")
                                    )
                                }
                                items(ExactCategories.talentRoles) { role ->
                                    FilterChip(
                                        selected = !showSavedProfilesOnly && selectedRole == role,
                                        onClick = {
                                            haptics.tap()
                                            showSavedProfilesOnly = false
                                            onRoleChange(role)
                                        },
                                        label = {
                                            Text(
                                                text = role,
                                                maxLines = 1,
                                                style = if (isCompactMobile) {
                                                    MaterialTheme.typography.labelMedium
                                                } else {
                                                    MaterialTheme.typography.labelLarge
                                                }
                                            )
                                        },
                                        modifier = Modifier.heightIn(min = if (isCompactMobile) 34.dp else 40.dp)
                                    )
                                }
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
                    if (showSavedProfilesOnly) {
                        val savedAsTalents = remember(savedProfiles, searchQuery) {
                            val q = searchQuery.trim()
                            val mapped = savedProfiles.map { it.toBackendTalent() }
                            if (q.isBlank()) mapped
                            else mapped.filter { t ->
                                t.name.contains(q, ignoreCase = true) ||
                                    t.role.contains(q, ignoreCase = true) ||
                                    t.headline.contains(q, ignoreCase = true) ||
                                    t.location.contains(q, ignoreCase = true) ||
                                    t.skills.any { it.contains(q, ignoreCase = true) }
                            }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(
                                horizontal = if (isCompactMobile) 14.dp else 16.dp,
                                vertical = if (isCompactMobile) 8.dp else 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 8.dp else 12.dp),
                            modifier = Modifier.fillMaxSize().testTag("talent_saved_profiles_list")
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saved Profiles (${savedAsTalents.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Stored locally",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (savedAsTalents.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("talent_saved_profiles_empty")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(22.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.BookmarkBorder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Text(
                                                text = if (searchQuery.isBlank()) "No Saved Profiles yet" else "No saved profiles match your search",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Tap the bookmark icon on any collaborator card to save their profile locally for quick follow-up.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    haptics.tap()
                                                    searchQuery = ""
                                                    showSavedProfilesOnly = false
                                                },
                                                modifier = Modifier.heightIn(min = 44.dp)
                                            ) {
                                                Text("Browse All Collaborators")
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(savedAsTalents, key = { "saved_${it.id}" }) { talent ->
                                    TalentCardItem(
                                        talent = talent,
                                        isSaved = savedProfileIds.contains(talent.id),
                                        onToggleSave = {
                                            haptics.toggle(false)
                                            onToggleSaveTalent(talent)
                                        },
                                        onMessage = {
                                            haptics.tap()
                                            onMessageTalent(talent)
                                        },
                                        onConnect = {
                                            haptics.tap()
                                            connectTarget = talent
                                        }
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                    } else when (talentsState) {
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
                                contentPadding = PaddingValues(
                                    horizontal = if (isCompactMobile) 14.dp else 16.dp,
                                    vertical = if (isCompactMobile) 8.dp else 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(if (isCompactMobile) 8.dp else 12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "my_collaborator_profile") {
                                    MyCollaboratorProfileCard(
                                        profile = myTalentProfile,
                                        isBusy = isBusy,
                                        onToggleVisibility = onToggleTalentVisibility,
                                        onEditProfile = { showEditProfileDialog = true },
                                        compact = isCompactMobile
                                    )
                                }
                                if (filtered.isEmpty()) {
                                    item(key = "talents_3d_constellation") {
                                        BuilderConstellation3D(
                                            selectedRole = selectedRole,
                                            onCycleRole = {
                                                haptics.tap()
                                                val roles = ExactCategories.talentRoles
                                                val idx = roles.indexOf(selectedRole)
                                                onRoleChange(roles[(idx + 1) % roles.size])
                                            },
                                            height = if (isCompactMobile) 118.dp else 178.dp
                                        )
                                    }
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
                                    val firstTalent = filtered.first()
                                    item(key = firstTalent.id) {
                                        val isSaved = savedProfileIds.contains(firstTalent.id)
                                        TalentCardItem(
                                            talent = firstTalent,
                                            isSaved = isSaved,
                                            onToggleSave = {
                                                haptics.toggle(!isSaved)
                                                onToggleSaveTalent(firstTalent)
                                            },
                                            onMessage = {
                                                haptics.tap()
                                                onMessageTalent(firstTalent)
                                            },
                                            onConnect = {
                                                haptics.tap()
                                                connectTarget = firstTalent
                                            }
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
                                            },
                                            height = if (isCompactMobile) 118.dp else 178.dp
                                        )
                                    }
                                    items(filtered.drop(1), key = { it.id }) { talent ->
                                        val isSaved = savedProfileIds.contains(talent.id)
                                        TalentCardItem(
                                            talent = talent,
                                            isSaved = isSaved,
                                            onToggleSave = {
                                                haptics.toggle(!isSaved)
                                                onToggleSaveTalent(talent)
                                            },
                                            onMessage = {
                                                haptics.tap()
                                                onMessageTalent(talent)
                                            },
                                            onConnect = {
                                                haptics.tap()
                                                connectTarget = talent
                                            }
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(84.dp)) }
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
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onMessage: () -> Unit,
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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

                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("talent_bookmark_${talent.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) {
                            "Remove ${talent.name} from Saved Profiles"
                        } else {
                            "Save ${talent.name} to Saved Profiles"
                        },
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onMessage,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("talent_message_${talent.id}")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Message", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onConnect,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Proposal", maxLines = 1)
                    }
                }
            }
        }
    }
}

