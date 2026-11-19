package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.RoseError
import com.example.util.AiNetworkHealthStatus
import com.example.util.DeviceConnectivityState
import com.example.util.DiagnosticStatus
import com.example.util.FirebaseDiagnosticsReport
import com.example.util.LocalHapticFeedbackManager
import com.example.util.NetworkDiagnosticManager
import com.example.util.NetworkDiagnosticState
import com.example.util.ServiceCheckResult

@Composable
fun ChatStreamNetworkStatusPill(
    diagnosticState: NetworkDiagnosticState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val haptics = LocalHapticFeedbackManager.current
    val isChecking = diagnosticState.isChecking || diagnosticState.health == AiNetworkHealthStatus.CHECKING
    val statusLabel = when {
        isChecking -> NetworkDiagnosticManager.LABEL_CHECKING_CONNECTION
        diagnosticState.health == AiNetworkHealthStatus.NO_INTERNET ->
            diagnosticState.streamLabel.ifBlank { NetworkDiagnosticManager.LABEL_NO_INTERNET }
        else -> diagnosticState.streamLabel.ifBlank { diagnosticState.statusMessage }
    }
    val secondaryText = when {
        isChecking -> "Testing internet and Firebase AI reachability…"
        diagnosticState.health == AiNetworkHealthStatus.NO_INTERNET ->
            diagnosticState.statusMessage.ifBlank { NetworkDiagnosticManager.STATUS_CHECK_INTERNET }
        diagnosticState.detailMessage.isNotBlank() -> diagnosticState.detailMessage
        else -> diagnosticState.statusMessage
    }
    val accent = when {
        isChecking -> MaterialTheme.colorScheme.primary
        diagnosticState.health == AiNetworkHealthStatus.APP_CHECK_ISSUE -> GoldAmber
        else -> RoseError
    }
    val icon = when {
        isChecking -> Icons.Filled.NetworkCheck
        diagnosticState.health == AiNetworkHealthStatus.NO_INTERNET -> Icons.Filled.WifiOff
        diagnosticState.health == AiNetworkHealthStatus.FIREBASE_UNREACHABLE -> Icons.Filled.CloudOff
        diagnosticState.health == AiNetworkHealthStatus.REQUESTS_BLOCKED -> Icons.Filled.Shield
        else -> Icons.Filled.WarningAmber
    }

    Surface(
        onClick = {
            haptics.tap()
            onClick()
        },
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("chat_stream_status_label")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 15.dp else 18.dp),
                    strokeWidth = 2.dp,
                    color = accent
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = statusLabel,
                    tint = accent,
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (secondaryText.isNotBlank()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = "Details",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
fun FirebaseDiagnosticsCard(
    report: FirebaseDiagnosticsReport,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedbackManager.current
    val (accentColor, statusIcon) = statusVisuals(
        status = if (report.isRunning) DiagnosticStatus.CHECKING else report.overallStatus,
        primary = MaterialTheme.colorScheme.primary
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                report.isRunning -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                report.hasRun && (report.overallStatus == DiagnosticStatus.OFFLINE ||
                    report.overallStatus == DiagnosticStatus.BLOCKED ||
                    report.overallStatus == DiagnosticStatus.ERROR) -> RoseError.copy(alpha = 0.65f)
                report.hasRun && report.overallStatus == DiagnosticStatus.WARN -> GoldAmber.copy(alpha = 0.65f)
                report.hasRun && report.overallStatus == DiagnosticStatus.PASS -> EmeraldSuccess.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outline
            }
        ),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_diagnostics_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row: status icon, title/subtitle, and run button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (report.isRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = stringResource(R.string.diag_cd_status_icon),
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (report.isRunning) {
                            stringResource(R.string.diag_checking_title)
                        } else if (report.hasRun) {
                            report.headline
                        } else {
                            stringResource(R.string.diag_title)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = when {
                        report.isRunning -> stringResource(R.string.diag_checking_subtitle)
                        report.hasRun -> "${report.transportLabel} · Checked ${report.checkedAtTimestamp}"
                        else -> stringResource(R.string.diag_idle_subtitle)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptics.tap()
                        onRunDiagnostics()
                    },
                    enabled = !report.isRunning,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("firebase_diagnostics_run_button")
                ) {
                    Icon(
                        imageVector = if (report.hasRun) Icons.Filled.Refresh else Icons.Filled.NetworkCheck,
                        contentDescription = if (report.hasRun) {
                            stringResource(R.string.diag_recheck)
                        } else {
                            stringResource(R.string.diag_run_check)
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (report.hasRun) {
                            stringResource(R.string.diag_recheck)
                        } else {
                            stringResource(R.string.diag_run_check)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Prominent Error / Warning Banner when offline, blocked, or failed
            AnimatedVisibility(
                visible = report.hasRun && !report.isRunning && !report.errorBanner.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val isWarn = report.overallStatus == DiagnosticStatus.WARN
                val bannerColor = if (isWarn) GoldAmber else RoseError
                Surface(
                    color = bannerColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_diagnostics_error_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (report.overallStatus) {
                                    DiagnosticStatus.OFFLINE -> Icons.Filled.WifiOff
                                    DiagnosticStatus.BLOCKED -> Icons.Filled.Shield
                                    DiagnosticStatus.WARN -> Icons.Filled.WarningAmber
                                    else -> Icons.Filled.ErrorOutline
                                },
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = report.headline,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = bannerColor
                            )
                        }
                        Text(
                            text = report.errorBanner.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (report.actionHint.isNotBlank()) {
                            Text(
                                text = report.actionHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Device Connectivity + App Check Summary Pills
            AnimatedVisibility(
                visible = report.hasRun && !report.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticSummaryPill(
                            label = "Device Network",
                            value = formatConnectivityLabel(report.connectivityState, report.transportLabel),
                            accent = when (report.connectivityState) {
                                DeviceConnectivityState.ONLINE_VALIDATED -> EmeraldSuccess
                                DeviceConnectivityState.ONLINE_UNVALIDATED -> GoldAmber
                                DeviceConnectivityState.OFFLINE -> RoseError
                                DeviceConnectivityState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticSummaryPill(
                            label = "App Check",
                            value = report.appCheckSummary.ifBlank { "Pending" },
                            accent = when {
                                report.appCheckSummary.contains("Token Ready") -> EmeraldSuccess
                                report.appCheckSummary.contains("Rejected") ||
                                    report.appCheckSummary.contains("Blocked") -> RoseError
                                else -> GoldAmber
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Per-service reachability list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.serviceChecks.forEach { check ->
                            ServiceReachabilityRow(check = check)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseDiagnosticsDialog(
    report: FirebaseDiagnosticsReport,
    onRunDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.diag_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FirebaseDiagnosticsCard(
                    report = report,
                    onRunDiagnostics = onRunDiagnostics
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptics.tap()
                    onRunDiagnostics()
                },
                enabled = !report.isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.heightIn(min = 48.dp).testTag("firebase_diagnostics_dialog_recheck")
            ) {
                Text(
                    text = if (report.isRunning) stringResource(R.string.diag_checking) else stringResource(R.string.diag_recheck)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    haptics.tap()
                    onDismiss()
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("firebase_diagnostics_dialog_close")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DiagnosticSummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ServiceReachabilityRow(check: ServiceCheckResult) {
    val (accent, _) = statusVisuals(check.status, MaterialTheme.colorScheme.primary)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firebase_diagnostics_row_${check.id}")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = check.host,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = check.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (check.status != DiagnosticStatus.PASS) {
                Text(
                    text = check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatConnectivityLabel(
    state: DeviceConnectivityState,
    transportLabel: String
): String = when (state) {
    DeviceConnectivityState.ONLINE_VALIDATED -> "Online ($transportLabel)"
    DeviceConnectivityState.ONLINE_UNVALIDATED -> "Unvalidated ($transportLabel)"
    DeviceConnectivityState.OFFLINE -> "Offline ($transportLabel)"
    DeviceConnectivityState.UNKNOWN -> "Unknown ($transportLabel)"
}

private fun statusVisuals(
    status: DiagnosticStatus,
    primary: Color
): Pair<Color, ImageVector> = when (status) {
    DiagnosticStatus.PASS -> EmeraldSuccess to Icons.Filled.CheckCircle
    DiagnosticStatus.WARN -> GoldAmber to Icons.Filled.WarningAmber
    DiagnosticStatus.OFFLINE -> RoseError to Icons.Filled.CloudOff
    DiagnosticStatus.BLOCKED -> RoseError to Icons.Filled.Shield
    DiagnosticStatus.ERROR -> RoseError to Icons.Filled.ErrorOutline
    DiagnosticStatus.CHECKING -> primary to Icons.Filled.NetworkCheck
}
