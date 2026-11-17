package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.ExactCategories
import com.example.data.model.ShareIdeaScope
import com.example.data.model.TalentProfile
import com.example.data.model.VerifiedDirectoryJsonParser
import com.example.util.LocalHapticFeedbackManager

@Composable
fun ImportVerifiedJsonDialog(
    onImport: (String) -> Result<Int>,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    var rawJson by remember { mutableStateOf("") }
    var showSchema by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Verified Directory JSON") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Each entry must include name, organizationType, sector, sourceName, valid sourceUrl, and YYYY-MM-DD lastVerifiedAt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        showSchema = !showSchema
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text(if (showSchema) "Hide Schema Template" else "View Required JSON Schema")
                }
                if (showSchema) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = VerifiedDirectoryJsonParser.SCHEMA_TEMPLATE_JSON,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = rawJson,
                    onValueChange = {
                        rawJson = it
                        errorMsg = null
                    },
                    label = { Text("Verified JSON Array") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("import_verified_json_input")
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val res = onImport(rawJson)
                    res.onSuccess {
                        haptics.success()
                        onDismiss()
                    }.onFailure { err ->
                        haptics.reject()
                        errorMsg = err.localizedMessage ?: "Invalid directory JSON."
                    }
                },
                enabled = rawJson.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp).testTag("confirm_import_directory_button")
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptics.tap()
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCollaboratorProfileDialog(
    initialProfile: TalentProfile?,
    isBusy: Boolean,
    onSave: (
        name: String,
        role: String,
        headline: String,
        bio: String,
        skills: List<String>,
        location: String,
        isPublic: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    val roleOptions = listOf("Creator", "Builder", "Investor", "Patent Professional")
    var roleExpanded by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(initialProfile?.name.orEmpty()) }
    var role by remember { mutableStateOf(initialProfile?.role ?: "Builder") }
    var headline by remember { mutableStateOf(initialProfile?.headline.orEmpty()) }
    var bio by remember { mutableStateOf(initialProfile?.bio.orEmpty()) }
    var skillsCsv by remember { mutableStateOf(initialProfile?.skills?.joinToString(", ").orEmpty()) }
    var location by remember { mutableStateOf(initialProfile?.location ?: "India") }
    var isPublic by remember { mutableStateOf(initialProfile?.isPublic ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Collaborator Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    OutlinedTextField(
                        value = role,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Role") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false }
                    ) {
                        roleOptions.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    role = r
                                    roleExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = headline,
                    onValueChange = { headline = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = skillsCsv,
                    onValueChange = { skillsCsv = it },
                    label = { Text("Skills (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("City / Country") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Collaboration Focus & Bio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Discoverable in People tab", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.success()
                    val skills = skillsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(name, role, headline, bio, skills, location, isPublic)
                },
                enabled = !isBusy && name.isNotBlank(),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyProposalDialog(
    targetTalent: BackendTalent,
    availableInnovations: List<BackendInnovation>,
    preselectedInnovation: BackendInnovation? = null,
    isBusy: Boolean,
    onSend: (
        innovation: BackendInnovation?,
        proposalType: String,
        amount: String,
        message: String,
        contactInfo: String,
        shareScope: ShareIdeaScope,
        customProblemSummary: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    val proposalTypes = ExactCategories.proposalTypes + listOf("Co-founder", "Engineering")
    var typeExpanded by remember { mutableStateOf(false) }
    var innExpanded by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf("Co-founder") }
    var selectedInnovation by remember { mutableStateOf(preselectedInnovation ?: availableInnovations.firstOrNull()) }
    var shareScope by remember { mutableStateOf(ShareIdeaScope.NONE) }
    var customProblemSummary by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect with ${targetTalent.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${targetTalent.role} · ${targetTalent.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Collaboration Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        proposalTypes.distinct().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                if (availableInnovations.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = innExpanded,
                        onExpandedChange = { innExpanded = !innExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedInnovation?.title ?: "None selected",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Referenced Idea (Optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(innExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = innExpanded,
                            onDismissRequest = { innExpanded = false }
                        ) {
                            availableInnovations.take(10).forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.title) },
                                    onClick = {
                                        selectedInnovation = item
                                        innExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Idea Privacy & Sharing Gate",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        ShareIdeaScope.entries.forEach { scopeOption ->
                            val enabledForInn = scopeOption == ShareIdeaScope.NONE || selectedInnovation != null
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = shareScope == scopeOption,
                                    enabled = enabledForInn,
                                    onClick = {
                                        haptics.tap()
                                        shareScope = scopeOption
                                    },
                                    modifier = Modifier.testTag("share_scope_${scopeOption.name}")
                                )
                                Spacer(Modifier.width(4.dp))
                                Column {
                                    Text(scopeOption.label, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        scopeOption.helperText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (shareScope == ShareIdeaScope.PROBLEM_SUMMARY) {
                    OutlinedTextField(
                        value = customProblemSummary,
                        onValueChange = { customProblemSummary = it },
                        label = { Text("Problem Summary Only (Optional)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Introduction / Proposal Note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("privacy_proposal_message")
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Terms / Grant / Equity (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactInfo,
                    onValueChange = { contactInfo = it },
                    label = { Text("Preferred Contact (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.success()
                    onSend(
                        selectedInnovation,
                        selectedType,
                        amount,
                        message,
                        contactInfo,
                        shareScope,
                        customProblemSummary
                    )
                },
                enabled = !isBusy && message.trim().length >= 5,
                modifier = Modifier.heightIn(min = 48.dp).testTag("privacy_proposal_submit")
            ) {
                Text("Confirm & Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}
