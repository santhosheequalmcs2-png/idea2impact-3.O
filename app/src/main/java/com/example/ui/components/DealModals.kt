package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Innovation
import com.example.data.model.InnovationCatalog
import com.example.data.model.InvestorOnePager
import com.example.data.model.Pledge
import com.example.data.model.SignedNda
import com.example.data.model.UiState
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.PurpleGlow
import com.example.util.SecurityCryptoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalNdaSheet(
    innovation: Innovation,
    defaultSignerName: String,
    existingNda: SignedNda?,
    isBusy: Boolean,
    onSignNda: (signerName: String, signerOrg: String?) -> Unit,
    onNotifyDownload: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var signerName by remember { mutableStateOf(defaultSignerName) }
    var signerOrg by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Confidential Deal Room · Mutual NDA", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Legal Non-Disclosure & IP Protection Agreement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close NDA modal")
                }
            }

            if (existingNda == null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "MUTUAL NON-DISCLOSURE AGREEMENT (v2.4)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Entered between ${innovation.authorName} (Disclosing Party) and ${signerName.ifBlank { "[Authorized Evaluator]" }} (Receiving Party) regarding \"${innovation.title}\".",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "1. Confidential Information: Covers technical schematics, CAD models, source code, patent claims, and financial projections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "2. Duty of Care & Non-Circumvention: Receiving Party agrees not to reverse engineer or disclose confidential materials for 24 months.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "3. Purpose Limitation: Used solely to evaluate investment, licensing, or technical collaboration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = signerName,
                    onValueChange = { signerName = it },
                    label = { Text("Signer Legal Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nda_signer_name_input")
                )

                OutlinedTextField(
                    value = signerOrg,
                    onValueChange = { signerOrg = it },
                    label = { Text("Entity / Venture Fund / Firm (Optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nda_signer_org_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { agreed = !agreed }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = agreed,
                        onCheckedChange = { agreed = it },
                        modifier = Modifier.testTag("nda_agree_checkbox")
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "I declare I am authorized to execute this agreement and accept all Mutual NDA terms.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = { onSignNda(signerName, signerOrg.ifBlank { null }) },
                    enabled = agreed && signerName.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("nda_sign_submit_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign & Unlock Confidential Room")
                    }
                }
            } else {
                Surface(
                    color = EmeraldMint.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldMint, modifier = Modifier.size(30.dp))
                        Column {
                            Text("Confidential Deal Room Unlocked", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Signature ID: ${existingNda.signatureId}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldMint
                            )
                        }
                    }
                }

                Text("Unlocked Proprietary Materials", style = MaterialTheme.typography.titleSmall)

                ConfidentialAssetCard(
                    title = "Full Technical Specification & Schematic Pack (v1.2)",
                    meta = "PDF · 14.8 MB · Cryptographically Timestamped",
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { onNotifyDownload("Full Technical Specification Pack (v1.2) prepared for offline review.") }
                )

                ConfidentialAssetCard(
                    title = "Cap Table & 3-Year Pro-Forma Financial Model",
                    meta = "XLSX · 2.4 MB · Unit margins & scenario modeling",
                    accent = PurpleGlow,
                    onClick = { onNotifyDownload("3-Year Pro-Forma Financial Model exported to workspace.") }
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Done & Return to Innovation")
                }
            }
        }
    }
}

@Composable
private fun ConfidentialAssetCard(
    title: String,
    meta: String,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Access", style = MaterialTheme.typography.labelMedium, color = accent)
        }
    }
}

@Composable
fun IpCertificateDialog(
    innovation: Innovation,
    onCopied: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val digest = remember(innovation.id, innovation.title) {
        SecurityCryptoUtils.sha256Hex("${innovation.id}:${innovation.title}:${innovation.creatorId}")
    }
    val registryId = remember(innovation.id) { SecurityCryptoUtils.formatRegistryId(innovation.id) }
    val dateStr = remember(innovation.createdAt) { SecurityCryptoUtils.formatDate(innovation.createdAt) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.5.dp, GoldAmber.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = GoldAmber)
                        Text("Cryptographic Proof of Creation", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close IP Certificate")
                    }
                }

                // Certificate Crest
                Surface(
                    shape = CircleShape,
                    color = GoldAmber.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, GoldAmber),
                    modifier = Modifier.size(62.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(34.dp))
                    }
                }

                Text(
                    "IDEA2IMPACT GLOBAL REGISTRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAmber
                )
                Text(
                    "CRYPTOGRAPHIC INVENTIVE TIMESTAMP RECORD",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text("This document attests that on $dateStr, the inventive disclosure titled", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Text(
                    "\"${innovation.title}\"",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("was registered by ${innovation.authorName}.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CertSpecBox("REGISTRY ID", registryId, Modifier.weight(1f))
                    CertSpecBox("CATEGORY", innovation.category, Modifier.weight(1f))
                }

                // SHA-256 Digest Box + Deterministic Matrix Seal
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Deterministic QR-style verification grid from digest
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val cells = 7
                            val cellSize = size.width / cells
                            for (r in 0 until cells) {
                                for (c in 0 until cells) {
                                    val idx = (r * cells + c) % digest.length
                                    val nibble = digest[idx].digitToIntOrNull(16) ?: 0
                                    if (nibble % 2 == 0 || (r in 0..1 && c in 0..1)) {
                                        drawRect(
                                            color = GoldAmber,
                                            topLeft = Offset(c * cellSize, r * cellSize),
                                            size = Size(cellSize * 0.85f, cellSize * 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("IMMUTABLE SHA-256 DIGEST", style = MaterialTheme.typography.labelSmall, color = GoldAmber)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = digest,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString("Registry: $registryId\nSHA-256: $digest"))
                            onCopied()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Hash")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun CertSpecBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroBackingSheet(
    innovation: Innovation,
    defaultSupporterName: String,
    isBusy: Boolean,
    completedPledge: Pledge?,
    onSubmitPledge: (supporterName: String, amount: Double, message: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTier by remember { mutableDoubleStateOf(25.0) }
    var customAmountText by remember { mutableStateOf("") }
    var supporterName by remember { mutableStateOf(defaultSupporterName) }
    var message by remember { mutableStateOf("") }

    val finalAmount = customAmountText.toDoubleOrNull()?.takeIf { it > 0 } ?: selectedTier

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.Coffee, contentDescription = null, tint = GoldAmber)
                    Column {
                        Text("Back this Inventor & Project", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Support ${innovation.authorName} with milestone micro-grants",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close backing sheet")
                }
            }

            if (completedPledge == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(5.0, "Coffee & Parts", "Sensor bench parts"),
                        Triple(25.0, "Rapid 3D Print", "PCB & filament runs"),
                        Triple(100.0, "Patent Patron", "IP search & filing")
                    ).forEach { (amt, title, desc) ->
                        val active = customAmountText.isBlank() && selectedTier == amt
                        Surface(
                            onClick = {
                                selectedTier = amt
                                customAmountText = ""
                            },
                            modifier = Modifier.weight(1f).testTag("backing_tier_${amt.toInt()}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = if (active) 1.5.dp else 1.dp,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("$${amt.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(title, style = MaterialTheme.typography.labelMedium)
                                Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("Or custom amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backing_custom_amount")
                )

                OutlinedTextField(
                    value = supporterName,
                    onValueChange = { supporterName = it },
                    label = { Text("Your Name / Organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("backing_supporter_name")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Encouraging Note (Optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("backing_message_input")
                )

                Button(
                    onClick = { onSubmitPledge(supporterName, finalAmount, message.ifBlank { null }) },
                    enabled = supporterName.isNotBlank() && finalAmount > 0 && !isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("backing_submit_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pledge $${finalAmount.toInt()} to Creator")
                    }
                }
            } else {
                Surface(
                    color = EmeraldMint.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Thank You for Backing Innovation!", style = MaterialTheme.typography.titleMedium, color = EmeraldMint)
                        Text(
                            "Pledged $${completedPledge.amount.toInt()} to ${innovation.authorName} for \"${innovation.title}\".",
                            style = MaterialTheme.typography.bodySmall
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Receipt ID: ${completedPledge.receiptId}", style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                        Text("Milestone: Hardware Prototype & Bench Validation", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Done & Return to Innovation")
                }
            }
        }
    }
}

@Composable
fun OfferProposalDialog(
    innovation: Innovation,
    isBusy: Boolean,
    onSubmit: (type: String, amount: String?, message: String, contact: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(InnovationCatalog.proposalTypes.first()) }
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Handshake, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Make an Offer / Proposal", style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close proposal modal")
                    }
                }

                Text(
                    "Propose partnership, investment, or licensing terms to ${innovation.authorName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InnovationCatalog.proposalTypes.take(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    InnovationCatalog.proposalTypes.drop(3).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Offer Amount / Terms (Optional)") },
                    placeholder = { Text("e.g. $50,000 Seed or 5% Royalty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("proposal_amount_input")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Proposal Details (Min 5 chars)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("proposal_message_input")
                )

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Direct Contact / Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("proposal_contact_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSubmit(selectedType, amount.ifBlank { null }, message, contact.ifBlank { null }) },
                        enabled = message.trim().length >= 5 && !isBusy,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("proposal_submit_button")
                    ) {
                        if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Send Offer")
                    }
                }
            }
        }
    }
}

@Composable
fun InvestorOnePagerDialog(
    innovation: Innovation,
    state: UiState<InvestorOnePager>,
    onRetry: () -> Unit,
    onCopied: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("AI Investor One-Pager", style = MaterialTheme.typography.titleMedium)
                            Text("Structured via Gemini 3.8 Flash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close One-Pager")
                    }
                }

                when (state) {
                    is UiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Synthesizing institutional investment memo for ${innovation.title}...", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }

                    is UiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Could not generate One-Pager", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                                Text(state.message, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = onRetry) { Text("Retry") }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val memo = state.data
                        MemoSection("Executive Summary", memo.executiveSummary)
                        MemoSection("Problem Statement", memo.problemStatement)
                        MemoSection("Solution & IP Moat", memo.solutionAdvantage)
                        MemoSection("Market Opportunity (TAM/SAM/SOM)", memo.marketOpportunity)
                        MemoSection("Suggested Investment Tranche", memo.suggestedTranche)

                        if (memo.milestones.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("90-Day Validation Milestones", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    memo.milestones.forEachIndexed { index, item ->
                                        Text("${index + 1}. $item", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val fullText = buildString {
                                        appendLine("${innovation.title} — Investor One-Pager")
                                        appendLine("\nExecutive Summary:\n${memo.executiveSummary}")
                                        appendLine("\nProblem:\n${memo.problemStatement}")
                                        appendLine("\nSolution & IP Advantage:\n${memo.solutionAdvantage}")
                                        appendLine("\nMarket Opportunity:\n${memo.marketOpportunity}")
                                        appendLine("\nSuggested Tranche:\n${memo.suggestedTranche}")
                                        appendLine("\n90-Day Milestones:")
                                        memo.milestones.forEachIndexed { idx, m -> appendLine("${idx + 1}. $m") }
                                    }
                                    clipboard.setText(AnnotatedString(fullText))
                                    onCopied()
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Memo")
                            }
                            Button(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                Text("Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoSection(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}
