package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.ExactCategories
import com.example.data.model.InviteCollaboratorTarget
import com.example.data.model.StartCollaborationDraft
import com.example.util.LocalHapticFeedbackManager

private val COLLAB_TIMELINES = listOf("2 Weeks", "4 Weeks", "8 Weeks", "12 Weeks", "6 Months")
private val COLLAB_ROLES = listOf("Co-Founder", "Lead Engineer", "Product Designer", "AI Architect", "Investor Advisor", "Patent Counsel")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartCollaborationSheet(
    sourceConversationId: String? = null,
    preselectedPeerId: String? = null,
    preselectedPeerName: String? = null,
    preselectedInnovation: BackendInnovation? = null,
    availableInnovations: List<BackendInnovation>,
    availableTalents: List<BackendTalent>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onLaunchCollaboration: (StartCollaborationDraft) -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by rememberSaveable { mutableIntStateOf(1) }

    var title by rememberSaveable {
        mutableStateOf(
            if (preselectedInnovation != null) "${preselectedInnovation.title} · Build Sprint"
            else if (!preselectedPeerName.isNullOrBlank()) "Collaboration with $preselectedPeerName"
            else ""
        )
    }
    var description by rememberSaveable {
        mutableStateOf(preselectedInnovation?.description?.take(400).orEmpty())
    }
    var selectedInnovationId by rememberSaveable { mutableStateOf(preselectedInnovation?.id) }
    var category by rememberSaveable {
        mutableStateOf(preselectedInnovation?.category ?: ExactCategories.defaultItems.first().name)
    }
    var skillsText by rememberSaveable {
        mutableStateOf("Kotlin, Product Design, Hardware Prototyping")
    }
    var objectives by rememberSaveable {
        mutableStateOf("Validate core architecture, ship working prototype, and prepare demo milestones.")
    }
    var deliverables by rememberSaveable {
        mutableStateOf("Functional MVP build, technical documentation, and milestone review.")
    }
    var timeline by rememberSaveable { mutableStateOf("4 Weeks") }
    var referenceLink by rememberSaveable { mutableStateOf("") }
    var invitationMessage by rememberSaveable {
        mutableStateOf("Let's convert our discussion into a structured IDEA2IMPACT collaboration workspace!")
    }

    val selectedPeerRoles = remember {
        mutableStateMapOf<String, String>().apply {
            if (!preselectedPeerId.isNullOrBlank() && !preselectedPeerId.startsWith("t_") && !preselectedPeerId.startsWith("seed_")) {
                put(preselectedPeerId, "Co-Founder")
            }
        }
    }

    val selectedInnovation = remember(selectedInnovationId, availableInnovations) {
        availableInnovations.firstOrNull { it.id == selectedInnovationId }
    }

    val invitableTalents = remember(availableTalents) {
        availableTalents.filter { !it.id.startsWith("t_") && !it.id.startsWith("seed_") }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("start_collaboration_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (step == 1) "Start Collaboration · Step 1 of 2" else "Invite Collaborators · Step 2 of 2",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (step == 1) "Define project goals, deliverables, and timeline."
                        else "Select team members and assign their collaboration roles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close collaboration wizard")
                }
            }

            if (step == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Collaboration / Project Title *") },
                        placeholder = { Text("e.g., Autonomous Grid Controller MVP") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collab_wizard_title_input")
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Project Description *") },
                        placeholder = { Text("Describe what you are building together...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("collab_wizard_desc_input")
                    )

                    Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ExactCategories.defaultItems.map { it.name }) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    haptics.tap()
                                    category = cat
                                },
                                label = { Text(cat) }
                            )
                        }
                    }

                    if (availableInnovations.isNotEmpty()) {
                        Text("Related Innovation Listing (Optional)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedInnovationId == null,
                                    onClick = { selectedInnovationId = null },
                                    label = { Text("None") }
                                )
                            }
                            items(availableInnovations.take(10), key = { it.id }) { inn ->
                                FilterChip(
                                    selected = selectedInnovationId == inn.id,
                                    onClick = {
                                        haptics.tap()
                                        selectedInnovationId = inn.id
                                        if (category.isBlank()) category = inn.category
                                    },
                                    label = { Text(inn.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = skillsText,
                        onValueChange = { skillsText = it },
                        label = { Text("Required Skills (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = objectives,
                        onValueChange = { objectives = it },
                        label = { Text("Project Objectives") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deliverables,
                        onValueChange = { deliverables = it },
                        label = { Text("Expected Deliverables") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Proposed Timeline", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        COLLAB_TIMELINES.forEach { t ->
                            FilterChip(
                                selected = timeline == t,
                                onClick = { timeline = t },
                                label = { Text(t) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = referenceLink,
                        onValueChange = { referenceLink = it },
                        label = { Text("Optional Reference Link / Spec URL") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        haptics.tap()
                        step = 2
                    },
                    enabled = title.trim().length >= 3 && description.trim().length >= 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("collab_wizard_next_step")
                ) {
                    Text("Next: Invite Collaborators")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = invitationMessage,
                        onValueChange = { invitationMessage = it },
                        label = { Text("Invitation Message") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Select Collaborators (${selectedPeerRoles.size} selected)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (invitableTalents.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Launch workspace now & invite anytime",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Other signed-in IDEA2IMPACT collaborators will appear here automatically. You can launch your private workspace immediately and invite collaborators later from Team.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(invitableTalents, key = { it.id }) { talent ->
                                val isSelected = selectedPeerRoles.containsKey(talent.id)
                                val assignedRole = selectedPeerRoles[talent.id] ?: "Collaborator"
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.tap()
                                            if (isSelected) selectedPeerRoles.remove(talent.id)
                                            else selectedPeerRoles[talent.id] = "Co-Founder"
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedPeerRoles[talent.id] = "Co-Founder"
                                                    else selectedPeerRoles.remove(talent.id)
                                                }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    talent.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "${talent.role} · ${talent.location}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                                            ) {
                                                items(COLLAB_ROLES) { roleOpt ->
                                                    FilterChip(
                                                        selected = assignedRole == roleOpt,
                                                        onClick = { selectedPeerRoles[talent.id] = roleOpt },
                                                        label = { Text(roleOpt, style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                    Button(
                        onClick = {
                            haptics.success()
                            val invitees = selectedPeerRoles.mapNotNull { (uid, role) ->
                                val peer = invitableTalents.firstOrNull { it.id == uid }
                                val peerName = peer?.name ?: preselectedPeerName?.takeIf { uid == preselectedPeerId } ?: "Collaborator"
                                InviteCollaboratorTarget(userId = uid, name = peerName, proposedRole = role)
                            }
                            onLaunchCollaboration(
                                StartCollaborationDraft(
                                    sourceConversationId = sourceConversationId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    relatedInnovationId = selectedInnovation?.id,
                                    relatedInnovationTitle = selectedInnovation?.title,
                                    category = category,
                                    requiredSkills = skillsText.split(',').map { it.trim() }.filter { it.isNotBlank() },
                                    objectives = objectives.trim(),
                                    expectedDeliverables = deliverables.trim(),
                                    proposedTimeline = timeline,
                                    referenceLinks = listOfNotNull(referenceLink.trim().takeIf { it.isNotBlank() }),
                                    invitedPeers = invitees,
                                    invitationMessage = invitationMessage.trim()
                                )
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 50.dp)
                            .testTag("collab_wizard_launch_button")
                    ) {
                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isBusy) "Creating…" else "Launch Workspace")
                    }
                }
            }
        }
    }
}
