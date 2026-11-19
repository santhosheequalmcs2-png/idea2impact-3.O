package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EcosystemTaxonomy
import com.example.data.model.TalentProfile
import com.example.data.model.VerifiedOrganization
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAmber
import com.example.util.LocalHapticFeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcosystemSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    clearContentDescription: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (!compact) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholderText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = clearContentDescription)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier.fillMaxWidth()
        )
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            interactionSource = interactionSource,
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (value.isNotBlank()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = clearContentDescription,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = OutlinedTextFieldDefaults.colors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun IndiaDirectoryDisclaimerCard(
    text: String = "Coverage note: This directory contains only curated or imported records with explicit source attribution and verification dates. It does not include every startup, investor, or program in India.",
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Directory coverage information",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCollaboratorProfileCard(
    profile: TalentProfile?,
    isBusy: Boolean,
    onToggleVisibility: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val haptics = LocalHapticFeedbackManager.current
    val isPublic = profile?.isPublic ?: false

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
        modifier = modifier.fillMaxWidth().testTag("my_collaborator_profile_card")
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = profile?.name?.takeIf { it.isNotBlank() } ?: "My Collaborator Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (profile != null) {
                            "${profile.role} · ${profile.location.ifBlank { "Location not set" }}"
                        } else {
                            "Sign in with Google to publish your builder/founder profile"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onEditProfile()
                    },
                    contentPadding = if (compact) PaddingValues(horizontal = 12.dp, vertical = 6.dp) else PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = if (compact) 40.dp else 48.dp)
                        .testTag("edit_my_talent_profile_button")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = if (isPublic) "Publicly Discoverable" else "Hidden from Public Discovery",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPublic) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Control whether other members can find your profile and send collaboration requests.",
                        style = if (compact) MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp) else MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(
                    checked = isPublic,
                    enabled = !isBusy,
                    onCheckedChange = { next ->
                        haptics.toggle(next)
                        onToggleVisibility(next)
                    },
                    modifier = Modifier.testTag("talent_visibility_switch")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerifiedOrganizationCard(
    org: VerifiedOrganization,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    showFundingDetails: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedbackManager.current
    val uriHandler = LocalUriHandler.current
    val isStale = remember(org.verificationStatus, org.sourceUrl, org.lastVerifiedAt) {
        org.isVerificationMissingOrStale()
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth().testTag("verified_org_card_${org.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = org.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = if (isStale) Icons.Filled.Warning else Icons.Filled.Verified,
                            contentDescription = if (isStale) "Verification needs review" else "Source verified",
                            tint = if (isStale) GoldAmber else EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = org.organizationType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (org.sector.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = org.sector,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Surface(
                            color = (if (isStale) GoldAmber else EmeraldSuccess).copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isStale) "Needs Review / Stale" else "Verified ${org.lastVerifiedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStale) GoldAmber else EmeraldSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        haptics.toggle(!isSaved)
                        onToggleSave()
                    },
                    modifier = Modifier.size(48.dp).testTag("save_org_${org.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove saved opportunity" else "Save opportunity",
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (org.description.isNotBlank()) {
                Text(
                    text = org.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = listOfNotNull(
                        org.location.takeIf { it.isNotBlank() },
                        org.stageOrFocus.takeIf { it.isNotBlank() }?.let { "Focus: $it" }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (org.publicOpportunities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    org.publicOpportunities.take(4).forEach { opp ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = opp,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (showFundingDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Eligibility: ${org.eligibility?.takeIf { it.isNotBlank() } ?: "Not specified in source record"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Geography: ${org.geography?.takeIf { it.isNotBlank() } ?: org.location.ifBlank { "India" }} · Deadline: ${org.deadline?.takeIf { it.isNotBlank() } ?: "Deadline unverified"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Source: ${org.sourceName.ifBlank { "Unattributed" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (org.sourceUrl.startsWith("http://") || org.sourceUrl.startsWith("https://")) {
                    TextButton(
                        onClick = {
                            haptics.tap()
                            runCatching { uriHandler.openUri(org.sourceUrl) }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Open Source", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open verification source", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VerifiedDirectoryPanel(
    organizations: List<VerifiedOrganization>,
    savedIds: Set<String>,
    onToggleSave: (VerifiedOrganization) -> Unit,
    onOpenImportDialog: () -> Unit,
    onClearImported: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedbackManager.current
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }

    val filtered = remember(organizations, query, selectedType) {
        organizations.filter { org ->
            val typeOk = selectedType == "All" || org.organizationType.equals(selectedType, ignoreCase = true)
            val q = query.trim()
            val queryOk = q.isBlank() ||
                org.name.contains(q, ignoreCase = true) ||
                org.sector.contains(q, ignoreCase = true) ||
                org.location.contains(q, ignoreCase = true) ||
                org.stageOrFocus.contains(q, ignoreCase = true) ||
                org.description.contains(q, ignoreCase = true)
            typeOk && queryOk
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = if (compact) 14.dp else 16.dp,
                vertical = if (compact) 8.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            modifier = Modifier.fillMaxSize().testTag("india_directory_list")
        ) {
            item {
                IndiaDirectoryDisclaimerCard()
            }

            item {
                EcosystemSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholderText = "Search name, sector, city, focus...",
                    clearContentDescription = "Clear directory search",
                    compact = compact,
                    modifier = Modifier.testTag("directory_search_input")
                )
            }

            item {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides if (compact) 36.dp else 48.dp
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                        items(EcosystemTaxonomy.organizationTypes) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    haptics.tap()
                                    selectedType = type
                                },
                                label = { Text(type, maxLines = 1) },
                                modifier = Modifier.heightIn(min = if (compact) 34.dp else 48.dp)
                            )
                        }
                    }
                }
            }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptics.tap()
                        onOpenImportDialog()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("open_import_directory_button")
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Verified JSON")
                }
                if (organizations.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            onClearImported()
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("clear_imported_directory_button")
                    ) {
                        Text("Clear Local")
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (organizations.isEmpty()) {
                                "No verified Indian directory entries loaded yet."
                            } else {
                                "No verified entries matched your search filters."
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "IDEA2IMPACT never fabricates startups, investors, or grants. Tap \"Import Verified JSON\" above to load your verified source records or sync admin-curated records from Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id.ifBlank { it.name } }) { org ->
                VerifiedOrganizationCard(
                    org = org,
                    isSaved = savedIds.contains(org.id),
                    onToggleSave = { onToggleSave(org) }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
    }
}

@Composable
fun FundingOpportunitiesPanel(
    organizations: List<VerifiedOrganization>,
    savedIds: Set<String>,
    onToggleSave: (VerifiedOrganization) -> Unit,
    onOpenImportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedbackManager.current
    var selectedType by remember { mutableStateOf("All Funding") }
    var savedOnly by remember { mutableStateOf(false) }

    val fundingOrgs = remember(organizations, savedIds, selectedType, savedOnly) {
        organizations.filter { org ->
            val relevant = org.isFundingRelevant()
            val typeOk = selectedType == "All Funding" || org.organizationType.equals(selectedType, ignoreCase = true)
            val savedOk = !savedOnly || savedIds.contains(org.id)
            relevant && typeOk && savedOk
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = if (compact) 14.dp else 16.dp,
                vertical = if (compact) 8.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            modifier = Modifier.fillMaxSize().testTag("funding_opportunities_list")
        ) {
            item {
                IndiaDirectoryDisclaimerCard(
                    text = "Funding & program directory disclaimer: Shows only investors, accelerators, incubators, and public programs backed by verified source records. Missing deadlines are labeled unverified."
                )
            }

            item {
                CompositionLocalProvider(
                    LocalMinimumInteractiveComponentSize provides if (compact) 36.dp else 48.dp
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                        item {
                            FilterChip(
                                selected = savedOnly,
                                onClick = {
                                    haptics.tap()
                                    savedOnly = !savedOnly
                                },
                                label = { Text("Saved (${savedIds.size})", maxLines = 1) },
                                modifier = Modifier
                                    .heightIn(min = if (compact) 34.dp else 48.dp)
                                    .testTag("funding_saved_only_chip")
                            )
                        }
                        items(EcosystemTaxonomy.fundingOrganizationTypes) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = {
                                    haptics.tap()
                                    selectedType = type
                                },
                                label = { Text(type, maxLines = 1) },
                                modifier = Modifier.heightIn(min = if (compact) 34.dp else 48.dp)
                            )
                        }
                    }
                }
            }

        if (fundingOrgs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (savedOnly) "No saved funding opportunities yet." else "No verified funding programs loaded yet.",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Bookmark verified investors/programs or import source-attributed JSON records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                haptics.tap()
                                onOpenImportDialog()
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Import Verified Directory JSON")
                        }
                    }
                }
            }
        } else {
            items(fundingOrgs, key = { "fund_${it.id.ifBlank { it.name }}" }) { org ->
                VerifiedOrganizationCard(
                    org = org,
                    isSaved = savedIds.contains(org.id),
                    onToggleSave = { onToggleSave(org) },
                    showFundingDetails = true
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
    }
}
